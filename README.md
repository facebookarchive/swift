# Swift

Swift is an easy-to-use, annotation-based Java library for creating Thrift
serializable types and services.

# Swift Codec

[Swift Codec](swift-codec) is a simple library specifying how Java
objects are converted to and from Thrift.  This library is similar to JaxB
(XML) and Jackson (JSON), but for Thrift.  Swift codec supports field, method,
constructor, and builder injection.  For example:

    @ThriftStruct
    public class LogEntry
    {
        private final String category;
        private final String message;

        @ThriftConstructor
        public LogEntry(String category, String message)
        {
            this.category = category;
            this.message = message;
        }

        @ThriftField(1)
        public String getCategory()
        {
            return category;
        }

        @ThriftField(2)
        public String getMessage()
        {
            return message;
        }
    }


# Swift Service

[Swift Service](swift-service) is a simple library annotating
services to be exported with Thrift.   For example:

    @ThriftService("scribe")
    public class InMemoryScribe
    {
        private final List<LogEntry> messages = new ArrayList<>();

        public List<LogEntry> getMessages()
        {
            return messages;
        }

        @ThriftMethod("Log")
        public ResultCode log(List<LogEntry> messages)
        {
            this.messages.addAll(messages);
            return ResultCode.OK;
        }
    }

# Swift Generator

[Swift Generator](swift-generator) is a library that creates Java code usable with the Swift codec from Thrift IDL files and vice versa.

[Swift Generator CLI](swift-generator-cli) and [Swift2Thrift Generator CLI](swift2thrift-generator-cli) are command-line front-ends to this generator.

# Swift Maven plugin

[Swift Maven plugin](swift-maven-plugin) allows using the code generator from a maven build and generate source code on the fly.

# Nifty

Nifty is an implementation of [Thrift](http://thrift.apache.org/) clients and servers on [Netty](http://netty.io/).

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
                                                                     new NettyServerConfigBuilder(),
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
