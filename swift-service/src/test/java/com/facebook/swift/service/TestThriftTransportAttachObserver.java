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

import com.facebook.nifty.core.NettyServerTransport;
import com.facebook.nifty.core.NiftyTimer;
import com.facebook.nifty.processor.NiftyProcessor;
import com.facebook.nifty.processor.NiftyProcessorAdapters;
import com.facebook.nifty.ssl.TransportAttachObserver;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.scribe.scribe;
import com.google.common.collect.ImmutableList;
import org.apache.thrift.TProcessor;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class TestThriftTransportAttachObserver
{
    @Test
    public void testSwiftService()
            throws Exception
    {
        SwiftScribe scribeService = new SwiftScribe();
        NiftyProcessor processor = new ThriftServiceProcessor(new ThriftCodecManager(), ImmutableList.<ThriftEventHandler>of(), scribeService);
        testProcessor(processor);
    }

    @Test
    public void testThriftService()
            throws Exception
    {
        ThriftScribeService scribeService = new ThriftScribeService();
        TProcessor processor = new scribe.Processor<>(scribeService);
        testProcessor(NiftyProcessorAdapters.processorFromTProcessor(processor));
    }

    private void testProcessor(NiftyProcessor processor)
            throws Exception
    {
        DummyTransportAttachObserver dummyTransportAttachObserver = new DummyTransportAttachObserver();
        try (ThriftServer server = new ThriftServer(
                processor,
                new ThriftServerConfig(),
                new NiftyTimer("timer"),
                ThriftServer.DEFAULT_FRAME_CODEC_FACTORIES,
                ThriftServer.DEFAULT_PROTOCOL_FACTORIES,
                ThriftServer.DEFAULT_WORKER_EXECUTORS,
                ThriftServer.DEFAULT_SECURITY_FACTORY,
                ThriftServer.DEFAULT_SSL_SERVER_CONFIGURATION,
                new ThriftServer.TransportAttachObserverHolder(dummyTransportAttachObserver)).start()) {
            assertTrue(dummyTransportAttachObserver.getState());
            server.close();
            assertFalse(dummyTransportAttachObserver.getState());
        }
    }

    public static class DummyTransportAttachObserver implements TransportAttachObserver
    {
        private boolean state;

        public void attachTransport(NettyServerTransport transport)
        {
            state = true;
        }

        public void detachTransport()
        {
            state = false;
        }

        public boolean getState()
        {
            return state;
        }
    }
}
