package com.facebook.nifty.server;

import com.facebook.fb303.FacebookService;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;

public class Client {
    public static void main(String[] args) throws TException, InterruptedException {
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
        Thread.sleep(5000L);
    }
}
