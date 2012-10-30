package com.facebook.swift.service.async;

import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.ThriftClient;
import com.facebook.swift.service.ThriftClientConfig;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.ThriftServer;
import com.facebook.swift.service.ThriftServerConfig;
import com.facebook.swift.service.ThriftServiceProcessor;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ListenableFuture;
import io.airlift.units.DataSize;
import io.airlift.units.Duration;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class AsyncTestBase
{
    public static final int MAX_FRAME_SIZE = 0x3fffffff;
    protected ThriftCodecManager codecManager;
    protected ThriftClientManager clientManager;

    protected <T> ListenableFuture<T> createClient(Class<T> clientClass, ThriftServer server)
            throws TTransportException, InterruptedException, ExecutionException
    {
        HostAndPort address = HostAndPort.fromParts("localhost", server.getPort());
        ThriftClientConfig config = new ThriftClientConfig().setConnectTimeout(new Duration(1, TimeUnit.SECONDS))
                                                            .setReadTimeout(new Duration(1, TimeUnit.SECONDS))
                                                            .setWriteTimeout(new Duration(1, TimeUnit.SECONDS));
        return new ThriftClient<>(clientManager, clientClass, config, "asyncTestClient").open(address);
    }

    protected ThriftServer createAsyncServer()
            throws InstantiationException, IllegalAccessException, TException
    {
        DelayedMapAsyncHandler handler = new DelayedMapAsyncHandler();
        handler.putValueSlowly(0, TimeUnit.MILLISECONDS, "testKey", "default");
        return createServerFromHandler(handler);
    }

    protected ThriftServer createServerFromHandler(Object handler)
            throws IllegalAccessException, InstantiationException
    {
        ThriftServiceProcessor processor = new ThriftServiceProcessor(codecManager, handler);
        ThriftServerConfig config = new ThriftServerConfig();
        config.setMaxFrameSize(new DataSize(MAX_FRAME_SIZE, DataSize.Unit.BYTE));

        return new ThriftServer(processor, config).start();
    }
}
