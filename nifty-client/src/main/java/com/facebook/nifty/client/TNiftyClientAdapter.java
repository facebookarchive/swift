package com.facebook.nifty.client;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ExceptionEvent;

public abstract class TNiftyClientAdapter implements TNiftyClientListener {
  @Override
  public void onFrameRead(Channel channel, ChannelBuffer buffer) {
    onInput(new TNiftyReadOnlyTransport(channel, buffer));
  }

  @Override
  public void onChannelClosedOrDisconnected(Channel channel) {
  }

  @Override
  public void onExceptionEvent(ExceptionEvent e) {
  }

  /**
   * called when a frame is ready to be read.
   * @param tNiftyReadOnlyTransport a one-time-use transport for the frame
   */
  public abstract void onInput(TNiftyReadOnlyTransport tNiftyReadOnlyTransport);

}
