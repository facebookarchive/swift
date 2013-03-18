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

    @Parameter(names = "-transport", description = "Type of Thrift transport to use for connecting")
    public TransportType transport = TransportType.FRAMED;

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

    @Parameter(names = "-weight_noop")
    public int weightNoop = 0;

    @Parameter(names = "-weight_oneway_noop")
    public int weightOnewayNoop = 0;

    @Parameter(names = "-weight_async_noop")
    public int weightAsyncNoop = 0;

    @Parameter(names = "-weight_add")
    public int weightAdd = 0;

    @Parameter(names = "-weight_echo")
    public int weightEcho = 0;

    @Parameter(names = "-weight_send")
    public int weightSend = 0;

    @Parameter(names = "-weight_recv")
    public int weightRecv = 0;

    @Parameter(names = "-weight_send_recv")
    public int weightSendRecv = 0;

    @Parameter(names = "-weight_oneway_send")
    public int weightOnewaySend = 0;

    @Parameter(names = "-weight_oneway_throw")
    public int weightOnewayThrow = 0;

    @Parameter(names = "-weight_throw_unexpected")
    public int weightThrowUnexpected = 0;

    @Parameter(names = "-weight_throw_error")
    public int weightThrowError = 0;

    @Parameter(names = "-weight_sleep")
    public int weightSleep = 0;

    @Parameter(names = "-weight_oneway_sleep")
    public int weightOnewaySleep = 0;

    @Parameter(names = "-weight_bad_burn")
    public int weightBadBurn = 0;

    @Parameter(names = "-weight_bad_sleep")
    public int weightBadSleep = 0;

    @Parameter(names = "-weight_oneway_burn")
    public int weightOnewayBurn = 0;

    @Parameter(names = "-weight_burn")
    public int weightBurn = 0;

    @Parameter(names = "-sleep_avg", description = "Average sleep duration in microseconds")
    public double sleepAverage = 5000;

    @Parameter(names = "-sleep_sigma", description = "Sigma for sleep duration LogNormal distribution")
    public double sleepSigma = -1;

    @Parameter(names = "-burn_avg", description = "Average burn duration in microseconds")
    public double burnAverage = 5000;

    @Parameter(names = "-burn_sigma", description = "Sigma for burn duration LogNormal distribution")
    public double burnSigma = -1;

    @Parameter(names = "-send_avg", description = "Average buffer size for send operation")
    public double sendAverage = 16384;

    @Parameter(names = "-send_sigma", description = "Sigma for send buffer size LogNormal distribution")
    public double sendSigma = -1;

    @Parameter(names = "-recv_avg", description = "Average buffer size for receive operations")
    public double receiveAverage = 16384;

    @Parameter(names = "-recv_sigma", description = "Sigma for receive buffer size LogNormal distribution")
    public double receiveSigma = -1;
}
