package com.facebook.nifty.server;

import com.facebook.fb303.FacebookBase;
import com.facebook.fb303.FacebookService;
import com.facebook.fb303.fb_status;
import com.facebook.nifty.core.NettyConfigBuilder;
import com.facebook.nifty.core.NettyServerTransport;
import com.facebook.nifty.core.ThriftServerDefBuilder;
import junit.framework.Assert;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.THeaderTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.Executors;

public class TestNettyServerWithHeaderTransport {

  public static final String VERSION = "1.0";
  private NettyServerTransport transport;
  private int port;

  @BeforeTest(alwaysRun = true)
  public void setup() {

    try {
      ServerSocket s = new ServerSocket();
      s.bind(new InetSocketAddress(0));
      port = s.getLocalPort();
      s.close();
    } catch (IOException e) {
      port = 8080;
    }

    transport = new NettyServerTransport(
      new ThriftServerDefBuilder()
        .listen(port)
        .limitFrameSizeTo(100000)
        .withProcessor(
          new FacebookService.Processor(
            new FacebookBase("test") {
              @Override
              public String getVersion() throws TException {
                return VERSION;
              }

              @Override
              public int getStatus() {
                return fb_status.ALIVE;
              }
            }
          )
        )
        .usingHeaderTransport()
        .build(),
      new NettyConfigBuilder(), new DefaultChannelGroup()
    );
    transport.start(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
  }

  @AfterTest(alwaysRun = true)
  public void teardown() throws InterruptedException {
    if (transport != null) {
      transport.stop();
    }
  }

  @Test(groups = "fast")
  public void testMethodCalls() throws Exception {
    FacebookService.Client client = makeHeaderClient();

    String version = client.getVersion();
    int status = client.getStatus();

    Assert.assertEquals(version, VERSION);
    Assert.assertEquals(status, fb_status.ALIVE);

  }

  @Test(groups = "fast")
  public void testMethodCallsWithNormalClient() throws Exception {
    FacebookService.Client client = makeNormalClient();

    String version = client.getVersion();
    int status = client.getStatus();

    Assert.assertEquals(version, VERSION);
    Assert.assertEquals(status, fb_status.ALIVE);

  }


  private FacebookService.Client makeHeaderClient() throws TTransportException {
    TSocket socket = new TSocket("localhost", port);
    socket.open();
    TProtocol tp = new TBinaryProtocol(new THeaderTransport(socket));
    return new FacebookService.Client(tp);
  }

  private FacebookService.Client makeNormalClient() throws TTransportException {
    TSocket socket = new TSocket("localhost", port);
    socket.open();
    TBinaryProtocol tp = new TBinaryProtocol(new TFramedTransport(socket));
    return new FacebookService.Client(tp);
  }


}
