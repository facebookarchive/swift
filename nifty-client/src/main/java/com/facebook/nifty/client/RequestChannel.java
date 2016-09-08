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
import org.apache.thrift.TException;
import org.jboss.netty.buffer.ChannelBuffer;

public interface RequestChannel
{
    /**
     * Sends a single message asynchronously, and notifies the {@link Listener}
     * when the request is finished sending, when the response has arrived, and/or when an error
     * occurs.
     *
     * @param request
     * @param oneway
     * @param listener
     * @throws TException
     */
    void sendAsynchronousRequest(final ChannelBuffer request,
            final boolean oneway,
            final Listener listener)
            throws TException;

    /**
     * Closes the channel
     */
    void close();

    /**
     * Checks whether the channel has encountered an error. This method is a shortcut for:
     *
     * <code>
     * return (getError() != null);
     * </code>
     *
     * @return {@code true} if the {@link RequestChannel} is broken
     */
    boolean hasError();

    /**
     * Returns the {@link TException} representing the error the channel encountered, if any.
     *
     * @return An instance of {@link TException} or {@code null} if the channel is still good.
     */
    TException getError();

    /**
     * Returns the {@link TDuplexProtocolFactory} that should be used by clients code to
     * serialize messages for sending on this channel
     *
     * @return An instance of {@link TDuplexProtocolFactory}
     */
    TDuplexProtocolFactory getProtocolFactory();

    /**
     * The listener interface that must be implemented for callback objects passed to
     * {@link #sendAsynchronousRequest}
     */
    public interface Listener {
        /**
         * This will be called when the request has successfully been written to the transport
         * layer (e.g. socket)
         */
        void onRequestSent();

        /**
         * This will be called when a full response to the request has been received
         *
         * @param message The response buffer
         */
        void onResponseReceived(ChannelBuffer message);

        /**
         * This will be called if the channel encounters an error before the request is sent or
         * before a response is received
         *
         * @param requestException A {@link TException} describing the problem that was encountered
         */
        void onChannelError(TException requestException);
    }
}
