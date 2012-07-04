/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service.guice;

import com.facebook.swift.service.ThriftServer;
import com.facebook.swift.service.ThriftServerConfig;
import com.facebook.swift.service.guice.ThriftServiceExporter.ThriftServiceExport;
import com.facebook.swift.service.guice.ThriftServiceExporter.ThriftServiceProcessorProvider;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import org.apache.thrift.TProcessor;

import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static io.airlift.configuration.ConfigurationModule.bindConfig;

public class ThriftServerModule implements Module
{
    @Override
    public void configure(Binder binder)
    {
        newSetBinder(binder, ThriftServiceExport.class).permitDuplicates();
        binder.bind(TProcessor.class).toProvider(ThriftServiceProcessorProvider.class);

        bindConfig(binder).to(ThriftServerConfig.class);
        binder.bind(ThriftServer.class).in(Scopes.SINGLETON);
    }
}
