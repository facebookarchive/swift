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
package com.facebook.swift.perf.loadgenerator;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = " =")
public class LoadGeneratorCommandLineConfig
{
    @Parameter(names = "-help", description = "Display this message")
    public boolean displayUsage = false;

    @Parameter(names = "-server", description = "Hostname or IP address of load test server")
    public String serverAddress = "localhost";

    @Parameter(names = "-port", description = "Port number of load test server")
    public int serverPort = 1234;

    @Parameter(names = "-num_threads", description = "Number of workers to create")
    public int numThreads = 1;

    @Parameter(names = "-ops_per_conn", description = "Number of requests to send per connection")
    public int operationsPerConnection = 1000;

    @Parameter(names = "-async", description = "Use asynchronous client workers")
    public boolean asyncMode = false;

    @Parameter(names = "-async_ops", description = "Target number of pipelined asynchronous requests")
    public int targetAsyncOperationsPending = 1;

    @Parameter(names = "-connect_timeout_ms", description = "Connect timeout in milliseconds")
    public double connectTimeoutMilliseconds = 0;

    @Parameter(names = "-send_timeout_ms", description = "Send timeout in milliseconds")
    public long sendTimeoutMilliseconds = 0;

    @Parameter(names = "-recv_timeout_ms", description = "Receive timeout in milliseconds")
    public long receiveTimeoutMilliseconds = 0;
}
