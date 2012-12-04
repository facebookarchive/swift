/*
 * Copyright (C) 2012 Facebook, Inc.
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

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.protocol.TProtocolUtil;
import org.apache.thrift.protocol.TType;
import org.apache.thrift.transport.TTransport;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;

public class ThriftFrameDecoder extends FrameDecoder {
    public static final int MESSAGE_FRAME_SIZE = 4;
    private final int maxFrameSize;
    private final TProtocolFactory inputProtocolFactory;

    public ThriftFrameDecoder(int maxFrameSize, TProtocolFactory inputProtocolFactory) {
        this.maxFrameSize = maxFrameSize;
        this.inputProtocolFactory = inputProtocolFactory;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer)
            throws Exception {
        if (!buffer.readable()) {
            return null;
        }

        short firstByte = buffer.getUnsignedByte(0);
        if (firstByte >= 0x80) {
            // A non-zero MSB for the first byte of the message implies the message starts with a
            // protocol id (and thus it is unframed).
            return tryDecodeUnframedMessage(ctx, channel, buffer);
        } else if (buffer.readableBytes() < MESSAGE_FRAME_SIZE) {
            // Expecting a framed message, but not enough bytes available to read the frame size
            return null;
        } else {
            // Messages with a zero MSB in the first byte are framed messages
            return tryDecodeFramedMessage(ctx, channel, buffer);
        }

    }

    private TTransport tryDecodeFramedMessage(ChannelHandlerContext ctx, Channel channel,
                                              ChannelBuffer buffer) {
        // Framed messages are prefixed by the size of the frame (which doesn't include the
        // framing itself).

        int messageStartReaderIndex = buffer.readerIndex();
        // Read the i32 frame contents size
        int messageContentsLength = buffer.getInt(messageStartReaderIndex);
        // The full message is larger by the size of the frame size prefix
        int messageLength = messageContentsLength + MESSAGE_FRAME_SIZE;

        if (messageContentsLength > maxFrameSize) {
            Channels.fireExceptionCaught(
                    ctx,
                    new TooLongFrameException("Maximum frame size of " + maxFrameSize +
                                              " exceeded")
            );
        }

        int messageContentsOffset = messageStartReaderIndex + MESSAGE_FRAME_SIZE;
        if (messageLength == 0) {
            // Zero-sized frame: just ignore it and return nothing
            buffer.readerIndex(messageContentsOffset);
            return null;
        }
        else if (buffer.readableBytes() < messageLength) {
            // Full message isn't available yet, return nothing for now
            return null;
        }
        else {
            // Full message is available, return it
            ChannelBuffer messageBuffer = extractFrame(buffer,
                                                       messageContentsOffset,
                                                       messageContentsLength);
            buffer.readerIndex(messageStartReaderIndex + messageLength);
            return new TNiftyTransport(channel, messageBuffer, ThriftTransportType.FRAMED);
        }
    }

    private TTransport tryDecodeUnframedMessage(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer)
            throws TException {
        // Perform a trial decode, skipping through
        // the fields, to see whether we have an entire message available.

        int messageLength = 0;
        int messageStartReaderIndex = buffer.readerIndex();

        try {
            TNiftyTransport decodeAttemptTransport =
                    new TNiftyTransport(channel, buffer, ThriftTransportType.UNFRAMED);
            TProtocol inputProtocol =
                    this.inputProtocolFactory.getProtocol(decodeAttemptTransport);

            // Skip through the message
            inputProtocol.readMessageBegin();
            TProtocolUtil.skip(inputProtocol, TType.STRUCT);
            inputProtocol.readMessageEnd();

            messageLength = buffer.readerIndex() - messageStartReaderIndex;
        } catch (IndexOutOfBoundsException e) {
            // No complete message was decoded: ran out of bytes
            return null;
        } finally {
            if (buffer.readerIndex() - messageStartReaderIndex > maxFrameSize) {
                Channels.fireExceptionCaught(
                        ctx,
                        new TooLongFrameException("Maximum frame size of " + maxFrameSize +
                                                  " exceeded")
                );
            }

            buffer.readerIndex(messageStartReaderIndex);
        }

        if (messageLength <= 0) {
            return null;
        }

        // We have a full message in the read buffer, slice it off
        ChannelBuffer messageBuffer =
                extractFrame(buffer, messageStartReaderIndex, messageLength);
        buffer.readerIndex(messageStartReaderIndex + messageLength);
        return new TNiftyTransport(channel, messageBuffer, ThriftTransportType.UNFRAMED);
    }

    protected ChannelBuffer extractFrame(ChannelBuffer buffer, int index, int length) {
        // Slice should be sufficient here (and avoids the copy in LengthFieldBasedFrameDecoder)
        // because we know no one is going to modify the contents in the read buffers.
        return buffer.slice(index, length);
    }
}