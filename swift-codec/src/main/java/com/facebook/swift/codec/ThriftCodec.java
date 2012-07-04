/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec;

import com.facebook.swift.codec.metadata.ThriftType;
import org.apache.thrift.protocol.TProtocol;

/**
 * A single type codec for reading and writing in Thrift format.  Each codec is symmetric and
 * therefore only supports a single concrete type.
 * <p/>
 * Implementations of this interface are expected to be thread safe.
 *
 * @param <T> the type this codec supports
 */
public interface ThriftCodec<T>
{
    /**
     * The Thrift type this codec supports.  The Thrift type contains the Java generic Type of the
     * codec.
     */
    public ThriftType getType();

    /**
     * Reads a value from supplied Thrift protocol reader.
     *
     * @param protocol the protocol to read from
     * @return the value; not null
     * @throws Exception if any problems occurred when reading or coercing  the value
     */
    public T read(TProtocol protocol)
            throws Exception;

    /**
     * Writes a value to the supplied Thrift protocol writer.
     *
     * @param value the value to write; not null
     * @param protocol the protocol to write to
     * @throws Exception if any problems occurred when writing or coercing  the value
     */
    public void write(T value, TProtocol protocol)
            throws Exception;
}
