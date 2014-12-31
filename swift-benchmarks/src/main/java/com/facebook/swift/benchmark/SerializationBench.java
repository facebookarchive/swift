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
package com.facebook.swift.benchmark;

import com.facebook.nifty.core.TNiftyTransport;
import com.facebook.nifty.core.ThriftMessage;
import com.facebook.nifty.core.ThriftTransportType;
import com.facebook.swift.benchmark.structs.BigBinary;
import com.facebook.swift.benchmark.structs.BigInt;
import com.facebook.swift.benchmark.structs.BigListInt;
import com.facebook.swift.benchmark.structs.BigListMixed;
import com.facebook.swift.benchmark.structs.BigString;
import com.facebook.swift.benchmark.structs.Empty;
import com.facebook.swift.benchmark.structs.LargeBinary;
import com.facebook.swift.benchmark.structs.LargeListMixed;
import com.facebook.swift.benchmark.structs.Mixed;
import com.facebook.swift.benchmark.structs.SmallInt;
import com.facebook.swift.benchmark.structs.SmallListInt;
import com.facebook.swift.benchmark.structs.SmallString;
import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.codec.internal.TProtocolSizers;
import com.google.common.collect.Lists;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class SerializationBench
{
    private static Object create(Class<?> cls) {
        if (cls.equals(Empty.class)) {
            Empty e = new Empty();
            return e;
        } else if (cls.equals(SmallInt.class)) {
            SmallInt i = new SmallInt();
            i.smallint = 5;
            return i;
        } else if (cls.equals(BigInt.class)) {
            BigInt i = new BigInt();
            i.bigint = 0x123456789abcdefL;
            return i;
        } else if (cls.equals(SmallString.class)) {
            SmallString s = new SmallString();
            s.str = "small string";
            return s;
        } else if (cls.equals(BigString.class)) {
            BigString s = new BigString();
            StringBuilder sb = new StringBuilder(10000);
            for (int i = 0; i < 10000; i++) {
                sb.append('a');
            }
            s.str = sb.toString();
            return s;
        } else if (cls.equals(BigBinary.class)) {
            BigBinary b = new BigBinary();
            b.buf = ByteBuffer.allocate(10000);
            return b;
        } else if (cls.equals(LargeBinary.class)) {
            LargeBinary b = new LargeBinary();
            b.buf = ByteBuffer.allocate(10000000);
            return b;
        } else if (cls.equals(Mixed.class)) {
            Mixed m = new Mixed();
            m.i = 5;
            m.l = 12345;
            m.b = true;
            m.s = "hello";
            return m;
        } else if (cls.equals(SmallListInt.class)) {
            SmallListInt l = new SmallListInt();
            List<Integer> lst = Lists.newArrayListWithCapacity(10);
            for (int i = 0; i < 10; i++) {
                lst.add(5);
            }
            l.lst = lst;
            return l;
        } else if (cls.equals(BigListInt.class)) {
            BigListInt l = new BigListInt();
            List<Integer> lst = Lists.newArrayListWithCapacity(10000);
            for (int i = 0; i < 10000; i++) {
                lst.add(5);
            }
            l.lst = lst;
            return l;
        } else if (cls.equals(BigListMixed.class)) {
            BigListMixed l = new BigListMixed();
            List<Mixed> lst = Lists.newArrayListWithCapacity(10000);
            for (int i = 0; i < 10000; i++) {
                lst.add((Mixed)create(Mixed.class));
            }
            l.lst = lst;
            return l;
        } else if (cls.equals(LargeListMixed.class)) {
            LargeListMixed l = new LargeListMixed();
            List<Mixed> lst = Lists.newArrayListWithCapacity(1000000);
            for (int i = 0; i < 1000000; i++) {
                lst.add((Mixed)create(Mixed.class));
            }
            l.lst = lst;
            return l;
        }
        return null;
    }

    private static GeneratedMessage createPB(Class<?> cls) {
        if (cls.equals(ProtoBufBenchData.Empty.class)) {
            return ProtoBufBenchData.Empty.newBuilder().build();
        } else if (cls.equals(ProtoBufBenchData.SmallInt.class)) {
            return ProtoBufBenchData.SmallInt.newBuilder().setSmallint(5).build();
        } else if (cls.equals(ProtoBufBenchData.BigInt.class)) {
            return ProtoBufBenchData.BigInt.newBuilder().setBigint(0x1234567890abcdefL).build();
        } else if (cls.equals(ProtoBufBenchData.SmallString.class)) {
            return ProtoBufBenchData.SmallString.newBuilder().setSmallstr("small string").build();
        } else if (cls.equals(ProtoBufBenchData.BigString.class)) {
            StringBuilder sb = new StringBuilder(10000);
            for (int i = 0; i < 10000; i++) {
                sb.append('a');
            }
            return ProtoBufBenchData.BigString.newBuilder().setBigstr(sb.toString()).build();
        } else if (cls.equals(ProtoBufBenchData.Mixed.class)) {
            return ProtoBufBenchData.Mixed.newBuilder().setI32(5).setI64(12345).setB(true).setS("hello").build();
        } else if (cls.equals(ProtoBufBenchData.SmallListInt.class)) {
            ProtoBufBenchData.SmallListInt.Builder b = ProtoBufBenchData.SmallListInt.newBuilder();
            for (int i = 0; i < 10; i++) {
                b.addLst(5);
            }
            return b.build();
        } else if (cls.equals(ProtoBufBenchData.BigListInt.class)) {
            ProtoBufBenchData.BigListInt.Builder b = ProtoBufBenchData.BigListInt.newBuilder();
            for (int i = 0; i < 10000; i++) {
                b.addLst(5);
            }
            return b.build();
        } else if (cls.equals(ProtoBufBenchData.BigListMixed.class)) {
            ProtoBufBenchData.BigListMixed.Builder b = ProtoBufBenchData.BigListMixed.newBuilder();
            for (int i = 0; i < 10000; i++) {
                b.addLst((ProtoBufBenchData.Mixed)createPB(ProtoBufBenchData.Mixed.class));
            }
            return b.build();
        } else if (cls.equals(ProtoBufBenchData.LargeListMixed.class)) {
            ProtoBufBenchData.LargeListMixed.Builder b = ProtoBufBenchData.LargeListMixed.newBuilder();
            for (int i = 0; i < 1000000; i++) {
                b.addLst((ProtoBufBenchData.Mixed)createPB(ProtoBufBenchData.Mixed.class));
            }
            return b.build();
        }
        return null;
    }

    private ThriftCodecManager codecManager = new ThriftCodecManager();

    @Param({
            "Empty",
            "SmallInt",
            "BigInt",
            "SmallString",
            "BigString",
            "BigBinary",
            "LargeBinary",
            "Mixed",
            "SmallListInt",
            "BigListInt",
            "BigListMixed",
            "LargeListMixed",
    })
    private String structClassAsString;
    private ThriftCodec codec;
    private Object struct;
    private ChannelBuffer serialized;
    private GeneratedMessage pbStruct;
    private byte[] pbSerialized;

    @Setup
    public void setup() throws Exception
    {
        Class structClass = Class.forName("com.facebook.swift.benchmark.structs." + structClassAsString);
        codec = codecManager.getCodec(structClass);

        // for serialization
        struct = create(structClass);

        // for deserialization
        TNiftyTransport outTransport = new TNiftyTransport(null, new ThriftMessage(ChannelBuffers.EMPTY_BUFFER, ThriftTransportType.UNFRAMED));
        int serializedSize = codec.serializedSize(struct, TProtocolSizers.COMPACT_SIZER);
        outTransport.reserveOutputBuffer(serializedSize);
        codec.write(create(structClass), new TCompactProtocol(outTransport));
        serialized = outTransport.getOutputBuffer();

        // for PB serialization
        try {
            Class pbStructClass = Class.forName("com.facebook.swift.benchmark.ProtoBufBenchData$" + structClassAsString);
            pbStruct = createPB(pbStructClass);
        } catch (ClassNotFoundException e) {
            pbStruct = null;
        }

        // for PB deserialization
        if (pbStruct != null) {
            pbSerialized = pbStruct.toByteArray();
        } else {
            pbSerialized = null;
        }
    }

    @Benchmark
    public ChannelBuffer serialize() throws Exception
    {
        TNiftyTransport transport = new TNiftyTransport(null, new ThriftMessage(ChannelBuffers.EMPTY_BUFFER, ThriftTransportType.UNFRAMED));
        int serializedSize = codec.serializedSize(struct, TProtocolSizers.COMPACT_SIZER);
        transport.reserveOutputBuffer(serializedSize);
        TProtocol protocol = new TCompactProtocol(transport);
        codec.write(struct, protocol);
        return transport.getOutputBuffer();
    }

    @Benchmark
    public byte[] serializePB() throws Exception
    {
        return pbStruct.toByteArray();
    }

    @Benchmark
    public Object deserialize() throws Exception
    {
        serialized.readerIndex(0);
        TNiftyTransport transport = new TNiftyTransport(null, new ThriftMessage(serialized, ThriftTransportType.UNFRAMED));
        TProtocol protocol = new TCompactProtocol(transport);
        return codec.read(protocol);
    }

    @Benchmark
    public Message deserializePB() throws Exception
    {
        return pbStruct.getParserForType().parseFrom(pbSerialized);
    }

    @Benchmark
    public ChannelBuffer both() throws Exception
    {
        serialized.readerIndex(0);
        TNiftyTransport transport = new TNiftyTransport(null, new ThriftMessage(serialized, ThriftTransportType.UNFRAMED));
        TProtocol protocol = new TCompactProtocol(transport);

        Object value = codec.read(protocol);
        int serializedSize = codec.serializedSize(value, TProtocolSizers.COMPACT_SIZER);
        transport.reserveOutputBuffer(serializedSize);
        codec.write(value, protocol);
        return transport.getOutputBuffer();
    }

    @Benchmark
    public TProtocol overhead() throws Exception
    {
        TNiftyTransport transport = new TNiftyTransport(null, new ThriftMessage(ChannelBuffers.EMPTY_BUFFER, ThriftTransportType.UNFRAMED));
        TProtocol protocol = new TCompactProtocol(transport);
        return protocol;
    }


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .warmupIterations(10)
                .measurementIterations(10)
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
