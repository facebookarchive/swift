/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.ThriftCodec;
import com.facebook.swift.metadata.ThriftCatalog;
import com.facebook.swift.metadata.ThriftStructMetadata;
import com.facebook.swift.metadata.ThriftType;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.apache.thrift.protocol.TList;
import org.apache.thrift.protocol.TMap;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TSet;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkState;

public class CompiledThriftCodec implements ThriftCodec
{
    private final ThriftCatalog catalog;
    private final DynamicClassLoader classLoader;
    private final LoadingCache<Class<?>, ThriftTypeCodec<?>> typeCodecs;
    private final ThriftCodecCompiler compiler;

    public CompiledThriftCodec(ThriftCatalog catalog, ThriftTypeCodec<?>... codecs)
    {
        this(catalog, new DynamicClassLoader(), codecs);
    }

    public CompiledThriftCodec(ThriftCatalog catalog, DynamicClassLoader classLoader, ThriftTypeCodec<?>... codecs)
    {
        this.catalog = catalog;
        this.classLoader = classLoader;
        this.compiler = new ThriftCodecCompiler(catalog, classLoader);

        typeCodecs = CacheBuilder.newBuilder().build(
                new CacheLoader<Class<?>, ThriftTypeCodec<?>>()
                {
                    public ThriftTypeCodec<?> load(Class<?> type)
                            throws Exception
                    {
                        return compiler.generateThriftTypeCodec(type);
                    }
                });

        for (ThriftTypeCodec<?> codec : codecs) {
            typeCodecs.put(codec.getType(), codec);
        }
    }

    public ThriftCatalog getCatalog()
    {
        return catalog;
    }

    @Override
    public <T> T read(Class<T> type, TProtocol protocol)
            throws Exception
    {
        return getTypeCodec(type).read(new TProtocolReader(protocol));
    }

    @Override
    public <T> void write(Class<T> type, T value, TProtocol protocol)
            throws Exception
    {
        getTypeCodec(type).write(value, new TProtocolWriter(protocol));
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
                return read(type.getStructMetadata().getStructClass(), protocol);
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
                write((Class<Object>) fieldStructMetadata.getStructClass(), value, protocol);
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

    public <T> ThriftTypeCodec<T> getTypeCodec(Class<T> type)
            throws ExecutionException
    {
        return (ThriftTypeCodec<T>) typeCodecs.get(type);
    }

}
