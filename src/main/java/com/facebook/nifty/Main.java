package com.facebook.nifty;

import com.google.inject.Guice;
import com.google.inject.Stage;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Guice.createInjector
                (
                        Stage.PRODUCTION,
                        new ConfigModule(),
                        new NiftyModule()
                )
                .getInstance(Bootstrap.class)
                .start()
                .join();
    }
}
