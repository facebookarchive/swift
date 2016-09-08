/*
 * Copyright (C) 2012-2016 Facebook, Inc.
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

import com.facebook.nifty.duplex.TDuplexProtocolFactory;
import io.airlift.units.Duration;
import org.apache.thrift.TException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;

import javax.annotation.Nullable;

public interface NiftyClientChannel extends RequestChannel {
    /**
     * Sets a timeout used to limit elapsed time for sending a message.
     *
     * @param sendTimeout
     */
    void setSendTimeout(@Nullable Duration sendTimeout);

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
    void setReceiveTimeout(@Nullable Duration receiveTimeout);

    /**
     * Returns the timeout most recently set by
     * {@link NiftyClientChannel#setReceiveTimeout(io.airlift.units.Duration)}
     *
     * @return
     */
    Duration getReceiveTimeout();

    /**
     * Sets a timeout used to limit the time that the client waits for data to be sent by the server.
     *
     * @param readTimeout
     */
    void setReadTimeout(@Nullable Duration readTimeout);

    /**
     * Returns the timeout most recently set by
     * {@link NiftyClientChannel#setReadTimeout(io.airlift.units.Duration)}
     *
     * @return
     */
    Duration getReadTimeout();

    /**
     * Executes the given {@link Runnable} on the I/O thread that manages reads/writes for this
     * channel.
     *
     * @param runnable
     */
    void executeInIoThread(Runnable runnable);

    Channel getNettyChannel();
}
