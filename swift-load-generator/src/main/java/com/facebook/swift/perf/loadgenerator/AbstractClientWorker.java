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

import java.util.concurrent.atomic.AtomicLong;

import com.google.common.net.HostAndPort;

import java.nio.ByteBuffer;
import java.util.Random;

public abstract class AbstractClientWorker implements Runnable
{
    protected final LoadGeneratorCommandLineConfig config;
    protected final HostAndPort serverHostAndPort;
    private final Random randomGenerator = new Random();
    protected AtomicLong requestsProcessed = new AtomicLong(0);
    protected AtomicLong requestsFailed = new AtomicLong(0);
    protected AtomicLong requestsPending = new AtomicLong(0);
    private final int totalWeight;

    public AbstractClientWorker(LoadGeneratorCommandLineConfig config)
    {
        this.config = config;
        this.serverHostAndPort = HostAndPort.fromParts(config.serverAddress, config.serverPort);
        this.totalWeight =
                config.weightNoop +
                config.weightOnewayNoop +
                config.weightAsyncNoop +
                config.weightAdd +
                config.weightEcho +
                config.weightSend +
                config.weightRecv +
                config.weightSendRecv +
                config.weightOnewaySend +
                config.weightOnewayThrow +
                config.weightThrowUnexpected +
                config.weightThrowError +
                config.weightSleep +
                config.weightOnewaySleep +
                config.weightBadBurn +
                config.weightBadSleep +
                config.weightOnewayBurn +
                config.weightBurn;
    }

    public long collectSuccessfulOperationCount()
    {
        return requestsProcessed.getAndSet(0);
    }

    public long collectFailedOperationCount()
    {
        return requestsFailed.getAndSet(0);
    }

    public long getOperationsPerConnection()
    {
        return config.operationsPerConnection;
    }

    public void reconnect()
    {
    }

    public abstract void shutdown();

    protected Operation nextOperation()
    {
        int random = randomGenerator.nextInt(totalWeight);
        int weightRunningTotal = 0;

        if (random < weightRunningTotal + config.weightNoop) {
            return Operation.NOOP;
        }
        weightRunningTotal += config.weightNoop;

        if (random < weightRunningTotal + config.weightOnewayNoop) {
            return Operation.ONEWAY_NOOP;
        }
        weightRunningTotal += config.weightOnewayNoop;

        if (random < weightRunningTotal + config.weightAsyncNoop) {
            return Operation.ASYNC_NOOP;
        }
        weightRunningTotal += config.weightAsyncNoop;

        if (random < weightRunningTotal + config.weightAdd) {
            return Operation.ADD;
        }
        weightRunningTotal += config.weightAdd;

        if (random < weightRunningTotal + config.weightEcho) {
            return Operation.ECHO;
        }
        weightRunningTotal += config.weightEcho;

        if (random < weightRunningTotal + config.weightSend) {
            return Operation.SEND;
        }
        weightRunningTotal += config.weightSend;

        if (random < weightRunningTotal + config.weightRecv) {
            return Operation.RECV;
        }
        weightRunningTotal += config.weightRecv;

        if (random < weightRunningTotal + config.weightSendRecv) {
            return Operation.SEND_RECV;
        }
        weightRunningTotal += config.weightSendRecv;

        if (random < weightRunningTotal + config.weightOnewaySend) {
            return Operation.ONEWAY_SEND;
        }
        weightRunningTotal += config.weightOnewaySend;

        if (random < weightRunningTotal + config.weightOnewayThrow) {
            return Operation.ONEWAY_THROW;
        }
        weightRunningTotal += config.weightOnewayThrow;

        if (random < weightRunningTotal + config.weightThrowUnexpected) {
            return Operation.THROW_UNEXPECTED;
        }
        weightRunningTotal += config.weightThrowUnexpected;

        if (random < weightRunningTotal + config.weightThrowError) {
            return Operation.THROW_ERROR;
        }
        weightRunningTotal += config.weightThrowError;

        if (random < weightRunningTotal + config.weightSleep) {
            return Operation.SLEEP;
        }
        weightRunningTotal += config.weightSleep;

        if (random < weightRunningTotal + config.weightOnewaySleep) {
            return Operation.ONEWAY_SLEEP;
        }
        weightRunningTotal += config.weightOnewaySleep;

        if (random < weightRunningTotal + config.weightBadBurn) {
            return Operation.BAD_BURN;
        }
        weightRunningTotal += config.weightBadBurn;

        if (random < weightRunningTotal + config.weightBadSleep) {
            return Operation.BAD_SLEEP;
        }
        weightRunningTotal += config.weightBadSleep;

        if (random < weightRunningTotal + config.weightOnewayBurn) {
            return Operation.ONEWAY_BURN;
        }
        weightRunningTotal += config.weightOnewayBurn;

        if (random < weightRunningTotal + config.weightBurn) {
            return Operation.BURN;
        }
        weightRunningTotal += config.weightBurn;

        throw new IllegalStateException();
    }

    protected long getNextSleepMicroseconds()
    {
        return Math.round(logNormalSample(config.sleepAverage, config.sleepSigma));
    }

    protected long getNextBurnMicroseconds()
    {
        return Math.round(logNormalSample(config.burnAverage, config.burnSigma));
    }

    protected ByteBuffer getNextSendBuffer()
    {
        return ByteBuffer.allocate(getNextSendBufferSize());
    }

    protected long getNextAddOperand()
    {
        return randomGenerator.nextInt();
    }

    protected int getNextSendBufferSize()
    {
        return (int)Math.round(logNormalSample(config.sendAverage, config.sendSigma));
    }

    protected int getNextReceiveBufferSize()
    {
        return (int)Math.round(logNormalSample(config.receiveAverage, config.receiveSigma));
    }

    protected int getNextExceptionCode()
    {
        return 1;
    }

    protected double logNormalSample(double mean, double sigma) {
        // TODO: find a good LogNormal sampler for java
        return mean;
    }
}
