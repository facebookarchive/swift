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
package com.facebook.swift.service.base;

import com.facebook.nifty.client.FramedClientConnector;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.ThriftEventHandler;
import com.facebook.swift.service.ThriftServer;
import com.facebook.swift.service.ThriftServerConfig;
import com.facebook.swift.service.ThriftServiceProcessor;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.thrift.transport.TTransportException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.util.concurrent.ExecutionException;

public class SuiteBase<ServiceInterface, ClientInterface> {
    private ThriftCodecManager codecManager = new ThriftCodecManager();
    private ThriftClientManager clientManager;
    private Class<? extends ClientInterface> clientClass;
    private Class<? extends ServiceInterface> handlerClass;
    private ClientInterface client;
    private ThriftServer server;
    private ServiceInterface handler;
    private ImmutableList<ThriftEventHandler> eventHandlers;
    private final ThriftServerConfig serverConfig;

    public SuiteBase(
            Class<? extends ServiceInterface> handlerClass,
            Class<? extends ClientInterface> clientClass) {
        this(handlerClass, clientClass, new ThriftServerConfig());
    }

    public SuiteBase(
            Class<? extends ServiceInterface> handlerClass,
            Class<? extends ClientInterface> clientClass,
            ThriftServerConfig serverConfig) {
        this(handlerClass, clientClass, serverConfig, ImmutableList.<ThriftEventHandler>of());
    }

    public SuiteBase(
            Class<? extends ServiceInterface> handlerClass,
            Class<? extends ClientInterface> clientClass,
            ThriftServerConfig serverConfig,
            ImmutableList<ThriftEventHandler> eventHandlers) {
        this.clientClass = clientClass;
        this.handlerClass = handlerClass;
        this.serverConfig = serverConfig;
        this.eventHandlers = eventHandlers;
    }


    @BeforeClass
    public void setupSuite() throws InstantiationException, IllegalAccessException {
    }

    @BeforeMethod
    public void setupTest() throws Exception {
        // TODO: move this to setupSuite when TestNG/surefire integration is fixed
        handler = handlerClass.newInstance();
        server = createServer(handler).start();

        clientManager = new ThriftClientManager(codecManager);
        client = createClient(clientManager).get();
    }

    @AfterMethod
    public void tearDownTest() throws Exception {
        AutoCloseable closeable = (AutoCloseable) client;
        closeable.close();
        clientManager.close();

        // TODO: move this to tearDownSuite when TestNG/surefire integration is fixed
        // (currently @AfterClass methods do not run if you are running a single test method
        // from a class containing multiple test methods)
        server.close();
    }

    @AfterClass
    public void tearDownSuite() {
    }

    private ThriftServer createServer(ServiceInterface handler)
            throws IllegalAccessException, InstantiationException {
        ThriftServiceProcessor processor = new ThriftServiceProcessor(codecManager, eventHandlers, handler);
        return new ThriftServer(processor, serverConfig);
    }

    private ListenableFuture<? extends ClientInterface> createClient(ThriftClientManager clientManager)
            throws TTransportException, InterruptedException, ExecutionException
    {
        HostAndPort address = HostAndPort.fromParts(serverConfig.getBindAddress(), server.getPort());
        return clientManager.createClient(new FramedClientConnector(address), clientClass);
    }

    protected ClientInterface getClient()
    {
        return client;
    }

    protected ServiceInterface getHandler()
    {
        return handler;
    }

    protected ThriftClientManager getClientManager()
    {
        return clientManager;
    }

}
