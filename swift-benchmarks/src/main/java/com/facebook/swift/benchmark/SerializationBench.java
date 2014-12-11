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
import com.facebook.swift.codec.ThriftCodecManager;
import com.google.common.collect.Lists;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
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

public class SerializationBench
{
    private static <T> T create(Class<T> cls) {
        if (cls.equals(Empty.class)) {
            Empty e = new Empty();
            return (T)e;
        } else if (cls.equals(SmallInt.class)) {
            SmallInt i = new SmallInt();
            i.smallint = 5;
            return (T)i;
        } else if (cls.equals(BigInt.class)) {
            BigInt i = new BigInt();
            i.bigint = 0x123456789abcdefL;
            return (T)i;
        } else if (cls.equals(SmallString.class)) {
            SmallString s = new SmallString();
            s.str = "small string";
            return (T)s;
        } else if (cls.equals(BigString.class)) {
            BigString s = new BigString();
            StringBuilder sb = new StringBuilder(10000);
            for (int i = 0; i < 10000; i++) {
                sb.append('a');
            }
            s.str = sb.toString();
            return (T) s;
        } else if (cls.equals(BigBinary.class)) {
            BigBinary b = new BigBinary();
            b.buf = ByteBuffer.allocate(10000);
            return (T)b;
        } else if (cls.equals(LargeBinary.class)) {
            LargeBinary b = new LargeBinary();
            b.buf = ByteBuffer.allocate(10000000);
            return (T)b;
        } else if (cls.equals(Mixed.class)) {
            Mixed m = new Mixed();
            m.i = 5;
            m.l = 12345;
            m.b = true;
            m.s = "hello";
            return (T)m;
        } else if (cls.equals(SmallListInt.class)) {
            SmallListInt l = new SmallListInt();
            List<Integer> lst = Lists.newArrayListWithCapacity(10);
            for (int i = 0; i < 10; i++) {
                lst.add(5);
            }
            l.lst = lst;
            return (T)l;
        } else if (cls.equals(BigListInt.class)) {
            BigListInt l = new BigListInt();
            List<Integer> lst = Lists.newArrayListWithCapacity(10000);
            for (int i = 0; i < 10000; i++) {
                lst.add(5);
            }
            l.lst = lst;
            return (T)l;
        } else if (cls.equals(BigListMixed.class)) {
            BigListMixed l = new BigListMixed();
            List<Mixed> lst = Lists.newArrayListWithCapacity(10000);
            for (int i = 0; i < 10000; i++) {
                lst.add(create(Mixed.class));
            }
            l.lst = lst;
            return (T)l;
        } else if (cls.equals(LargeListMixed.class)) {
            LargeListMixed l = new LargeListMixed();
            List<Mixed> lst = Lists.newArrayListWithCapacity(1000000);
            for (int i = 0; i < 1000000; i++) {
                lst.add(create(Mixed.class));
            }
            l.lst = lst;
            return (T)l;
        }
        return null;
    }

    @State(Scope.Thread)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public static class Serialization
    {
        private ThriftCodecManager codecManager = new ThriftCodecManager();
        private TProtocol protocol;

        @Param({
                "com.facebook.swift.benchmark.structs.Empty",
                "com.facebook.swift.benchmark.structs.SmallInt",
                "com.facebook.swift.benchmark.structs.BigInt",
                "com.facebook.swift.benchmark.structs.SmallString",
                "com.facebook.swift.benchmark.structs.BigString",
                "com.facebook.swift.benchmark.structs.BigBinary",
                "com.facebook.swift.benchmark.structs.LargeBinary",
                "com.facebook.swift.benchmark.structs.Mixed",
                "com.facebook.swift.benchmark.structs.SmallListInt",
                "com.facebook.swift.benchmark.structs.BigListInt",
                "com.facebook.swift.benchmark.structs.BigListMixed",
                "com.facebook.swift.benchmark.structs.LargeListMixed",
        })
        private String structClassAsString;
        private Class structClass;

        @Setup(Level.Invocation)
        public void setup() throws Exception
        {
            structClass = Class.forName(structClassAsString);
            TNiftyTransport transport = new TNiftyTransport(null, new ThriftMessage(ChannelBuffers.EMPTY_BUFFER, ThriftTransportType.UNFRAMED));
            protocol = new TCompactProtocol(transport);
        }

        @Benchmark
        public void serialize() throws Exception
        {
            codecManager.write(structClass, create(structClass), protocol);
        }
    }

    @State(Scope.Thread)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public static class Deserialization
    {
        private ThriftCodecManager codecManager = new ThriftCodecManager();
        private TProtocol protocol;
        private ChannelBuffer serialized;

        @Param({
                "com.facebook.swift.benchmark.structs.Empty",
                "com.facebook.swift.benchmark.structs.SmallInt",
                "com.facebook.swift.benchmark.structs.BigInt",
                "com.facebook.swift.benchmark.structs.SmallString",
                "com.facebook.swift.benchmark.structs.BigString",
                "com.facebook.swift.benchmark.structs.BigBinary",
                "com.facebook.swift.benchmark.structs.LargeBinary",
                "com.facebook.swift.benchmark.structs.Mixed",
                "com.facebook.swift.benchmark.structs.SmallListInt",
                "com.facebook.swift.benchmark.structs.BigListInt",
                "com.facebook.swift.benchmark.structs.BigListMixed",
                "com.facebook.swift.benchmark.structs.LargeListMixed",
        })
        private String structClassAsString;
        private Class structClass;


        @Setup
        public void beforeLoopSetup() throws Exception
        {
            structClass = Class.forName(structClassAsString);
            TNiftyTransport outTransport = new TNiftyTransport(null, new ThriftMessage(ChannelBuffers.EMPTY_BUFFER, ThriftTransportType.UNFRAMED));
            codecManager.write(structClass, create(structClass), new TCompactProtocol(outTransport));
            serialized = outTransport.getOutputBuffer();
        }

        @Setup(Level.Invocation)
        public void insideLoopSetup()
        {
            TNiftyTransport inTransport = new TNiftyTransport(null, new ThriftMessage(serialized.duplicate(), ThriftTransportType.UNFRAMED));
            protocol = new TCompactProtocol(inTransport);
        }

        @Benchmark
        public Object deserialize() throws Exception
        {
            return codecManager.read(structClass, protocol);
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                //.exclude(SerializationBench.Serialization.class.getCanonicalName())
                //.exclude(SerializationBench.Deserialization.class.getCanonicalName())
                .warmupIterations(10)
                .measurementIterations(10)
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
