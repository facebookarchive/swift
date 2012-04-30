/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.reflection;

import com.facebook.swift.ThriftCodec;
import com.facebook.swift.metadata.ThriftCatalog;
import com.facebook.swift.metadata.ThriftConstructorInjection;
import com.facebook.swift.metadata.ThriftExtraction;
import com.facebook.swift.metadata.ThriftFieldExtractor;
import com.facebook.swift.metadata.ThriftFieldInjection;
import com.facebook.swift.metadata.ThriftFieldMetadata;
import com.facebook.swift.metadata.ThriftType;
import com.facebook.swift.metadata.ThriftInjection;
import com.facebook.swift.metadata.ThriftMethodExtractor;
import com.facebook.swift.metadata.ThriftMethodInjection;
import com.facebook.swift.metadata.ThriftParameterInjection;
import com.facebook.swift.metadata.ThriftStructMetadata;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.apache.thrift.protocol.TField;
import org.apache.thrift.protocol.TList;
import org.apache.thrift.protocol.TMap;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolUtil;
import org.apache.thrift.protocol.TSet;
import org.apache.thrift.protocol.TStruct;
import org.apache.thrift.protocol.TType;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Preconditions.checkNotNull;

public class ReflectionThriftCodec implements ThriftCodec
{
    private final ThriftCatalog catalog;

    public ReflectionThriftCodec(ThriftCatalog catalog)
    {
        this.catalog = checkNotNull(catalog, "catalog is null");
    }

    @Override
    public <T> T read(Class<T> type, TProtocol protocol)
            throws Exception
    {
        ThriftType thriftType = catalog.getThriftType(type);
        return (T) read(thriftType, protocol);
    }

    @Override
    public Object read(ThriftType type, TProtocol protocol)
            throws Exception
    {
        switch (type.getProtocolType()) {
            case BOOL: {
                return protocol.readBool();
            }
            case BYTE: {
                return protocol.readByte();
            }
            case DOUBLE: {
                return protocol.readDouble();
            }
            case I16: {
                return protocol.readI16();
            }
            case I32: {
                return protocol.readI32();
            }
            case I64: {
                return protocol.readI64();
            }
            case STRING: {
                return protocol.readString();
            }
            case STRUCT: {
                return read(type.getStructMetadata(), protocol);
            }
            case MAP: {
                ThriftType keyType = type.getKeyType();
                ThriftType valueType = type.getValueType();

                TMap tMap = protocol.readMapBegin();

                ImmutableMap.Builder<Object, Object> map = ImmutableMap.builder();
                for (int i = 0; i < tMap.size; i++) {
                    Object entryKey = read(keyType, protocol);
                    Object entryValue = read(valueType, protocol);
                    map.put(entryKey, entryValue);
                }
                protocol.readMapEnd();
                return map.build();
            }
            case SET: {
                ThriftType elementType = type.getValueType();

                TSet tSet = protocol.readSetBegin();
                ImmutableSet.Builder<Object> set = ImmutableSet.builder();
                for (int i = 0; i < tSet.size; i++) {
                    Object element = read(elementType, protocol);
                    set.add(element);
                }
                protocol.readSetEnd();
                return set.build();
            }
            case LIST: {
                ThriftType elementType = type.getValueType();

                TList tList = protocol.readListBegin();
                ImmutableList.Builder<Object> list = ImmutableList.builder();
                for (int i = 0; i < tList.size; i++) {
                    Object element = read(elementType, protocol);
                    list.add(element);
                }
                protocol.readListEnd();
                return list.build();
            }
            case ENUM: {
                // todo implement enums
                throw new UnsupportedOperationException("enums are not implemented");
            }
            default: {
                throw new IllegalStateException("Read does not support fields of type " + type);
            }
        }
    }

    public <T> T read(ThriftStructMetadata<T> metadata, TProtocol protocol)
            throws Exception
    {
        TField field;
        protocol.readStructBegin();

        Map<Short, Object> data = new HashMap<>(metadata.getFields().size());
        while (true) {
            field = protocol.readFieldBegin();
            if (field.type == TType.STOP) {
                break;
            }

            ThriftFieldMetadata fieldMetadata = metadata.getField(field.id);

            // ignore unknown fields
            if (fieldMetadata == null) {
                TProtocolUtil.skip(protocol, field.type);
            }

            // skip fields when the protocol type does not match the expected type
            ThriftType type = fieldMetadata.getType();
            if (type.getProtocolType().ordinal() != field.type) {
                TProtocolUtil.skip(protocol, field.type);
            }
            Object value = read(type, protocol);
            data.put(fieldMetadata.getId(), value);

            protocol.readFieldEnd();
        }
        protocol.readStructEnd();

        return constructStruct(metadata, data);
    }

