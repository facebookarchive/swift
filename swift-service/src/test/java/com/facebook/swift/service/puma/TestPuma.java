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
package com.facebook.swift.service.puma;

import com.facebook.nifty.client.FramedClientConnector;
import com.facebook.nifty.processor.NiftyProcessor;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.ThriftEventHandler;
import com.facebook.swift.service.ThriftServer;
import com.facebook.swift.service.ThriftServiceProcessor;
import com.facebook.swift.service.puma.swift.PumaReadServer;
import com.facebook.swift.service.puma.swift.PumaReadService;
import com.facebook.swift.service.puma.swift.ReadQueryInfoTimeString;
import com.facebook.swift.service.puma.swift.ReadResultQueryInfoTimeString;
import com.facebook.swift.service.puma.swift.ReadSemanticException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import org.testng.annotations.Test;

import java.util.List;

import static com.google.common.net.HostAndPort.fromParts;
import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * Demonstrates creating Puma.
 */
public class TestPuma
{
    public static final ImmutableList<ReadQueryInfoTimeString> PUMA_REQUEST = ImmutableList.of(
            new ReadQueryInfoTimeString(
                    "foo",
                    "now",
                    "later",
                    42,
                    ImmutableMap.of("a", "b"),
                    ImmutableList.of("apple", "banana")
            ),
            new ReadQueryInfoTimeString(
                    "bar",
                    "snack",
                    "attack",
                    33,
                    ImmutableMap.of("c", "d"),
                    ImmutableList.of("cheetos", "doritos")
            )
    );

    @Test
    public void testPumaDirect()
            throws Exception
    {
        PumaReadServer puma = new PumaReadServer();

        List<ReadResultQueryInfoTimeString> results = puma.getResultTimeString(PUMA_REQUEST);
        verifyPumaResults(results);
    }

    @Test
    public void testPumaSwift()
            throws Exception
    {
        // create server and start
        PumaReadServer puma = new PumaReadServer();
        ThriftServiceProcessor processor = new ThriftServiceProcessor(new ThriftCodecManager(), ImmutableList.<ThriftEventHandler>of(), puma);

        // create server and client
        try (
                ThriftServer server = new ThriftServer(processor).start();
                ThriftClientManager clientManager = new ThriftClientManager();
                PumaReadService pumaClient = clientManager.createClient(
                        new FramedClientConnector(fromParts("localhost", server.getPort())),
                        PumaReadService.class).get()
        ) {
            // invoke puma
            List<ReadResultQueryInfoTimeString> results = pumaClient.getResultTimeString(PUMA_REQUEST);
            verifyPumaResults(results);
        }
    }

    @Test
    public void testPumaDirectException()
            throws Exception
    {
        PumaReadServer puma = new PumaReadServer();
        ReadSemanticException exception = new ReadSemanticException("my exception");
        puma.setException(exception);

        try {
            puma.getResultTimeString(PUMA_REQUEST);
            fail("Expected ReadSemanticException");
        }
        catch (ReadSemanticException e) {
            assertEquals(e, exception);
        }
    }

    @Test
    public void testPumaSwiftException()
            throws Exception
    {
        PumaReadServer puma = new PumaReadServer();
        ReadSemanticException exception = new ReadSemanticException("my exception");
        puma.setException(exception);

        NiftyProcessor processor = new ThriftServiceProcessor(new ThriftCodecManager(), ImmutableList.<ThriftEventHandler>of(), puma);
        try (
                ThriftServer server = new ThriftServer(processor).start();
                ThriftClientManager clientManager = new ThriftClientManager();
                PumaReadService pumaClient = clientManager.createClient(
                        new FramedClientConnector(HostAndPort.fromParts("localhost", server.getPort())),
                        PumaReadService.class).get()
        ) {
            pumaClient.getResultTimeString(PUMA_REQUEST);
            fail("Expected ReadSemanticException");
        }
        catch (ReadSemanticException e) {
            assertEquals(e, exception);
        }
    }

    public static void verifyPumaResults(List<ReadResultQueryInfoTimeString> results)
    {
        assertThat(results)
                .as("results")
                .hasSize(2)
                .containsSequence(
                        new ReadResultQueryInfoTimeString("now", ImmutableMap.of("apple", "apple", "banana", "banana")),
                        new ReadResultQueryInfoTimeString("snack", ImmutableMap.of("cheetos", "cheetos", "doritos", "doritos"))
                );
    }
}
