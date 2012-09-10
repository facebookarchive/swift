package com.facebook.nifty.perf;

import com.facebook.nifty.core.NiftyBootstrap;
import com.facebook.nifty.core.ThriftServerDef;
import com.facebook.nifty.core.ThriftServerDefBuilder;
import com.facebook.nifty.guice.NiftyModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.proofpoint.bootstrap.LifeCycleManager;
import com.proofpoint.bootstrap.LifeCycleModule;
import com.proofpoint.configuration.Config;
import com.proofpoint.configuration.ConfigurationFactory;
import com.proofpoint.configuration.ConfigurationLoader;
import com.proofpoint.configuration.ConfigurationModule;
import com.proofpoint.configuration.ConfigurationValidator;
import com.proofpoint.configuration.ValidationErrorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

public class NiftyLoadTester {

  public static void main(String[] args) throws Exception {
    ConfigurationFactory cf = new ConfigurationFactory(new ConfigurationLoader().loadProperties());
    AbstractModule exampleModule = new AbstractModule() {
      @Override
      public void configure() {
        ConfigurationModule.bindConfig(binder()).to(LoadTesterConfig.class);
        binder().bind(NiftyBootstrap.class).in(Singleton.class);
      }
    };
    Guice.createInjector
      (
        Stage.PRODUCTION,
        new ConfigurationModule(cf),
        new ValidationErrorModule(new ConfigurationValidator(cf, null).validate(exampleModule)),
        new LifeCycleModule(),
        exampleModule,
        new NiftyModule() {
          @Override
          protected void configureNifty() {
            bind().toProvider(LoadTestServerProvider.class);
            withDefaultNettyConfig();
          }
        }
      )
      .getInstance(LifeCycleManager.class)
      .start();
  }

  private static class LoadTestServerProvider implements Provider<ThriftServerDef> {

    private static final Logger log = LoggerFactory.getLogger(
      LoadTestServerProvider.class
    );

    private final LoadTesterConfig config;
    private final LifeCycleManager lifeCycleManager;

    @Inject
    private LoadTestServerProvider(LoadTesterConfig config, LifeCycleManager lifeCycleManager) {
      this.config = config;
      this.lifeCycleManager = lifeCycleManager;
    }

    @Override
    public ThriftServerDef get() {
      ThriftServerDefBuilder builder = new ThriftServerDefBuilder()
        .listen(config.getServerPort())
        .limitFrameSizeTo(config.getMaxFrameSize())
        .limitQueuedResponsesPerConnection(config.getQueuedResponseLimit())
        .withProcessor(new LoadTest.Processor<LoadTest.Iface>(new LoadTestHandler()));

      if (config.getUseTaskQueue()) {
        builder.using(Executors.newFixedThreadPool(config.getNumTaskThreads()));
      }

      return builder.build();
    }
  }

  public static class LoadTesterConfig {
    private int serverPort = 1234;
    private int maxFrameSize = 1048576;
    private boolean useTaskQueue = false;
    private int numTaskThreads = 8;
    private int queuedResponseLimit = 500;

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

    public boolean getUseTaskQueue() {
      return useTaskQueue;
    }

    @Config("useTaskQueue")
    public void setUseTaskQueue(boolean useTaskQueue) {
      this.useTaskQueue = useTaskQueue;
    }

    public int getNumTaskThreads() {
      return numTaskThreads;
    }

    @Config("numTaskThreads")
    public void setNumTaskThreads(int numTaskThreads) {
      this.numTaskThreads = numTaskThreads;
    }

    public int getQueuedResponseLimit()
    {
      return queuedResponseLimit;
    }

    @Config("queuedResponseLimit")
    public void setQueuedResponseLimit(int queuedResponseLimit) {
      this.queuedResponseLimit = queuedResponseLimit;
    }
  }
}
