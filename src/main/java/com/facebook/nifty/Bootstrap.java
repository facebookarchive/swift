package com.facebook.nifty;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: jaxlaw
 * Date: 4/18/12
 * Time: 3:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class Bootstrap {

    private final int port;
    private final ChannelPipelineFactory pipelineFactory;

    public Bootstrap(int port, ChannelPipelineFactory pipelineFactory) {
        this.port = port;
        this.pipelineFactory = pipelineFactory;
    }

    public Bootstrap start() {
        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));
        bootstrap.setPipelineFactory(pipelineFactory);
        bootstrap.bind(new InetSocketAddress(port));
        return this;
    }

    public void join() throws InterruptedException {
        Thread.currentThread().join();
    }
}
