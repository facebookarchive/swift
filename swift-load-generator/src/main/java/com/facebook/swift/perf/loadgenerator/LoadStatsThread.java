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

import io.airlift.log.Logger;

import java.util.concurrent.TimeUnit;

public class LoadStatsThread extends Thread
{
    private static final Logger logger = Logger.get(LoadStatsThread.class);

    private final AbstractClientWorker[] clientWorkers;
    private volatile boolean shutdown = false;

    public LoadStatsThread(AbstractClientWorker[] clientWorkers)
    {
        this.clientWorkers = clientWorkers.clone();
    }

    @Override
    public void run()
    {
        long operations = 0;
        long failedOperations = 0;
        long startTime = System.nanoTime();
        long lastTime = startTime;

        while (!shutdown) {
            try {
                // Collect and report stats once per second
                Thread.sleep(1000);

                long deltaSuccessfulOperations = 0;
                long deltaFailedOperations = 0;
                long currentTime = System.nanoTime();

                for (AbstractClientWorker worker : clientWorkers) {
                    deltaSuccessfulOperations += worker.collectSuccessfulOperationCount();
                    deltaFailedOperations += worker.collectFailedOperationCount();
                }

                operations += deltaSuccessfulOperations;
                failedOperations += deltaFailedOperations;
                long currentQps = deltaSuccessfulOperations * TimeUnit.SECONDS.toNanos(1) / (currentTime - lastTime);
                long averageQps = operations * TimeUnit.SECONDS.toNanos(1) / (currentTime - startTime);

                logger.info(
                        "QPS: " + currentQps + " Delta completed: " + deltaSuccessfulOperations +
                        " Average QPS: " + averageQps + " Total completed: " + operations +
                        " Total failed: " + failedOperations);

                lastTime = currentTime;
            }
            catch (InterruptedException e) {
                logger.error("Stats thread was interrupted");
            }
        }
    }

    public void shutdown()
    {
        shutdown = true;
    }
}
