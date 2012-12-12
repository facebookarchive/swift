/*
 * Copyright (C) 2012 Facebook, Inc.
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
package com.facebook.swift.service.guice;

import com.facebook.swift.service.ThriftClientManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class ThriftClientManagerProvider implements Provider<ThriftClientManager>
{
    private Integer maxFrameSize;

    @Inject(optional = true)
    public void setMaxFrameSize(@Named("thrift_client_max_frame_size") Integer maxFrameSize)
    {
        this.maxFrameSize = maxFrameSize;
    }

    @Override
    public ThriftClientManager get()
    {
        if (maxFrameSize == null) {
            return new ThriftClientManager();
        }

        return new ThriftClientManager(maxFrameSize);
    }
}
