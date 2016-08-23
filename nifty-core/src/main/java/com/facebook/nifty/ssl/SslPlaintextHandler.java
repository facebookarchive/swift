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
package com.facebook.nifty.ssl;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.ssl.SslHandler;

public class SslPlaintextHandler extends FrameDecoder {

    private final SslHandler sslHandler;
    private final String sslHandlerName;

    public SslPlaintextHandler(SslHandler sslHandler, String sslHandlerName) {
        this.sslHandler = sslHandler;
        this.sslHandlerName = sslHandlerName;
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        // delay channel connects until we know what kind of transport we have.
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        if (buffer.readableBytes() < 9) {
            return null;
        }

        if (looksLikeTLS(buffer)) {
            ctx.getPipeline().addAfter(ctx.getName(), sslHandlerName, sslHandler);
        } else {
            // If the SSL handler is not used, close the ssl engine. This will clean up any native structures
            // that the ssl engine holds on to.
            sslHandler.getEngine().closeOutbound();
        }

        ctx.getPipeline().remove(this);
        Channels.fireChannelConnected(ctx, ctx.getPipeline().getChannel().getRemoteAddress());
        Channels.fireMessageReceived(ctx, buffer, ctx.getPipeline().getChannel().getRemoteAddress());
        return null;
    }


    // Requires 9 bytes of input.
    private static boolean looksLikeTLS(ChannelBuffer buffer) {
        // TLS starts as
        // 0: 0x16 - handshake protocol magic
        // 1: 0x03 - SSL version major
        // 2: 0x00 to 0x03 - SSL version minor (SSLv3 or TLS1.0 through TLS1.2)
        // 3-4: length (2 bytes)
        // 5: 0x01 - handshake type (ClientHello)
        // 6-8: handshake len (3 bytes), equals value from offset 3-4 minus 4

        // Framed binary starts as
        // 0-3: frame len
        // 4: 0x80 - binary magic
        // 5: 0x01 - protocol version
        // 6-7: various
        // 8-11: method name len

        // Other Thrift transports/protocols can't conflict because they don't have
        // 16-03-01 at offsets 0-1-5.

        // Definitely not TLS
        if (buffer.getByte(0) != 0x16 || buffer.getByte(1) != 0x03 || buffer.getByte(5) != 0x01) {
            return false;
        }

        // This is most likely TLS, but could be framed binary, which has 80-01
        // at offsets 4-5.
        if (buffer.getByte(4) == 0x80 && buffer.getByte(8) != 0x7c) {
            // Binary will have the method name length at offsets 8-11, which must be
            // smaller than the frame length at 0-3, so byte 8 is <=  byte 0,
            // which is 0x16.
            // However, for TLS, bytes 6-8 (24 bits) are the length of the
            // handshake protocol and this value is 4 less than the record-layer
            // length at offset 3-4 (16 bits), so byte 8 equals 0x7c (0x80 - 4),
            // which is not smaller than 0x16
            return false;
        }
        return true;
    }
}
