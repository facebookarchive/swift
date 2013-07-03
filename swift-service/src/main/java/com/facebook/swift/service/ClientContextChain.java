/*
 * Copyright (C) 2013 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.swift.service;

import com.facebook.nifty.core.RequestContext;

import java.util.ArrayList;
import java.util.List;

public class ClientContextChain
{
    private final List<? extends ThriftClientEventHandler> handlers;
    private final List<Object> contexts;

    ClientContextChain(List<? extends ThriftClientEventHandler> handlers, String methodName)
    {
        this.handlers = handlers;
        this.contexts = new ArrayList<>();
        for (ThriftClientEventHandler h: this.handlers) {
            this.contexts.add(h.getContext(methodName));
        }
    }

    public void preWrite(String methodName, Object[] args)
    {
        for (int i = 0; i < handlers.size(); i++) {
            handlers.get(i).preWrite(contexts.get(i), methodName, args);
        }
    }

    public void postWrite(String methodName, Object[] args)
    {
        for (int i = 0; i < handlers.size(); i++) {
            handlers.get(i).postWrite(contexts.get(i), methodName, args);
        }
    }

    public void preRead(String methodName)
    {
        for (int i = 0; i < handlers.size(); i++) {
            handlers.get(i).preRead(contexts.get(i), methodName);
        }
    }

    public void preReadException(String methodName, Exception e)
    {
        for (int i = 0; i < handlers.size(); i++) {
            handlers.get(i).preReadException(contexts.get(i), methodName, e);
        }
    }

    public void postRead(String methodName, Object result)
    {
        for (int i = 0; i < handlers.size(); i++) {
            handlers.get(i).postRead(contexts.get(i), methodName, result);
        }
    }

    public void postReadException(String methodName, Exception e)
    {
        for (int i = 0; i < handlers.size(); i++) {
            handlers.get(i).postReadException(contexts.get(i), methodName, e);
        }
    }

    public void done(String methodName)
    {
        for (int i = 0; i < handlers.size(); i++) {
            handlers.get(i).done(contexts.get(i), methodName);
        }
    }
}
