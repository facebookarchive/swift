/*
 * Copyright (C) 2012-2013 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.nifty.core;

import com.facebook.nifty.codec.DefaultThriftFrameDecoder;
import com.facebook.nifty.codec.ThriftFrameDecoder;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TField;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TStruct;
import org.apache.thrift.protocol.TType;
import org.apache.thrift.transport.TFramedTransport;
import org.easymock.EasyMock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.AbstractChannelSink;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DefaultChannelConfig;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

public class TestThriftFrameDecoder
{
    private Channel channel;
    private AtomicInteger messagesReceived;
    private AtomicInteger exceptionsCaught;

    private static final int MAX_FRAME_SIZE = 1024;
    public static final int MESSAGE_CHUNK_SIZE = 10;

    // Send an empty buffer and make sure nothing breaks, and
    @Test
    public void testDecodeEmptyBuffer() throws Exception
    {
        Channels.fireMessageReceived(channel, ChannelBuffers.EMPTY_BUFFER);

        Assert.assertEquals(exceptionsCaught.get(), 0);
        Assert.assertEquals(messagesReceived.get(), 0);
    }

    // Send two unframed messages in a single buffer, and check they both get decoded
    @Test
    public void testDecodeUnframedMessages() throws Exception
    {
        TChannelBufferOutputTransport transport = new TChannelBufferOutputTransport();
        TBinaryProtocol protocol = new TBinaryProtocol(transport);

        writeTestMessages(protocol, 2);

        Channels.fireMessageReceived(channel, transport.getOutputBuffer());

        Assert.assertEquals(exceptionsCaught.get(), 0);
        Assert.assertEquals(messagesReceived.get(), 2);
    }

    // Send two framed messages in a single buffer, and check they both get decoded
    @Test
    public void testDecodeFramedMessages() throws Exception
    {
        TChannelBufferOutputTransport transport = new TChannelBufferOutputTransport();
        TBinaryProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));

        writeTestMessages(protocol, 2);

        Channels.fireMessageReceived(channel, transport.getOutputBuffer());

        Assert.assertEquals(messagesReceived.get(), 2);
    }

    // Send three unframed messages, chunked into 10-byte buffers and make sure they all get decoded
    @Test
    public void testDecodeChunkedUnframedMessages() throws Exception
    {
        TChannelBufferOutputTransport transport = new TChannelBufferOutputTransport();
        TBinaryProtocol protocol = new TBinaryProtocol(transport);

        writeTestMessages(protocol, 3);

        sendMessagesInChunks(channel, transport, MESSAGE_CHUNK_SIZE);

        Assert.assertEquals(messagesReceived.get(), 3);
    }

    // Send three framed messages, chunked into 10-byte buffers and make sure they all get decoded
    @Test
    public void testDecodeChunkedFramedMessages() throws Exception
    {
        TChannelBufferOutputTransport transport = new TChannelBufferOutputTransport();
        TBinaryProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));

        writeTestMessages(protocol, 3);

        sendMessagesInChunks(channel, transport, MESSAGE_CHUNK_SIZE);

        Assert.assertEquals(messagesReceived.get(), 3);
    }

    private void sendMessagesInChunks(Channel channel,
                                      TChannelBufferOutputTransport transport,
                                      int chunkSize)
    {
        ChannelBuffer buffer = transport.getOutputBuffer();
        while (buffer.readable()) {
            ChannelBuffer chunk = buffer.readSlice(Math.min(chunkSize, buffer.readableBytes()));
            Channels.fireMessageReceived(channel, chunk);
        }
    }

    private void writeTestMessages(TBinaryProtocol protocol, int count)
            throws TException
    {
        for (int i = 0; i < count; i++) {
            protocol.writeMessageBegin(new TMessage("testmessage" + i, TMessageType.CALL, i));
            {
                protocol.writeStructBegin(new TStruct());
                {
                    protocol.writeFieldBegin(new TField("i32field", TType.I32, (short) 1));
                    protocol.writeI32(123);
                    protocol.writeFieldEnd();
                }
                {
                    protocol.writeFieldBegin(new TField("strfield", TType.STRING, (short) 2));
                    protocol.writeString("foo");
                    protocol.writeFieldEnd();
                }
                {
                    protocol.writeFieldBegin(new TField("boolfield", TType.BOOL, (short) 3));
                    protocol.writeBool(true);
                    protocol.writeFieldEnd();
                }
                protocol.writeFieldStop();
                protocol.writeStructEnd();
            }
            protocol.writeMessageEnd();
            protocol.getTransport().flush();
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp()
    {
        ThriftFrameDecoder decoder = new DefaultThriftFrameDecoder(MAX_FRAME_SIZE,
                                                                   new TBinaryProtocol.Factory());
        ChannelPipeline pipeline = Channels.pipeline(
                decoder,
                new SimpleChannelUpstreamHandler()
                {
                    @Override
                    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
                            throws Exception
                    {
                        messagesReceived.incrementAndGet();
                        super.messageReceived(ctx, e);
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception
                    {
                        exceptionsCaught.incrementAndGet();
                        super.exceptionCaught(ctx, e);
                    }
                }
        );

        InetSocketAddress remoteAddress = new InetSocketAddress("localhost", 1234);

        exceptionsCaught = new AtomicInteger(0);
        messagesReceived = new AtomicInteger(0);

        channel = EasyMock.createMock(Channel.class);
        EasyMock.expect(channel.getRemoteAddress()).andReturn(remoteAddress).anyTimes();
        EasyMock.expect(channel.getPipeline()).andReturn(pipeline).anyTimes();
        EasyMock.expect(channel.getConfig()).andReturn(new DefaultChannelConfig()).anyTimes();
        EasyMock.replay(channel);

        pipeline.attach(channel, new AbstractChannelSink()
        {
            @Override
            public void eventSunk(ChannelPipeline pipeline, ChannelEvent e) { return; }
        });
    }
}
