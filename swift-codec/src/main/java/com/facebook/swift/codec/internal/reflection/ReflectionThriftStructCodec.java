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
import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.internal.TProtocolReader;
import com.facebook.swift.codec.internal.TProtocolWriter;
import com.facebook.swift.codec.metadata.ThriftConstructorInjection;
import com.facebook.swift.codec.metadata.ThriftFieldInjection;
import com.facebook.swift.codec.metadata.ThriftFieldMetadata;
import com.facebook.swift.codec.metadata.ThriftInjection;
import com.facebook.swift.codec.metadata.ThriftMethodInjection;
import com.facebook.swift.codec.metadata.ThriftParameterInjection;
import com.facebook.swift.codec.metadata.ThriftStructMetadata;
import com.google.common.base.Throwables;

import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolException;

import javax.annotation.concurrent.Immutable;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static com.facebook.swift.codec.metadata.FieldKind.THRIFT_FIELD;

import static java.lang.String.format;

@Immutable
public class ReflectionThriftStructCodec<T> extends AbstractReflectionThriftCodec<T>
{
    public ReflectionThriftStructCodec(ThriftCodecManager manager, ThriftStructMetadata metadata)
    {
        super(manager, metadata);
    }

    @Override
    public T read(TProtocol protocol)
            throws Exception
    {
        TProtocolReader reader = new TProtocolReader(protocol);
        reader.readStructBegin();

        Map<Short, Object> data = new HashMap<>(metadata.getFields().size());
        while (reader.nextField()) {
            short fieldId = reader.getFieldId();

            // do we have a codec for this field
            ThriftCodec<?> codec = fields.get(fieldId);
            if (codec == null) {
                reader.skipFieldData();
                continue;
            }

            // is this field readable
            ThriftFieldMetadata field = metadata.getField(fieldId);
            if (field.isReadOnly() || field.getType() != THRIFT_FIELD) {
                reader.skipFieldData();
                continue;
            }

            // read the value
            Object value = reader.readField(codec);
            if (value == null) {
              if (field.getRequiredness() == ThriftField.Requiredness.REQUIRED) {
                throw new TProtocolException("required field was not set");
              } else {
                continue;
              }
            }

            data.put(fieldId, value);
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
        writer.writeStructBegin(metadata.getStructName());

        for (ThriftFieldMetadata fieldMetadata : metadata.getFields(THRIFT_FIELD)) {
            // is the field readable?
            if (fieldMetadata.isWriteOnly()) {
                continue;
            }

            // get the field value
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
    private T constructStruct(Map<Short, Object> data)
            throws Exception
    {
        // construct instance
        Object instance;
        {
            ThriftConstructorInjection constructor = metadata.getConstructorInjection().get();
            Object[] parametersValues = new Object[constructor.getParameters().size()];
            for (ThriftParameterInjection parameter : constructor.getParameters()) {
                Object value = data.get(parameter.getId());
                parametersValues[parameter.getParameterIndex()] = value;
            }

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

        // inject fields
        for (ThriftFieldMetadata fieldMetadata : metadata.getFields(THRIFT_FIELD)) {
            for (ThriftInjection injection : fieldMetadata.getInjections()) {
                if (injection instanceof ThriftFieldInjection) {
                    ThriftFieldInjection fieldInjection = (ThriftFieldInjection) injection;
                    Object value = data.get(fieldInjection.getId());
                    if (value != null) {
                        fieldInjection.getField().set(instance, value);
                    }
                }
            }
        }

        // inject methods
        for (ThriftMethodInjection methodInjection : metadata.getMethodInjections()) {
            boolean shouldInvoke = false;
            Object[] parametersValues = new Object[methodInjection.getParameters().size()];
            for (ThriftParameterInjection parameter : methodInjection.getParameters()) {
                Object value = data.get(parameter.getId());
                if (value != null) {
                    parametersValues[parameter.getParameterIndex()] = value;
                    shouldInvoke = true;
                }
            }

            if (shouldInvoke) {
                try {
                    methodInjection.getMethod().invoke(instance, parametersValues);
                }
                catch (InvocationTargetException e) {
                    if (e.getTargetException() != null) {
                        Throwables.propagateIfInstanceOf(e.getTargetException(), Exception.class);
                    }
                    throw e;
                }
            }
        }

        // builder method
        if (metadata.getBuilderMethod().isPresent()) {
            ThriftMethodInjection builderMethod = metadata.getBuilderMethod().get();
            Object[] parametersValues = new Object[builderMethod.getParameters().size()];
            for (ThriftParameterInjection parameter : builderMethod.getParameters()) {
                Object value = data.get(parameter.getId());
                parametersValues[parameter.getParameterIndex()] = value;
            }

            try {
                instance = builderMethod.getMethod().invoke(instance, parametersValues);
                if (instance == null) {
                    throw new IllegalArgumentException("Builder method returned a null instance");

                }
                if (!metadata.getStructClass().isInstance(instance)) {
                    throw new IllegalArgumentException(format("Builder method returned instance of type %s, but an instance of %s is required",
                            instance.getClass().getName(),
                            metadata.getStructClass().getName()));
                }
            }
            catch (InvocationTargetException e) {
                if (e.getTargetException() != null) {
                    Throwables.propagateIfInstanceOf(e.getTargetException(), Exception.class);
                }
                throw e;
            }
        }

        return (T) instance;
    }
}
