/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.guice;

import com.facebook.swift.codec.InternalThriftCodec;
import com.facebook.swift.codec.ThriftCodec;
import com.facebook.swift.codec.ThriftCodecManager;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;

import static com.google.inject.multibindings.Multibinder.newSetBinder;

public class ThriftCodecModule implements Module
{
    @Override
    public void configure(Binder binder)
    {
        binder.disableCircularProxies();
        binder.requireExplicitBindings();

        binder.bind(ThriftCodecManager.class).in(Scopes.SINGLETON);
        newSetBinder(binder, new TypeLiteral<ThriftCodec<?>>() {}, InternalThriftCodec.class).permitDuplicates();
    }
}

