/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service.guice;

import com.facebook.swift.service.ThriftClientManager;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;

public class ThriftClientModule implements Module
{
    @Override
    public void configure(Binder binder)
    {
        binder.bind(ThriftClientManager.class).in(Scopes.SINGLETON);
    }
}
