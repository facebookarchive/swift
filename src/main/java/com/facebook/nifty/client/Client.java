package com.facebook.nifty.client;

import com.facebook.fb303.FacebookService;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;

/**
 * Created with IntelliJ IDEA.
 * User: jaxlaw
 * Date: 4/19/12
 * Time: 4:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class Client {
    public static void main(String[] args) throws TException {
        TSocket socket = new TSocket("localhost", 8080);
        try {
            socket.open();
            TBinaryProtocol tp = new TBinaryProtocol(new TFramedTransport(socket));
            FacebookService.Client client = new FacebookService.Client(tp);

            System.out.println(client.getStatus());
            System.out.println(client.getVersion());
            System.out.println(client.aliveSince());
            System.out.println(client.getCommandLine());
            System.out.println(client.getName());
            System.out.println(client.getMemoryUsage());
            client.shutdown();
        } finally {
            socket.close();
        }
    }
}
