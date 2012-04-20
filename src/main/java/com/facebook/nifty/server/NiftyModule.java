package com.facebook.nifty.server;

import com.google.inject.AbstractModule;
import com.proofpoint.configuration.ConfigurationModule;

/**
 * Binds NitfyConfig with magic.
 *
 * @author jaxlaw
 */
public class NiftyModule extends AbstractModule {
    @Override
    protected void configure() {
        ConfigurationModule.bindConfig(binder()).to(NiftyConfig.class);
    }
}
