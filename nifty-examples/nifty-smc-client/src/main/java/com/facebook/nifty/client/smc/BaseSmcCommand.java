package com.facebook.nifty.client.smc;

import com.facebook.nifty.client.NiftyClient;
import com.facebook.nifty.client.TNiftyClientTransport;
import com.facebook.services.Constants;
import com.facebook.services.ServiceException;
import com.facebook.services.ServiceManager;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransportException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.iq80.cli.Option;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;

public abstract class BaseSmcCommand implements Runnable {

  static final ObjectMapper mapper = new ObjectMapper();

  @Option(
    name = {"-h", "--smc-host"},
    description = "smc host"
  )
  public String smcHost = "localhost";

  @Option(
    name = {"-p", "--smc-port"},
    description = "smc port"
  )
  public int smcPort = Constants.kSmcProxyDefaultPort;

  protected void withSmcClient(SmcClientCallback callback) {
    TNiftyClientTransport proto = null;
    NiftyClient niftyClient = new NiftyClient();
    try {
      proto = niftyClient.connectSync(new InetSocketAddress(smcHost, smcPort));
    } catch (TTransportException e) {
      e.printStackTrace();
      return;
    } catch (InterruptedException e) {
      e.printStackTrace();
      return;
    }
    try {
      callback.withSmcClient(new ServiceManager.Client(new TBinaryProtocol(proto)));
    } catch (TException e) {
      e.printStackTrace();
    } catch (ServiceException e) {
      e.printStackTrace();
    } finally {
      if (proto != null) {
        proto.close();
      }
      niftyClient.close();
    }
  }

  public static String toPrettyJson(Object object) {
    StringWriter w = new StringWriter();
    try {
      JsonGenerator jgen = mapper.getJsonFactory().createJsonGenerator(w);
      jgen.useDefaultPrettyPrinter();
      jgen.writeObject(object);
      jgen.flush();
      jgen.close();
    } catch (IOException e) {
    }
    return w.toString();
  }


  interface SmcClientCallback {
    public void withSmcClient(ServiceManager.Client client) throws TException, ServiceException;
  }
}
