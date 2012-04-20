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
import com.proofpoint.configuration.ConfigurationFactory;
import com.proofpoint.configuration.ConfigurationLoader;
import com.proofpoint.configuration.ConfigurationModule;
import com.proofpoint.configuration.ConfigurationValidator;
import com.proofpoint.configuration.ValidationErrorModule;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

/**
 * An example of how to create a Nifty server without plugging into config or lifecycle framework.
 */
public class Plain {
    private static final Logger log = LoggerFactory.getLogger(Plain.class);

    public static void main(String[] args) throws Exception {
        final NiftyBootstrap bootstrap = Guice.createInjector
            (
                Stage.PRODUCTION,
                new AbstractModule() {
                    @Override
                    public void configure() {
                        binder().bind(NiftyBootstrap.class).in(Singleton.class);
                        Multibinder.newSetBinder(binder(), ThriftServerDef.class)
                            .addBinding().toProvider(ThriftServerProvider.class).in(Singleton.class);
                    }
                }
            )
            .getInstance(NiftyBootstrap.class);
        bootstrap.start();
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                try {
                    bootstrap.stop();
                } catch (InterruptedException e) {
                    // ignore
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private static class ThriftServerProvider implements Provider<ThriftServerDef> {

        private final MyFacebookBase facebookBase;

        private ThriftServerProvider(MyFacebookBase facebookBase) {
            this.facebookBase = facebookBase;
        }

        @Override
        public ThriftServerDef get() {
            return new ThriftServerDefBuilder()
                .listen(8080)
                .limitFrameSizeTo(1048576)
                .withProcessor(new FacebookService.Processor(facebookBase))
                .build();
        }
    }


    private static class MyFacebookBase extends FacebookBase {
        private final NiftyBootstrap bootstrap;

        @Inject
        public MyFacebookBase(NiftyBootstrap bootstrap) {
            super("nifty-plain");
            this.bootstrap = bootstrap;
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
                        bootstrap.stop();
                    } catch (Exception e) {
                        log.error("Exception caught during shutdown :", e);
                    }
                }
            }.start();
            super.shutdown();
        }
    }
}
