/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service;

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
import java.util.List;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.net.HostAndPort.fromParts;
import static org.testng.Assert.assertEquals;

/**
 * Demonstrates creating a Thrift service using Swift.
 */
public class TestThriftService
{
    @Test(groups = "fast")
    public void testSwiftService()
            throws Exception
    {
        SwiftScribe scribeService = new SwiftScribe();
        TProcessor processor = new ThriftServiceProcessor(new ThriftCodecManager(), scribeService);

        List<LogEntry> messages = testProcessor(processor);
        assertEquals(scribeService.getMessages(), newArrayList(concat(toSwiftLogEntry(messages), toSwiftLogEntry(messages))));
    }

    @Test(groups = "fast")
    public void testThriftService()
            throws Exception
    {
        ThriftScribeService scribeService = new ThriftScribeService();
        TProcessor processor = new scribe.Processor<>(scribeService);

        List<LogEntry> messages = testProcessor(processor);
        assertEquals(scribeService.getMessages(), newArrayList(concat(messages, messages)));
    }

    private List<LogEntry> testProcessor(TProcessor processor)
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
                Scribe scribe = clientManager.createClient(fromParts("localhost", port), Scribe.class)
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
}
