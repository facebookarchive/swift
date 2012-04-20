package com.facebook.nifty.server;

import com.proofpoint.configuration.Config;

/**
 * Author @jaxlaw
 * Date: 4/19/12
 * Time: 5:01 PM
 */
public class NiftyConfig {
    int numBossThreads = 0;
    int numWorkerThreads = 0;

    public int getNumBossThreads() {
        return numBossThreads;
    }

    @Config("numBossThreads")
    public void setNumBossThreads(int numBossThreads) {
        this.numBossThreads = numBossThreads;
    }

    public int getNumWorkerThreads() {
        return numWorkerThreads;
    }

    @Config("numWorkerThreads")
    public void setNumWorkerThreads(int numWorkerThreads) {
        this.numWorkerThreads = numWorkerThreads;
    }
}
