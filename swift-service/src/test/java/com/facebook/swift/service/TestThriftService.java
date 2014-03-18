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
package com.facebook.swift.service;

import com.facebook.nifty.client.FramedClientConnector;
import com.facebook.nifty.core.RequestContext;
import com.facebook.nifty.processor.NiftyProcessor;
import com.facebook.nifty.processor.NiftyProcessorAdapters;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.scribe.LogEntry;
import com.facebook.swift.service.scribe.ResultCode;
import com.facebook.swift.service.scribe.scribe;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.List;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.net.HostAndPort.fromParts;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Demonstrates creating a Thrift service using Swift.
 */
public class TestThriftService
{
    @Test
    public void testSwiftService()
            throws Exception
    {
        SwiftScribe scribeService = new SwiftScribe();
        NiftyProcessor processor = new ThriftServiceProcessor(new ThriftCodecManager(), ImmutableList.<ThriftEventHandler>of(), scribeService);

        List<LogEntry> messages = testProcessor(processor);
        assertEquals(scribeService.getMessages(), newArrayList(concat(toSwiftLogEntry(messages), toSwiftLogEntry(messages))));
    }

    @Test
    public void testThriftService()
            throws Exception
    {
        ThriftScribeService scribeService = new ThriftScribeService();
        TProcessor processor = new scribe.Processor<>(scribeService);

        List<LogEntry> messages = testProcessor(processor);
        assertEquals(scribeService.getMessages(), newArrayList(concat(messages, messages)));
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Multiple @ThriftMethod-annotated methods named.*")
    public void testConflictingServices()
            throws Exception
    {
        new ThriftServiceProcessor(new ThriftCodecManager(), ImmutableList.<ThriftEventHandler>of(),
                new SwiftScribe(), new ConflictingLogService());
    }

    private List<LogEntry> testProcessor(TProcessor processor) throws Exception
    {
        return testProcessor(NiftyProcessorAdapters.processorFromTProcessor(processor));
    }

    private List<LogEntry> testProcessor(NiftyProcessor processor)
            throws Exception
    {
        ImmutableList<LogEntry> messages = ImmutableList.of(
                new LogEntry("hello", "world"),
                new LogEntry("bye", "world")
        );

        try (ThriftServer server = new ThriftServer(processor).start()) {
            assertEquals(logThrift(server.getPort(), messages), ResultCode.OK);
            assertEquals(logSwift(server.getPort(), toSwiftLogEntry(messages)), com.facebook.swift.service.ResultCode.OK);
        }

        return messages;
    }

    private ResultCode logThrift(int port, List<LogEntry> messages)
            throws TException
    {
        TSocket socket = new TSocket("localhost", port);
        socket.open();
        try {
            TBinaryProtocol tp = new TBinaryProtocol(new TFramedTransport(socket));
            return new scribe.Client(tp).Log(messages);
        }
        finally {
            socket.close();
        }
    }

    private com.facebook.swift.service.ResultCode logSwift(int port, List<com.facebook.swift.service.LogEntry> entries)
            throws Exception
    {
        try (
                ThriftClientManager clientManager = new ThriftClientManager();
                Scribe scribe = clientManager.createClient(
                        new FramedClientConnector(fromParts("localhost", port)),
                        Scribe.class).get()
        ) {
            return scribe.log(entries);
        }
    }

    private List<com.facebook.swift.service.LogEntry> toSwiftLogEntry(List<LogEntry> messages)
    {
        return Lists.transform(messages, new Function<LogEntry, com.facebook.swift.service.LogEntry>()
        {
            @Override
            public com.facebook.swift.service.LogEntry apply(@Nullable LogEntry input)
            {
                return new com.facebook.swift.service.LogEntry(input.category, input.message);
            }
        });
    }

    @ThriftService
    public class ConflictingLogService
    {
        @ThriftMethod
        public void Log(List<String> messages) throws TException
        {
        }
    }

    static class EventHandler extends ThriftEventHandler
    {
        private final boolean niftyProcessor;
        private int getContextCounter = 0, preReadCounter = 0, postReadCounter = 0,
                preWriteCounter = 0, postWriteCounter = 0;
        private final List<Object> ctxs = newArrayList();

        EventHandler(boolean niftyProcessor)
        {
            this.niftyProcessor = niftyProcessor;
        }

        public boolean validate(int count)
        {
            return getContextCounter == count && preReadCounter == count && postReadCounter == count &&
                    preWriteCounter == count && postWriteCounter == count;
        }

        @Override
        public Object getContext(String methodName, RequestContext requestContext)
        {
            assertEquals(methodName, "scribe.Log");
            if (niftyProcessor) {
                assertNotNull(requestContext);
                assertTrue(((InetSocketAddress)requestContext.getConnectionContext().getRemoteAddress()).getAddress().isLoopbackAddress());
            } else {
                assertNull(requestContext);
            }
            Object ctx = new Object();
            ctxs.add(ctx);
            getContextCounter++;
            return ctx;
        }

        @Override
        public void preRead(Object context, String methodName)
        {
            assertEquals(methodName, "scribe.Log");
            assertEquals(context, ctxs.get(preReadCounter++));
        }

        @Override
        public void postRead(Object context, String methodName, Object[] args)
        {
            assertEquals(methodName, "scribe.Log");
            assertEquals(context, ctxs.get(postReadCounter++));
            assertEquals(args.length, 1);
            assertTrue(args[0] instanceof List);
        }

        @Override
        public void preWrite(Object context, String methodName, Object result)
        {
            assertEquals(methodName, "scribe.Log");
            assertEquals(context, ctxs.get(preWriteCounter++));
            assertTrue(result instanceof com.facebook.swift.service.ResultCode);
        }

        @Override
        public void postWrite(Object context, String methodName, Object result)
        {
            assertEquals(methodName, "scribe.Log");
            assertEquals(context, ctxs.get(postWriteCounter++));
            assertTrue(result instanceof com.facebook.swift.service.ResultCode);
        }
    }

    public void swiftEventHandlerTester(boolean niftyProcessor) throws Exception
    {
        SwiftScribe scribeService = new SwiftScribe();
        EventHandler eventHandler = new EventHandler(niftyProcessor);
        EventHandler secondHandler = new EventHandler(niftyProcessor);
        List<EventHandler> handlers = ImmutableList.of(eventHandler, secondHandler);
        final ThriftServiceProcessor processor = new ThriftServiceProcessor(new ThriftCodecManager(), handlers, scribeService);

        List<LogEntry> messages = niftyProcessor ?
                testProcessor(processor) : testProcessor(NiftyProcessorAdapters.processorToTProcessor(processor));
        assertEquals(scribeService.getMessages(), newArrayList(concat(toSwiftLogEntry(messages), toSwiftLogEntry(messages))));
        assertTrue(eventHandler.validate(2));
        assertTrue(secondHandler.validate(2));
    }

    @Test
    public void testSwiftEventHandlersWithNiftyProcessor() throws Exception
    {
        swiftEventHandlerTester(true);
    }

    @Test
    public void testSwiftEventHandlersWithTProcessor() throws Exception
    {
        swiftEventHandlerTester(false);
    }
}
