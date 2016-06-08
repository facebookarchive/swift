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

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.airlift.configuration.Config;
import io.airlift.units.DataSize;
import io.airlift.units.Duration;
import io.airlift.units.MaxDataSize;
import io.airlift.units.MinDataSize;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.airlift.units.DataSize.Unit.MEGABYTE;

public class ThriftServerConfig
{
    private static final int DEFAULT_BOSS_THREAD_COUNT = 1;
    private static final int DEFAULT_IO_WORKER_THREAD_COUNT = 2 * Runtime.getRuntime().availableProcessors();
    private static final int DEFAULT_WORKER_THREAD_COUNT = 200;
    private static final int DEFAULT_PER_CONNECTION_QUEUED_RESPONSE_LIMIT = 16;

    private String bindAddress = "localhost";
    private int port;
    private int acceptBacklog = 1024;
    private int connectionLimit;
    private int maxQueuedResponsesPerConnection = DEFAULT_PER_CONNECTION_QUEUED_RESPONSE_LIMIT;
    private int acceptorThreadCount = DEFAULT_BOSS_THREAD_COUNT;
    private int ioThreadCount = DEFAULT_IO_WORKER_THREAD_COUNT;
    private int trafficClass = 0;
    private Duration idleConnectionTimeout = Duration.valueOf("60s");
    private Duration taskExpirationTimeout = Duration.valueOf("5s");
    private Duration queueTimeout = null;
    private Optional<Integer> workerThreads = Optional.absent();
    private Optional<Integer> maxQueuedRequests = Optional.absent();
    private Optional<ExecutorService> workerExecutor = Optional.absent();
    private Optional<String> workerExecutorKey = Optional.absent();
    private String transportName = "framed";
    private String protocolName = "binary";
    /**
     * The default maximum allowable size for a single incoming thrift request or outgoing thrift
     * response. A server can configure the actual maximum to be much higher (up to 0x3FFFFFFF or
     * almost 1 GB). The default max could also be safely bumped up, but 64MB is chosen simply
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

    @MinDataSize("0B")
    // 0x3FFFFFFF bytes
    @MaxDataSize("1073741823B")
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
        checkArgument(maxFrameSize.toBytes() <= 0x3FFFFFFF);
        this.maxFrameSize = maxFrameSize;
        return this;
    }

    /**
     * <p>Sets the number of pending connections that the {@link java.net.ServerSocket} will
     * queue up before the server process can actually accept them. If your server may take a lot
     * of connections in a very short interval, you'll want to set this higher to avoid rejecting
     * some of the connections. Setting this to 0 will apply an implementation-specific default.</p>
     *
     * <p>The default value is 1024.</p>
     *
     * <p>Actual behavior of the socket backlog is dependent on OS and JDK implementation, and it may
     * even be ignored on some systems. See JDK docs
     * <a href="http://docs.oracle.com/javase/7/docs/api/java/net/ServerSocket.html#ServerSocket%28int%2C%20int%29" target="_top">here</a>
     * for details.</p>
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

    public int getTrafficClass()
    {
        return trafficClass;
    }

    @Config("thrift.traffic-class")
    public ThriftServerConfig setTrafficClass(int trafficClass)
    {
        this.trafficClass = trafficClass;
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

    public Duration getQueueTimeout()
    {
        return queueTimeout;
    }

    /**
     * Sets a timeout period between receiving a request and the pulling the request off the queue.
     * If the timeout expires before the request reaches the front of the queue and begins
     * processing, the server will discard the request instead of processing it.
     *
     * @param queueTimeout The timeout
     * @return This {@link ThriftServerConfig} instance
     */
    @Config("thrift.queue-timeout")
    public ThriftServerConfig setQueueTimeout(Duration queueTimeout)
    {
        this.queueTimeout = queueTimeout;
        return this;
    }

    public Duration getTaskExpirationTimeout()
    {
        return taskExpirationTimeout;
    }

