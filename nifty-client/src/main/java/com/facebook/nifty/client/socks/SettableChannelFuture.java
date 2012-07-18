package com.facebook.nifty.client.socks;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.DefaultChannelFuture;

/**
 * A channel future that allow channel to be set at a later time.
 */
public class SettableChannelFuture extends DefaultChannelFuture {
  private Channel settableChannel = null;
  private boolean channelIsSet = false;

  public SettableChannelFuture() {
    super(null, false);
  }

  public void setChannel(Channel channel) {
    if (!channelIsSet) {
      this.settableChannel = channel;
      this.channelIsSet = true;
    }
  }

  @Override
  public Channel getChannel() {
    return this.settableChannel;
  }

  @Override
  public boolean setFailure(Throwable cause) {
    if (!this.channelIsSet) throw new IllegalStateException("channel not set yet !");
    return super.setFailure(cause);
  }

  @Override
  public boolean setSuccess() {
    if (!this.channelIsSet) throw new IllegalStateException("channel not set yet !");
    return super.setSuccess();
  }
}
