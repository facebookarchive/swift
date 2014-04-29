/*
 * Copyright (C) 2012 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.swift.service;

import com.facebook.nifty.codec.ThriftFrameCodecFactory;
import com.facebook.nifty.duplex.TDuplexProtocolFactory;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.airlift.configuration.Config;
import io.airlift.units.DataSize;
import io.airlift.units.Duration;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.airlift.units.DataSize.Unit.MEGABYTE;
import static java.util.concurrent.Executors.newFixedThreadPool;

public class ThriftServerConfig
{
    private static final int DEFAULT_BOSS_THREAD_COUNT = 1;
    private static final int DEFAULT_IO_WORKER_THREAD_COUNT = 2 * Runtime.getRuntime().availableProcessors();
    private static final int DEFAULT_WORKER_THREAD_COUNT = 200;

    private String bindAddress = "localhost";
    private int port;
    private int acceptBacklog = 1024;
    private int connectionLimit;
    private int acceptorThreadCount = DEFAULT_BOSS_THREAD_COUNT;
    private int ioThreadCount = DEFAULT_IO_WORKER_THREAD_COUNT;
    private Duration idleConnectionTimeout = Duration.valueOf("60s");
    private Optional<Integer> workerThreads = Optional.absent();
    private Optional<ExecutorService> workerExecutor = Optional.absent();
    private Optional<String> workerExecutorKey = Optional.absent();
    private String transportName = "framed";
    private String protocolName = "binary";
    /**
     * The default maximum allowable size for a single incoming thrift request or outgoing thrift
     * response. A server can configure the actual maximum to be much higher (up to 0x7FFFFFFF or
     * almost 2 GB). This default could also be safely bumped up, but 64MB is chosen simply
     * because it seems reasonable that if you are sending requests or responses larger than
     * that, it should be a conscious decision (something you must manually configure).
     */
    private DataSize maxFrameSize = new DataSize(64, MEGABYTE);

    public String getBindAddress()
    {
        return bindAddress;
    }

    @Config("thrift.bind-address")
    public ThriftServerConfig setBindAddress(String bindAddress)
    {
        this.bindAddress = bindAddress;
        return this;
    }

    @Min(0)
    @Max(65535)
    public int getPort()
    {
        return port;
    }

    @Config("thrift.port")
    public ThriftServerConfig setPort(int port)
    {
        this.port = port;
        return this;
    }

    public DataSize getMaxFrameSize()
    {
        return maxFrameSize;
    }

    /**
     * Sets a maximum frame size
     * @param maxFrameSize
     * @return
     */
    @Config("thrift.max-frame-size")
    public ThriftServerConfig setMaxFrameSize(DataSize maxFrameSize)
    {
        this.maxFrameSize = maxFrameSize;
        return this;
    }

    /**
     * Sets the number of pending connections that the {@link java.net.ServerSocket} will
     * queue up before the server process can actually accept them. If your server may take a lot
     * of connections in a very short interval, you'll want to set this higher to avoid rejecting
     * some of the connections. Setting this to 0 will apply an implementation-specific default.
     * </b>
     * The default value is 1024.
     * </b>
     * Actual behavior of the socket backlog is dependent on OS and JDK implementation, and it may
     * even be ignored on some systems. See JDK docs
     * <a href="http://docs.oracle.com/javase/7/docs/api/java/net/ServerSocket.html#ServerSocket(int, int)">here</a>
     * for details.
     *
     * @param acceptBacklog
     * @return
     */
    @Config("thrift.accept-backlog")
    public ThriftServerConfig setAcceptBacklog(int acceptBacklog)
    {
        this.acceptBacklog = acceptBacklog;
        return this;
    }

    @Min(0)
    public int getAcceptBacklog()
    {
        return acceptBacklog;
    }

    public int getAcceptorThreadCount()
    {
        return acceptorThreadCount;
    }

    @Config("thrift.acceptor-threads.count")
    public ThriftServerConfig setAcceptorThreadCount(int acceptorThreadCount)
    {
        this.acceptorThreadCount = acceptorThreadCount;
        return this;
    }

    public int getIoThreadCount()
    {
        return ioThreadCount;
    }

    @Config("thrift.io-threads.count")
    public ThriftServerConfig setIoThreadCount(int ioThreadCount)
    {
        this.ioThreadCount = ioThreadCount;
        return this;
    }

    public Duration getIdleConnectionTimeout()
    {
        return this.idleConnectionTimeout;
    }

    /**
     * Sets a timeout period between receiving requests from a client connection. If the timeout
     * is exceeded (no complete requests have arrived from the client within the timeout), the
     * server will disconnect the idle client.
     *
     * The default is 60s.
     *
     * @param idleConnectionTimeout The timeout
     * @return This {@link ThriftServerConfig} instance
     */
    @Config("thrift.idle-connection-timeout")
    public ThriftServerConfig setIdleConnectionTimeout(Duration idleConnectionTimeout)
    {
        this.idleConnectionTimeout = idleConnectionTimeout;
        return this;
    }

    @Min(0)
    public int getConnectionLimit()
    {
        return this.connectionLimit;
    }

    /**
     * Sets an upper bound on the number of concurrent connections the server will accept.
     *
     * The default is not to limit the number of connections.
     *
     * @param connectionLimit The maximum number of concurrent connections
     * @return This {@link ThriftServerConfig} instance
     */
    @Config("thrift.connection-limit")
    public ThriftServerConfig setConnectionLimit(int connectionLimit)
    {
        this.connectionLimit = connectionLimit;
        return this;
    }

    @Min(1)
    public int getWorkerThreads()
    {
        return workerThreads.or(DEFAULT_WORKER_THREAD_COUNT);
    }

    /**
     * Sets the number of worker threads that will be created for processing thrift requests after
     * they have arrived. Any value passed here will be ignored if
     * {@link ThriftServerConfig#setWorkerExecutor(java.util.concurrent.ExecutorService)} is called.
     *
     * The default value is 200.
     *
     * @param workerThreads Number of worker threads to use
     * @return This {@link ThriftServerConfig} instance
     */
    @Config("thrift.threads.max")
    public ThriftServerConfig setWorkerThreads(int workerThreads)
    {
        this.workerThreads = Optional.of(workerThreads);
        return this;
    }

    public String getWorkerExecutorKey()
    {
        return workerExecutorKey.orNull();
    }

    /**
     * Sets the key for locating the an {@link java.util.concurrent.ExecutorService} from the
     * mapped executors installed by Guice modules.
     *
     * If you are not configuring your application usingGuice, it will probably be simpler to just
     * call {@link this#setWorkerExecutor(java.util.concurrent.ExecutorService)} instead.
     *
     * Use of this method on a {@link com.facebook.swift.service.ThriftServerConfig} instance is
     * incompatible with use of {@link this#setWorkerExecutor(java.util.concurrent.ExecutorService)}
     * or {@link this#setWorkerThreads(int)}
     */
    @Config("thrift.worker-executor-key")
    public ThriftServerConfig setWorkerExecutorKey(String workerExecutorKey)
    {
        this.workerExecutorKey = Optional.fromNullable(workerExecutorKey);
        return this;
    }

    /**
     * <p>Builds the {@link java.util.concurrent.ExecutorService} used for running Thrift server methods.</p>
     *
     * <p>The details of the {@link java.util.concurrent.ExecutorService} that gets built can be tweaked
     * by calling any of the following (though only <b>one</b> of these should actually be called):</p>
     *
     * <ul>
     *     <li>{@link this#setWorkerThreads(int)}</li>
     *     <li>{@link this#setWorkerExecutor(java.util.concurrent.ExecutorService)}</li>
     *     <li>{@link this#setWorkerExecutorKey(String)}</li>
     * </ul>
     *
     * <p>The default behavior if none of the above were called is to synthesize a fixed-size
     * {@link java.util.concurrent.ThreadPoolExecutor} using {@link this#DEFAULT_WORKER_THREAD_COUNT}
     * threads.</p>
     */
    public ExecutorService getOrBuildWorkerExecutor(Map<String, ExecutorService> boundWorkerExecutors)
    {
        if (workerExecutorKey.isPresent()) {
            checkState(!workerExecutor.isPresent() && !workerThreads.isPresent());

            String key = workerExecutorKey.get();
            checkArgument(boundWorkerExecutors.containsKey(key),
                          "No ExecutorService was bound to key '" + key + "'");
            ExecutorService executor = boundWorkerExecutors.get(key);
            checkNotNull(executor, "WorkerExecutorKey maps to null");
            return executor;
        }

        return workerExecutor.or(makeDefaultWorkerExecutor());
    }

    /**
     * Sets the executor that will be used to process thrift requests after they arrive. Setting
     * this will override any call to {@link ThriftServerConfig#setWorkerThreads(int)}.
     *
     * Use of this method on a {@link com.facebook.swift.service.ThriftServerConfig} instance is
     * incompatible with use of {@link this#setWorkerExecutorKey(String)} or
     * {@link this#setWorkerThreads(int)}
     *
     * @param workerExecutor The worker executor
     * @return This {@link ThriftServerConfig} instance
     */
    public ThriftServerConfig setWorkerExecutor(ExecutorService workerExecutor)
    {
        this.workerExecutor = Optional.of(workerExecutor);
        return this;
    }

    private ExecutorService makeDefaultWorkerExecutor()
    {
        return newFixedThreadPool(getWorkerThreads(), new ThreadFactoryBuilder().setNameFormat("thrift-worker-%s").build());
    }

    /**
     * Sets the name of the transport (frame codec) that this server will handle. The available
     * options by default are 'unframed', 'buffered', and 'framed'. Additional modules may install
     * other options. Server startup will fail if you specify an unavailable transport here.
     *
     * @param transportName The name of the transport
     * @return This {@link ThriftServerConfig} instance
     */
    @Config("thrift.transport")
    public ThriftServerConfig setTransportName(String transportName)
    {
        this.transportName = transportName;
        return this;
    }

    @NotNull
    public String getTransportName()
    {
        return transportName;
    }

    /**
     * Sets the name of the protocol that this server will speak. The available options by default
     * are 'binary' and 'compact'. Additional modules may install other options. Server startup will
     * fail if you specify an unavailable protocol here.
     *
     * @param protocolName The name of the protocol
     * @return This {@link ThriftServerConfig} instance
     */
    @Config("thrift.protocol")
    public ThriftServerConfig setProtocolName(String protocolName)
    {
        this.protocolName = protocolName;
        return this;
    }

    @NotNull
    public String getProtocolName()
    {
        return protocolName;
    }
}
