# Nifty

Nifty is an implementation of [Thrift](http://thrift.apache.org/) clients and servers on [Netty](https://netty.io/).

It is also the implementation used by [Swift](https://github.com/facebook/swift).

# Examples

To create a basic Thrift server using Nifty, use the [Thrift 0.9.0](https://dist.apache.org/repos/dist/release/thrift/0.9.0/thrift-0.9.0.tar.gz) code generator to generate Java stub code, write a Handler for your service interface, and pass it to Nifty like this:

    public void startServer() {
        // Create the handler
        MyService.Iface serviceInterface = new MyServiceHandler();

        // Create the processor
        TProcessor processor = new MyService.Processor<>(serviceInterface);

        // Build the server definition
        ThriftServerDef serverDef = new ThriftServerDefBuilder().withProcessor(processor)
                                                                .build();

        // Create the server transport
        final NettyServerTransport server = new NettyServerTransport(serverDef,
                                                                     new NettyConfigBuilder(),
                                                                     new DefaultChannelGroup(),
                                                                     new HashedWheelTimer());

        // Create netty boss and executor thread pools
        ExecutorService bossExecutor = Executors.newCachedThreadPool();
        ExecutorService workerExecutor = Executors.newCachedThreadPool();

        // Start the server
        server.start(bossExecutor, workerExecutor);

        // Arrange to stop the server at shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    server.stop();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

Or the same thing using guice:

    public void startGuiceServer() {
        final NiftyBootstrap bootstrap = Guice.createInjector(
            Stage.PRODUCTION,
            new NiftyModule() {
                @Override
                protected void configureNifty() {
                    // Create the handler
                    MyService.Iface serviceInterface = new MyServiceHandler();

                    // Create the processor
                    TProcessor processor = new MyService.Processor<>(serviceInterface);

                    // Build the server definition
                    ThriftServerDef serverDef = new ThriftServerDefBuilder().withProcessor(processor)
                                                                            .build();

                    // Bind the definition
                    bind().toInstance(serverDef);
                }
            }).getInstance(NiftyBootstrap.class);

        // Start the server
        bootstrap.start();

        // Arrange to stop the server at shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                bootstrap.stop();
            }
        });
    }
