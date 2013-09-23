/*
 * Copyright (C) 2012 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.swift.codec.internal.reflection;

import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.codec.internal.TProtocolReader;
import com.facebook.swift.codec.internal.TProtocolWriter;
import com.facebook.swift.codec.metadata.FieldKind;
import com.facebook.swift.codec.metadata.ThriftConstructorInjection;
import com.facebook.swift.codec.metadata.ThriftFieldInjection;
import com.facebook.swift.codec.metadata.ThriftFieldMetadata;
import com.facebook.swift.codec.metadata.ThriftInjection;
import com.facebook.swift.codec.metadata.ThriftMethodInjection;
import com.facebook.swift.codec.metadata.ThriftStructMetadata;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

import org.apache.thrift.protocol.TProtocol;

import javax.annotation.concurrent.Immutable;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static com.facebook.swift.codec.metadata.FieldKind.THRIFT_FIELD;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getOnlyElement;

import static java.lang.String.format;

@Immutable
public class ReflectionThriftUnionCodec<T> extends AbstractReflectionThriftCodec<T>
{
    private final Map<Short, ThriftFieldMetadata> metadataMap;
    private final Map.Entry<ThriftFieldMetadata, ThriftCodec<?>> idField;

    public ReflectionThriftUnionCodec(ThriftCodecManager manager, ThriftStructMetadata metadata)
    {
        super(manager, metadata);

        ThriftFieldMetadata idField = getOnlyElement(metadata.getFields(FieldKind.THRIFT_UNION_ID));

        this.idField = Maps.<ThriftFieldMetadata, ThriftCodec<?>>immutableEntry(idField, manager.getCodec(idField.getThriftType()));
        checkNotNull(this.idField.getValue(), "No codec for id field %s found", idField);

        this.metadataMap = Maps.uniqueIndex(metadata.getFields(), ThriftFieldMetadata.getIdGetter());
    }

    @Override
    public T read(TProtocol protocol)
            throws Exception
    {
        TProtocolReader reader = new TProtocolReader(protocol);
        reader.readStructBegin();

        Map.Entry<Short, Object> data = null;
        Short fieldId = null;
        while (reader.nextField()) {
            checkState(fieldId == null, "Received Union with more than one value (seen id %s, now id %s)", fieldId, reader.getFieldId());

            fieldId = reader.getFieldId();

            // do we have a codec for this field
            ThriftCodec<?> codec = fields.get(fieldId);
            if (codec == null) {
                reader.skipFieldData();
            }
            else {

                // is this field readable
                ThriftFieldMetadata field = metadata.getField(fieldId);
                if (field.isWriteOnly() || field.getType() != THRIFT_FIELD) {
                    reader.skipFieldData();
                    continue;
                }

                // read the value
                Object value = reader.readField(codec);
                if (value == null) {
                    continue;
                }

                data = Maps.immutableEntry(fieldId, value);
            }
        }
        reader.readStructEnd();

        // build the struct
        return constructStruct(data);
    }

    @Override
    public void write(T instance, TProtocol protocol)
            throws Exception
    {
        TProtocolWriter writer = new TProtocolWriter(protocol);

        Short idValue = (Short) getFieldValue(instance, idField.getKey());

        writer.writeStructBegin(metadata.getStructName());

        if (metadataMap.containsKey(idValue)) {
            ThriftFieldMetadata fieldMetadata = metadataMap.get(idValue);

            if (fieldMetadata.isReadOnly() || fieldMetadata.getType() != THRIFT_FIELD) {
                throw new IllegalStateException(format("Field %s is not readable", fieldMetadata.getName()));
            }

            Object fieldValue = getFieldValue(instance, fieldMetadata);

            // write the field
            if (fieldValue != null) {
                @SuppressWarnings("unchecked")
                ThriftCodec<Object> codec = (ThriftCodec<Object>) fields.get(fieldMetadata.getId());
                writer.writeField(fieldMetadata.getName(), fieldMetadata.getId(), codec, fieldValue);
            }
        }
        writer.writeStructEnd();
    }

    @SuppressWarnings("unchecked")
    private T constructStruct(Map.Entry<Short, Object> data)
            throws Exception
    {
        // construct instance
        Object instance = null;

        ThriftFieldMetadata fieldMetadata = null;

        if (data != null) {
            fieldMetadata = metadataMap.get(data.getKey());

            if (fieldMetadata != null && fieldMetadata.getConstructorInjection().isPresent()) {
                    ThriftConstructorInjection constructor = fieldMetadata.getConstructorInjection().get();

                    Object[] parametersValues = new Object[] { data.getValue() };

                    try {
                        instance = constructor.getConstructor().newInstance(parametersValues);
                    }
                    catch (InvocationTargetException e) {
                        if (e.getTargetException() != null) {
                            Throwables.propagateIfInstanceOf(e.getTargetException(), Exception.class);
                        }
                        throw e;
                    }
            }
        }

        if (instance == null && metadata.getConstructorInjection().isPresent()) {
            ThriftConstructorInjection constructor = metadata.getConstructorInjection().get();
            // must be no-args
            Object[] parametersValues = new Object[0];

            try {
                instance = constructor.getConstructor().newInstance(parametersValues);
            }
            catch (InvocationTargetException e) {
                if (e.getTargetException() != null) {
                    Throwables.propagateIfInstanceOf(e.getTargetException(), Exception.class);
                }
                throw e;
            }
        }

        if (fieldMetadata != null) {
            // inject fields
            for (ThriftInjection injection : fieldMetadata.getInjections()) {
                if (injection instanceof ThriftFieldInjection) {
                    ThriftFieldInjection fieldInjection = (ThriftFieldInjection) injection;
                    if (data.getValue() != null) {
                        fieldInjection.getField().set(instance, data.getValue());
                    }
                }
            }

            if (fieldMetadata.getMethodInjection().isPresent()) {
                Object[] parametersValues = new Object[] { data.getValue() };

                if (data.getValue() != null) {
                    try {
                        fieldMetadata.getMethodInjection().get().getMethod().invoke(instance, parametersValues);
                    }
                    catch (InvocationTargetException e) {
                        if (e.getTargetException() != null) {
                            Throwables.propagateIfInstanceOf(e.getTargetException(), Exception.class);
                        }
                        throw e;
                    }
                }
            }
        }

        if (data != null) {
            // inject id value
            for (ThriftInjection injection : idField.getKey().getInjections()) {
                if (injection instanceof ThriftFieldInjection) {
                    ThriftFieldInjection fieldInjection = (ThriftFieldInjection) injection;
                    fieldInjection.getField().set(instance, data.getKey());
                }
            }

            // builder method
            if (metadata.getBuilderMethod().isPresent()) {
                ThriftMethodInjection builderMethod = metadata.getBuilderMethod().get();
                Object[] parametersValues = new Object[] { data.getValue() };

                try {
                    instance = builderMethod.getMethod().invoke(instance, parametersValues);
                    checkState(instance != null, "Builder method returned a null instance");
                    checkState(metadata.getStructClass().isInstance(instance),
                               "Builder method returned instance of type %s, but an instance of %s is required",
                               instance.getClass().getName(),
                               metadata.getStructClass().getName());
                }
                catch (InvocationTargetException e) {
                    if (e.getTargetException() != null) {
                        Throwables.propagateIfInstanceOf(e.getTargetException(), Exception.class);
                    }
                    throw e;
                }
            }
        }

        return (T) instance;
    }
}
