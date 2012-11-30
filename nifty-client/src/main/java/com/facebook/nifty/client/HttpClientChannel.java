package com.facebook.nifty.client;

import com.google.common.net.HttpHeaders;
import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.util.Timer;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Map;

@NotThreadSafe
public class HttpClientChannel extends AbstractClientChannel {
    private final Channel underlyingNettyChannel;
    private final String hostName;
    private Map<String, String> headerDictionary;
    private final String endpointUri;

    public HttpClientChannel(Channel channel,
                             Timer timer,
                             String hostName,
                             String endpointUri) {
        super(channel, timer);

        this.underlyingNettyChannel = channel;
        this.hostName = hostName;
        this.endpointUri = endpointUri;
    }

    @Override
    protected int extractSequenceId(ChannelBuffer message)
            throws TTransportException
    {
        try {
            int sequenceId;
            int stringLength;
            stringLength = message.getInt(4);
            sequenceId = message.getInt(8 + stringLength);
            return sequenceId;
        } catch (Throwable t) {
            throw new TTransportException("Could not find sequenceId in Thrift message");
        }
    }

    @Override
    public Channel getNettyChannel() {
        return underlyingNettyChannel;
    }

    @Override
    protected ChannelBuffer extractResponse(Object message)
    {
        if (!(message instanceof HttpResponse)) {
            return null;
        }

        HttpResponse httpResponse = (HttpResponse) message;
        ChannelBuffer content = httpResponse.getContent();

        if (!content.readable()) {
            return null;
        }

        return content;
    }

    @Override
    protected ChannelFuture writeRequest(ChannelBuffer request)
    {
        HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST,
                                                         endpointUri);

        httpRequest.setHeader(HttpHeaders.HOST, hostName);
        httpRequest.setHeader(HttpHeaders.CONTENT_LENGTH, request.readableBytes());
        httpRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-thrift");
        httpRequest.setHeader(HttpHeaders.USER_AGENT, "Java/Swift-HttpThriftClientChannel");

        if (headerDictionary != null) {
            for (Map.Entry<String, String> entry : headerDictionary.entrySet()) {
                httpRequest.setHeader(entry.getKey(), entry.getValue());
            }
        }

        httpRequest.setContent(request);

        return underlyingNettyChannel.write(httpRequest);
    }

    public void setHeaders(Map<String, String> headers)
    {
        this.headerDictionary = headers;
    }

    public static class Factory implements NiftyClientChannel.Factory<HttpClientChannel> {
        private final String hostName;
        private final String endpointUri;
        private final int maxMessageSize;

        public Factory(String hostName, String endpointUri, int maxMessageSize)
        {
            this.hostName = hostName;
            this.endpointUri = endpointUri;
            this.maxMessageSize = maxMessageSize;
        }

        @Override
        public HttpClientChannel newThriftClientChannel(Channel nettyChannel, Timer timer) {
            HttpClientChannel channel =
                    new HttpClientChannel(nettyChannel, timer, hostName, endpointUri);
            channel.getNettyChannel().getPipeline().addLast("thriftHandler", channel);
            return channel;
        }

        @Override
        public ChannelPipelineFactory newChannelPipelineFactory(final int maxFrameSize) {
            return new ChannelPipelineFactory()
            {
                @Override
                public ChannelPipeline getPipeline()
                        throws Exception {
                ChannelPipeline cp = Channels.pipeline();
                cp.addLast("frameEncoder", new HttpRequestEncoder());
                cp.addLast("frameDecoder", new HttpResponseDecoder());
                cp.addLast("chunkAggregator", new HttpChunkAggregator(maxMessageSize));
                return cp;
                }
            };
        }
    }
}
