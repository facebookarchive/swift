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
package com.facebook.nifty.client.socks;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import java.net.InetAddress;

/**
 * static helper for dealing with socks protocol.
 *
 * @see <a href="http://en.wikipedia.org/wiki/SOCKS">http://en.wikipedia.org/wiki/SOCKS</a>
 * @see <a href="http://www.openssh.org/txt/socks4.protocol">SOCKS Protocol Version 4</a>
 */
public class SocksProtocols
{
    public static final int SOCKS_VERSION_4 = 0x04;
    public static final int CONNECT = 0x01;
    public static final int REQUEST_GRANTED = 0x5a;
    public static final int REQUEST_FAILED = 0x5b;
    public static final int REQUEST_FAILED_NO_IDENTD = 0x5c;
    public static final int REQUEST_FAILED_USERID_NOT_CONFIRMED = 0x5d;

    public static ChannelBuffer createSocks4packet(InetAddress address, int port)
    {
        if (address == null) {
            throw new IllegalArgumentException("address is null");
        }
        byte[] userBytes = System.getProperty("user.name", "").getBytes();
        ChannelBuffer handshake = ChannelBuffers.dynamicBuffer(9 + userBytes.length);
        handshake.writeByte(SOCKS_VERSION_4); // SOCKS version
        handshake.writeByte(CONNECT); // CONNECT
        handshake.writeShort(port); // port
        handshake.writeBytes(address.getAddress()); // remote address to connect to
        handshake.writeBytes(userBytes); // user name
        handshake.writeByte(0x00); // null terminating the string
        return handshake;
    }

    public static ChannelBuffer createSock4aPacket(String hostName, int port)
    {
        if (hostName == null) {
            throw new IllegalArgumentException("hostName is null");
        }
        byte[] userBytes = System.getProperty("user.name", "").getBytes();
        byte[] hostNameBytes = hostName.getBytes();
        ChannelBuffer handshake = ChannelBuffers.dynamicBuffer(10 + userBytes.length + hostNameBytes.length);
        handshake.writeByte(SOCKS_VERSION_4); // SOCKS version
        handshake.writeByte(CONNECT); // CONNECT
        handshake.writeShort(port); // port
        handshake.writeByte(0x00); // fake ip
        handshake.writeByte(0x00); // fake ip
        handshake.writeByte(0x00); // fake ip
        handshake.writeByte(0x01); // fake ip
        handshake.writeBytes(userBytes); // user name
        handshake.writeByte(0x00); // null terminating the string
        handshake.writeBytes(hostNameBytes); // remote host name to connect to
        handshake.writeByte(0x00); // null terminating the string
        return handshake;
    }
}
