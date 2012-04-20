package com.facebook.nifty.example;

import com.proofpoint.configuration.Config;

/**
 * Created with IntelliJ IDEA.
 * User: jaxlaw
 * Date: 4/19/12
 * Time: 10:01 AM
 * To change this template use File | Settings | File Templates.
 */
public class ExampleConfig {
    private int serverPort = 8080;
    private int maxFrameSize = 1048576;

    public int getServerPort() {
        return serverPort;
    }

    @Config("serverPort")
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public int getMaxFrameSize() {
        return maxFrameSize;
    }

    @Config("maxFrameSize")
    public void setMaxFrameSize(int maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
    }
}
