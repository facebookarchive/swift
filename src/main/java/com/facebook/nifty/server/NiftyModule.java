package com.facebook.nifty.server;

import com.google.inject.AbstractModule;
import com.proofpoint.configuration.ConfigurationModule;

/**
 * Created with IntelliJ IDEA.
 * User: jaxlaw
 * Date: 4/19/12
 * Time: 5:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class NiftyModule extends AbstractModule {
    @Override
    protected void configure() {
        ConfigurationModule.bindConfig(binder()).to(NiftyConfig.class);
    }
}
