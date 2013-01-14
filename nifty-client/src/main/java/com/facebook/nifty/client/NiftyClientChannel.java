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
package com.facebook.nifty.client;

import io.airlift.units.Duration;
import org.apache.thrift.TException;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.util.Timer;

public interface NiftyClientChannel {
    /**
     * Sends a single message asynchronously, and notifies the {@link Listener}
     * when the request is finished sending, when the response has arrived, and/or when an error
     * occurs.
     *
     *
     * @param request
     * @param oneway
     *@param listener  @throws TException
     */
    void sendAsynchronousRequest(final ChannelBuffer request,
                                 final boolean oneway,
                                 final Listener listener)
            throws TException;

    /**
     * Sets a timeout used to limit elapsed time for sending a message.
     *
     * @param sendTimeout
     */
    void setSendTimeout(Duration sendTimeout);

    /**
     * Returns the timeout most recently set by
     * {@link NiftyClientChannel#setSendTimeout(io.airlift.units.Duration)}
     *
     * @return
     */
    Duration getSendTimeout();

    /**
     * Sets a timeout used to limit elapsed time between successful send, and reception of the
     * response.
     *
     * @param receiveTimeout
     */
    void setReceiveTimeout(Duration receiveTimeout);

    /**
     * Returns the timeout most recently set by
     * {@link NiftyClientChannel#setReceiveTimeout(io.airlift.units.Duration)}
     *
     * @return
     */
    Duration getReceiveTimeout();

    /**
     * Closes the channel
     */
    void close();

    /**
     * Returns true if the channel has encountered an error. This method is a shortcut for:
     * <p/>
     * {@code return (getError() != null);}
     *
     * @return
     */
    boolean hasError();

    /**
     * Returns the {@link TException} representing the error the channel encountered, if any.
     *
     * @return
     */
    TException getError();

    /**
     * Executes the given {@link Runnable} on the I/O thread that manages reads/writes for this
     * channel.
     *
     * @param runnable
     */
    void executeInIoThread(Runnable runnable);

    Channel getNettyChannel();

    public interface Listener {
        public abstract void onRequestSent();

        public abstract void onResponseReceived(ChannelBuffer message);

        public abstract void onChannelError(TException t);
    }
}
