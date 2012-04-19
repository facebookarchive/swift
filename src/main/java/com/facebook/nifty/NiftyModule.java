package com.facebook.nifty;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.apache.commons.configuration.Configuration;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;

/**
 * Created with IntelliJ IDEA.
 * User: jaxlaw
 * Date: 4/18/12
 * Time: 3:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class NiftyModule extends AbstractModule {
    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    public Bootstrap getServer(Configuration conf, ChannelPipelineFactory factory) {
        return new Bootstrap(conf.getInt("server.port"), factory);
    }

    @Provides
    @Singleton
    public ChannelPipelineFactory getPipelineFactory() {
        return new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        };
    }

}