    /**
     * Sets a timeout period between receiving a request and the completion of that request. If
     * the timeout expires before the request reaches the front of the queue and begins processing,
     * the server will discard the request instead of processing it. If the timeout expires after
     * the request has started processing, the server will send an error immediately, and discard
     * the result of request handling.
     *
     * @param taskExpirationTimeout The timeout
     * @return This {@link ThriftServerConfig} instance
     */
    @Config("thrift.task-expiration-timeout")
    public ThriftServerConfig setTaskExpirationTimeout(Duration taskExpirationTimeout)
    {
        this.taskExpirationTimeout = taskExpirationTimeout;
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
     * Sets the key for locating an {@link java.util.concurrent.ExecutorService} from the
     * mapped executors installed by Guice modules.
     *
     * If you are not configuring your application using Guice, it will probably be simpler to just
     * call
     * {@link com.facebook.swift.service.ThriftServerConfig#setWorkerExecutor(java.util.concurrent.ExecutorService)}
     * instead.
     *
     * Use of this method on a {@link com.facebook.swift.service.ThriftServerConfig} instance is
     * incompatible with use of {@link com.facebook.swift.service.ThriftServerConfig#setWorkerExecutor(java.util.concurrent.ExecutorService)}
     * or {@link com.facebook.swift.service.ThriftServerConfig#setWorkerThreads(int)}
     */
    @Config("thrift.worker-executor-key")
    public ThriftServerConfig setWorkerExecutorKey(String workerExecutorKey)
    {
        this.workerExecutorKey = Optional.fromNullable(workerExecutorKey);
        return this;
    }

    public Integer getMaxQueuedRequests()
    {
        return maxQueuedRequests.orNull();
    }

    /**
     * Sets the maximum number of received requests that will wait in the queue to be executed.
     *
     * After this many requests are waiting, the worker queue will start rejecting requests, which
     * will cause the server to fail those requests.
     */
    @Config("thrift.max-queued-requests")
    public ThriftServerConfig setMaxQueuedRequests(Integer maxQueuedRequests)
    {
        this.maxQueuedRequests = Optional.fromNullable(maxQueuedRequests);
        return this;
    }

    public int getMaxQueuedResponsesPerConnection()
    {
        return maxQueuedResponsesPerConnection;
    }

    /**
     * Sets the maximum number of responses that may accumulate per connection before the connection
     * starts blocking reads (to avoid building up limitless queued responses).
     *
     * This limit applies whenever either the client doesn't support receiving out-of-order
     * responses.
     */
    @Config("thrift.max-queued-responses-per-connection")
    public ThriftServerConfig setMaxQueuedResponsesPerConnection(int maxQueuedResponsesPerConnection)
    {
        this.maxQueuedResponsesPerConnection = maxQueuedResponsesPerConnection;
        return this;
    }

    /**
     * <p>Builds the {@link java.util.concurrent.ExecutorService} used for running Thrift server methods.</p>
     *
     * <p>The details of the {@link java.util.concurrent.ExecutorService} that gets built can be tweaked
     * by calling any of the following (though only <b>one</b> of these should actually be called):</p>
     *
     * <ul>
     *     <li>{@link com.facebook.swift.service.ThriftServerConfig#setWorkerThreads}</li>
     *     <li>{@link com.facebook.swift.service.ThriftServerConfig#setWorkerExecutor}</li>
     *     <li>{@link com.facebook.swift.service.ThriftServerConfig#setWorkerExecutorKey}</li>
     * </ul>
     *
     * <p>The default behavior if none of the above were called is to synthesize a fixed-size
     * {@link java.util.concurrent.ThreadPoolExecutor} using
     * {@link com.facebook.swift.service.ThriftServerConfig#DEFAULT_WORKER_THREAD_COUNT}
     * threads.</p>
     */
    public ExecutorService getOrBuildWorkerExecutor(Map<String, ExecutorService> boundWorkerExecutors)
    {
        if (workerExecutorKey.isPresent()) {
            checkState(!workerExecutor.isPresent(),
                       "Worker executor key should not be set along with a specific worker executor instance");
            checkState(!workerThreads.isPresent(),
                       "Worker executor key should not be set along with a number of worker threads");
            checkState(!maxQueuedRequests.isPresent(),
                       "When using a custom executor, handling maximum queued requests must be done manually");

            String key = workerExecutorKey.get();
            checkArgument(boundWorkerExecutors.containsKey(key),
                          "No ExecutorService was bound to key '" + key + "'");
            ExecutorService executor = boundWorkerExecutors.get(key);
            checkNotNull(executor, "WorkerExecutorKey maps to null");
            return executor;
        }
        else if (workerExecutor.isPresent()) {
            checkState(!workerThreads.isPresent(),
                       "Worker executor should not be set along with number of worker threads");
            checkState(!maxQueuedRequests.isPresent(),
                       "When using a custom executor, handling maximum queued requests must be done manually");

            return workerExecutor.get();
        }
        else {
            return makeDefaultWorkerExecutor();
        }
    }

    /**
     * Sets the executor that will be used to process thrift requests after they arrive. Setting
     * this will override any call to
     * {@link com.facebook.swift.service.ThriftServerConfig#setWorkerThreads(int)}.
     *
     * Use of this method on a {@link com.facebook.swift.service.ThriftServerConfig} instance is
     * incompatible with use of
     * {@link com.facebook.swift.service.ThriftServerConfig#setWorkerExecutorKey(String)} or
     * {@link com.facebook.swift.service.ThriftServerConfig#setWorkerThreads(int)}
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
        BlockingQueue<Runnable> queue;

        if (maxQueuedRequests.isPresent()) {
            // Create a limited-capacity executor that will throw RejectedExecutionException when full.
            // NiftyDispatcher will handle RejectedExecutionException by sending a TApplicationException.
            queue = new LinkedBlockingQueue<>(maxQueuedRequests.get());
        }
        else {
            queue = new LinkedBlockingQueue<>();
        }

        return new ThreadPoolExecutor(getWorkerThreads(),
                                      getWorkerThreads(),
                                      0L,
                                      TimeUnit.MILLISECONDS,
                                      queue,
                                      new ThreadFactoryBuilder().setNameFormat("thrift-worker-%s").build(),
                                      new ThreadPoolExecutor.AbortPolicy());
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
