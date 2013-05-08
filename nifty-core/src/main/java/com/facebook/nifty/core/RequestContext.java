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
package com.facebook.nifty.core;

import java.net.SocketAddress;

import static com.google.common.base.Preconditions.checkState;

public class RequestContext
{
    private static ThreadLocal<RequestContext> threadLocalContext = new ThreadLocal<>();
    private final SocketAddress remoteAddress;

    /**
     * Gets the thread-local {@link RequestContext} for the Thrift request that is being processed
     * on the current thread.
     *
     * Note that this method will only work properly when called from the thread on which Nifty
     * invoked your server method. If you transfer work to another thread in the course of
     * processing a request, this is not tracked by Nifty.
     *
     * @return The {@link RequestContext} of the current request
     * @throws IllegalStateException when not called on the thread on which your server * method
     * was originally invoked
     */
    public static RequestContext getCurrentContext()
    {
        RequestContext currentContext = threadLocalContext.get();
        checkState(currentContext != null,
                   "Cannot only get a RequestContext when running inside a Thrift handler");
        return currentContext;
    }

    /**
     * Gets the remote address of the client that made the request
     *
     * @return The client's remote address as a {@link SocketAddress}
     */
    public SocketAddress getRemoteAddress()
    {
        return remoteAddress;
    }

    // Contexts are only created, set, and cleared internally by Nifty

    RequestContext(SocketAddress remoteAddress)
    {
        this.remoteAddress = remoteAddress;
    }

    static void setCurrentContext(RequestContext requestContext)
    {
        threadLocalContext.set(requestContext);
    }

    static void clearCurrentContext()
    {
        threadLocalContext.remove();
    }
}