    private <T> T constructStruct(ThriftStructMetadata<T> metadata, Map<Short, Object> data)
            throws Exception
    {
        // construct instance
        Object instance;
        {
            ThriftConstructorInjection constructor = metadata.getConstructor();
            Object[] parametersValues = new Object[constructor.getParameters().size()];
            for (ThriftParameterInjection parameter : constructor.getParameters()) {
                parametersValues[parameter.getParameterIndex()] = data.get(parameter.getId());
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
        for (ThriftFieldMetadata fieldMetadata : metadata.getFields()) {
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
            Object[] parametersValues = new Object[methodInjection.getParameters().size()];
            for (ThriftParameterInjection parameter : methodInjection.getParameters()) {
                parametersValues[parameter.getParameterIndex()] = data.get(parameter.getId());
            }

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

        // builder method
        if (metadata.getBuilderMethod() != null) {
            ThriftMethodInjection builderMethod = metadata.getBuilderMethod();
            Object[] parametersValues = new Object[builderMethod.getParameters().size()];
            for (ThriftParameterInjection parameter : builderMethod.getParameters()) {
                parametersValues[parameter.getParameterIndex()] = data.get(parameter.getId());
            }

            try {
                instance = builderMethod.getMethod().invoke(instance, parametersValues);
                if (instance == null) {
                    throw new IllegalArgumentException("Builder method returned a null instance");

                }
                if (!metadata.getStructClass().isInstance(instance)) {
                    throw new IllegalArgumentException(String.format("Builder method returned instance of type %s, but an instance of %s is required",
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

    @Override
    public <T> void write(Class<T> type, T value, TProtocol protocol)
            throws Exception
    {
        ThriftType thriftType = catalog.getThriftType(type);
        write(thriftType, value, protocol);
    }

    public void write(ThriftType type, Object value, TProtocol protocol)
            throws Exception
    {
        switch (type.getProtocolType()) {
            case BOOL: {
                protocol.writeBool((Boolean) value);
                break;
            }
            case BYTE: {
                protocol.writeByte((Byte) value);
                break;
            }
            case DOUBLE: {
                protocol.writeDouble((Double) value);
                break;
            }
            case I16: {
                protocol.writeI16((Short) value);
                break;
            }
            case I32: {
                protocol.writeI32((Integer) value);
                break;
            }
            case I64: {
                protocol.writeI64((Long) value);
                break;
            }
            case STRING: {
                protocol.writeString((String) value);
                break;
            }
            case STRUCT: {
                ThriftStructMetadata<?> fieldStructMetadata = type.getStructMetadata();
                writeStruct((ThriftStructMetadata<Object>) fieldStructMetadata, value, protocol);
                break;
            }
            case MAP: {
                ThriftType keyType = type.getKeyType();
                ThriftType valueType = type.getValueType();

                Map<?, ?> map = (Map<?, ?>) value;

                protocol.writeMapBegin(new TMap(keyType.getProtocolType().getType(), valueType.getProtocolType().getType(), map.size()));
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    Object entryKey = entry.getKey();
                    checkState(entryKey != null, "Thrift does not support null map keys");
                    write(keyType, entryKey, protocol);

                    Object entryValue = entry.getValue();
                    checkState(entryValue != null, "Thrift does not support null map values");
                    write(valueType, entryValue, protocol);
                }
                protocol.writeMapEnd();
                break;
            }
            case SET: {
                ThriftType elementType = type.getValueType();

                Iterable<?> list = (Iterable<?>) value;
                protocol.writeSetBegin(new TSet(elementType.getProtocolType().getType(), Iterables.size(list)));
                for (Object element : list) {
                    checkState(element != null, "Thrift does not support null set elements");
                    write(elementType, element, protocol);
                }
                protocol.writeSetEnd();
                break;
            }
            case LIST: {
                ThriftType elementType = type.getValueType();

                Iterable<?> list = (Iterable<?>) value;
                protocol.writeListBegin(new TList(elementType.getProtocolType().getType(), Iterables.size(list)));
                for (Object element : list) {
                    checkState(element != null, "Thrift does not support null list elements");
                    write(elementType, element, protocol);
                }
                protocol.writeListEnd();
                break;
            }
            case ENUM: {
                // todo implement enums
                throw new UnsupportedOperationException("enums are not implemented");
            }
            default: {
                throw new IllegalStateException("Write does not support fields of type " + type);
            }
        }
    }

    public <T> void writeStruct(ThriftStructMetadata<T> metadata, T instance, TProtocol protocol)
            throws Exception
    {
        protocol.writeStructBegin(new TStruct(metadata.getStructName()));
        for (ThriftFieldMetadata field : metadata.getFields()) {
            Object value = getFieldValue(instance, field);
            if (value == null) {
                continue;
            }

            ThriftType type = field.getType();
            protocol.writeFieldBegin(new TField(field.getName(), type.getProtocolType().getType(), field.getId()));
            write(type, value, protocol);
            protocol.writeFieldEnd();

        }
        protocol.writeFieldStop();
        protocol.writeStructEnd();
    }

    private Object getFieldValue(Object instance, ThriftFieldMetadata field)
            throws Exception
    {
        try {
            ThriftExtraction extraction = field.getExtraction();
            if (extraction instanceof ThriftFieldExtractor) {
                ThriftFieldExtractor thriftFieldExtractor = (ThriftFieldExtractor) extraction;
                return thriftFieldExtractor.getField().get(instance);
            }
            else if (extraction instanceof ThriftMethodExtractor) {
                ThriftMethodExtractor thriftMethodExtractor = (ThriftMethodExtractor) extraction;
                return thriftMethodExtractor.getMethod().invoke(instance);
            }
            else {
                throw new IllegalAccessException("Unsupported field extractor type " + extraction.getClass().getName());
            }
        }
        catch (InvocationTargetException e) {
            if (e.getTargetException() != null) {
                Throwables.propagateIfInstanceOf(e.getTargetException(), Exception.class);
            }
            throw e;
        }
    }
}
