package com.facebook.nifty.server;

import com.facebook.fb303.FacebookBase;
import com.facebook.fb303.FacebookService;
import com.facebook.fb303.fb_status;
import com.facebook.nifty.core.NiftyBootstrap;
import com.facebook.nifty.core.ThriftServerDef;
import com.facebook.nifty.core.ThriftServerDefBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.google.inject.multibindings.Multibinder;
import com.proofpoint.bootstrap.LifeCycleManager;
import com.proofpoint.bootstrap.LifeCycleModule;
import com.proofpoint.configuration.Config;
import com.proofpoint.configuration.ConfigurationFactory;
import com.proofpoint.configuration.ConfigurationLoader;
import com.proofpoint.configuration.ConfigurationModule;
import com.proofpoint.configuration.ConfigurationValidator;
import com.proofpoint.configuration.ValidationErrorModule;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws Exception {
        ConfigurationFactory cf = new ConfigurationFactory(new ConfigurationLoader().loadProperties());
        AbstractModule exampleModule = new AbstractModule() {
            @Override
            public void configure() {
                ConfigurationModule.bindConfig(binder()).to(ExampleConfig.class);
                binder().bind(NiftyBootstrap.class).in(Singleton.class);
                Multibinder<ThriftServerDef> mbinder = Multibinder.newSetBinder(binder(), ThriftServerDef.class);
                mbinder.addBinding().toProvider(ExampleThriftServerProvider.class).asEagerSingleton();
            }
        };
        Guice.createInjector
            (
                Stage.PRODUCTION,
                new ConfigurationModule(cf),
                new ValidationErrorModule(new ConfigurationValidator(cf, null).validate(exampleModule)),
                new LifeCycleModule(),
                exampleModule
            )
            .getInstance(LifeCycleManager.class)
            .start();
    }

    private static class ExampleThriftServerProvider implements Provider<ThriftServerDef> {

        private static final Logger log = LoggerFactory.getLogger(ExampleThriftServerProvider.class);

        private final ExampleConfig config;
        private final LifeCycleManager lifeCycleManager;

        @Inject
        private ExampleThriftServerProvider(ExampleConfig config, LifeCycleManager lifeCycleManager) {
            this.config = config;
            this.lifeCycleManager = lifeCycleManager;
        }

        @Override
        public ThriftServerDef get() {
            return new ThriftServerDefBuilder()
                .listen(config.getServerPort())
                .limitFrameSizeTo(config.getMaxFrameSize())
                .withProcessor(new FacebookService.Processor(new MyFacebookBase()))
                .using(Executors.newFixedThreadPool(5))
                .build();
        }

        private class MyFacebookBase extends FacebookBase {
            public MyFacebookBase() {
                super("nifty");
            }

            @Override
            public String getVersion() throws TException {
                return "1.0";
            }

            @Override
            public int getStatus() {
                return fb_status.ALIVE;
            }

            @Override
            public void shutdown() {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            lifeCycleManager.stop();
                        } catch (Exception e) {
                            log.error("Exception caught during shutdown :", e);
                        }
                    }
                }.start();
                super.shutdown();
            }
        }
    }

    /**
     * Created with IntelliJ IDEA.
     * User: jaxlaw
     * Date: 4/19/12
     * Time: 10:01 AM
     * To change this template use File | Settings | File Templates.
     */
    public static class ExampleConfig {
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
}
