package com.facebook.nifty.client;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;

public interface TNiftyClientListener {
  /**
   * Called when a full frame as defined in TFramedTransport is available.
   *
   * @param channel the channel
   * @param buffer the payload of the frame, without the leading 4-bytes length header
   */
  void onFrameRead(Channel channel, ChannelBuffer buffer) ;
}
