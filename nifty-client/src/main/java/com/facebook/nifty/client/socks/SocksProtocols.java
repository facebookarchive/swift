package com.facebook.nifty.client.socks;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import java.net.InetAddress;

/**
 * static helper for dealing with socks protocol.
 */
public class SocksProtocols {

  public static ChannelBuffer createSocks4packet(InetAddress address, int port) {
    if(address == null) throw new IllegalArgumentException("address is null");
    String user = System.getProperty("user.name", "");
    ChannelBuffer handshake = ChannelBuffers.dynamicBuffer(9 + user.length());
    handshake.writeByte(0x04);
    handshake.writeByte(0x01);
    handshake.writeShort(port);
    handshake.writeBytes(address.getAddress());
    handshake.writeBytes(user.getBytes());
    handshake.writeByte(0x00);
    return handshake;
  }

  public static ChannelBuffer createSock4aPacket(String hostName, int port) {
    if(hostName == null) throw new IllegalArgumentException("hostName is null");
    String user = System.getProperty("user.name", "");
    ChannelBuffer handshake = ChannelBuffers.dynamicBuffer(10 + user.length() + hostName.length());
    handshake.writeByte(0x04);
    handshake.writeByte(0x01);
    handshake.writeShort(port);
    handshake.writeByte(0x00);
    handshake.writeByte(0x00);
    handshake.writeByte(0x00);
    handshake.writeByte(0x01);
    handshake.writeBytes(user.getBytes());
    handshake.writeByte(0x00);
    handshake.writeBytes(hostName.getBytes());
    handshake.writeByte(0x00);
    return handshake;
  }
}
