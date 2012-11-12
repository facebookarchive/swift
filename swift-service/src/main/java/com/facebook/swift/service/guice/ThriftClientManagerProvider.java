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
