# Swift Service

Swift Service is a simple library annotating services to be exported with
Thrift.

# Clients and Servers

In Swift, a Thrift client is simply a Java interface annotated with `@ThriftService`, and a
Thrift server is simply a Java class annotated with `@ThriftService`.  All of the
annotations documented below apply to both clients and servers.  In addition to annotating
the class or interface directly, Swift supports annotations on a super classes or
interfaces.  This means that you can create a server by implementing the client interface
without having to annotate twice.  For example the scribe server below implements the
scribe interface:

    @ThriftService
    public interface Scribe
    {
        @ThriftMethod
        ResultCode log(List<LogEntry> messages);
    }

    public class SwiftScribe implements Scribe
    {
        private final List<LogEntry> messages = new ArrayList<>();

        public List<LogEntry> getMessages()
        {
            return messages;
        }

        @Override
        public ResultCode log(List<LogEntry> messages)
        {
            this.messages.addAll(messages);
            return ResultCode.OK;
        }
    }

# Method

Swift will export all methods annotated with `@ThiftMethod`.  Swift will assume
that the Java method and the Thrift method have the same name, so if they
don't, just add a value to the annotation like this
`@ThriftMethod("myMethodName")`.

    @ThriftService
    public interface Scribe
    {
        @ThriftMethod("Log")
        ResultCode process(List<LogEntry> messages);
    }

# Parameters

In Thrift, method parameters are encoded as a Thrift struct, so each parameter
must have a name and an id.  Swift assumes the parameters are numbered starting
with one, and assuming you have compiled with debug symbols, the Thrift
parameter names match the Java parameter names.  If you want to use a different
id or name, simply annotate the parameter as follows:

    @ThriftService
    public interface Scribe
    {
        @ThriftMethod
        ResultCode log(@ThriftField(value = 3, name = "mesg") List<LogEntry> messages);
    }

# Exceptions

As with method parameters, Thrift encodes the response as a struct with field
zero being a standard return and exceptions be stored in higher number fields.
If the Java method throws only one exception annotated with @ThriftStruct,
Swift will assume the result struct field id is `1`.  Otherwise you will need to
add the extremely verbose `@ThriftException` annotations as follows:

    @ThriftMethod(exception = {
          @ThriftException(type = MyException.class, id = 1),
          @ThriftException(type = MyOther.class, id = 2)
    })
    void doSomething() throws MyException, MyOther
    {
    }

# Thrift Client

## Client Manager

Swift client implementations can be created using the `ThriftClientManager`.  The
following code creates a Scribe client:

    ThriftClientManager clientManager = new ThriftClientManager();
    FramedClientConnector connector = new FramedClientConnector(fromParts("localhost", port));
    Scribe scribe = clientManager.createClient(connector, Scribe.class).get();

A `ClientManager` owns an executor, which typically represents a pool of threads, so it is
a heavyweight object. Generally, a single `ClientManager` can (and should) be used to create
all the clients you will need.

## Client Connectors

In the example above, a `FramedClientConnector` is used to create a client that uses a
framed transport (each message is preceded by a 4-byte frame size). Other types of
connectors exist to connect to servers that use other transports
(e.g. `HttpClientConnector`, `UnframedClientConnector`).

## Asynchronous client calls

If you want to make asynchronous thrift requests, simple change the return type of the
corresponding method to a ListenableFuture, as shown here:

    @ThriftMethod()
    ListenableFuture<List<Integer>> getValues();

and the future will be set when the server returns a value. When adding listeners that do
non-trivial work when the future is set, keep in mind that if you do not provide an
executor for listeners to run on, they will run on the NIO threads, and therefore can
potentially block other clients.

## Client Cleanup

Each client owns a connection that should be closed when the client is no longer in use.
Swift automatically implements a `close()` method to every client to allow for cleanup of
the resources.  To access this method, you will need to explicitly declare it in your
interface, or extend from `AutoCloseable`.  If your client interface extends from
`AutoCloseable`, you can use Java 7 resource management to clean up the socket.

    @ThriftService
    public interface Scribe extends AutoCloseable
    {
        @ThriftMethod
        ResultCode log(List<LogEntry> messages);
    }

    ThriftClientManager clientManager = new ThriftClientManager();
    FramedClientConnector connector = new FramedClientConnector(fromParts("localhost", port));
    try (Scribe scribe = clientManager.createClient(connector, Scribe.class).get()) {
        return scribe.log(entries);
    }

You should also `close()` the `ThriftClientManager` when it is no longer useful (e.g. when
your service is shutting down).

# Thrift Server

Swift services can be integrated into existing Thrift servers with the
`ThriftServiceProcessor` class, which implements the core Thrift service
interface `TProcessor`.  The following code creates a Scribe server `TProcessor`:

    SwiftScribe scribeService = new SwiftScribe();
    TProcessor processor = new ThriftServiceProcessor(new ThriftCodecManager(), scribeService);

Additionally, Swift supports mix in style servers where multiple Thrift services can be
exported on the same port without having to use Java class inheritance.  The only
restriction is that the services must have non-overlaping method names.  For example, to
create a `TProcessor` for Scribe and some StatsService, you would write:

    new ThriftServiceProcessor(new ThriftCodecManager(),
            scribeService,
            statsService);

## Nifty Integration

Swift includes and integrates with Nifty (a Netty NIO-based thrift server)

    ThriftServer server = new ThriftServer(processor, new ThriftServerConfig().setPort(8899));
    server.start();

For testing, you can use the much simpler try with resources style on a random port:

    ThriftServiceProcessor processor = new ThriftServiceProcessor(new ThriftCodecManager(), new SwiftScribe());
    try (
            ThriftServer server = new ThriftServer(processor).start();
            ThriftClientManager clientManager = new ThriftClientManager();
            FramedClientConnector connector = new FramedClientConnector(fromParts("localhost", server.getPort()));
            Scribe scribeClient = clientManager.createClient(connector, Scribe.class).get()
    ) {
        scribeClient.log(entries);
    }

# Guice Support

Swift includes optional support for binding clients and servers into Guice.

## Binding Client in Guice

To bind a client, add the `ThriftClientModule` and the `ThriftCodecModule` to the injector
and bind the clients with the fluent `ThriftClientBinder` as follows:

    Injector injector = Guice.createInjector(Stage.PRODUCTION,
            new ConfigurationModule(new ConfigurationFactory(ImmutableMap.<String, String>of())),
            new ThriftCodecModule(),
            new ThriftClientModule(),
            new Module()
            {
                @Override
                public void configure(Binder binder)
                {
                    thriftClientBinder(binder).bindThriftClient(Scribe.class);
                }
            });

Then, simply add the `ThriftClient` type to any `@Inject` annotated field or method.  Like this:

    public MyClass(ThriftClient<Scribe> scribeClient)
    {
        FramedClientConnector connector = new FramedClientConnector(fromParts("localhost", 8899));
        try (Scribe scribe = scribeClient.open(connector).get()) {
            scribeClient.log(entries);
        }
    }

## Exporting a Service in Guice

Exporting a service is simmilar to binding a client.  Add the `ThriftServerModule` and the
`ThriftCodecModule` to the injector and exporting the services with the fluent
`ThriftServiceExporter` as follows:

    Injector injector = Guice.createInjector(Stage.PRODUCTION,
            new ConfigurationModule(new ConfigurationFactory(ImmutableMap.<String, String>of())),
            new ThriftCodecModule(),
            new ThriftServerModule(),
            new Module()
            {
                @Override
                public void configure(Binder binder)
                {
                    // bind scribe service implementation
                    binder.bind(SwiftScribe.class).in(Scopes.SINGLETON);

                    // export scribe service implementation
                    thriftServerBinder(binder).exportThriftService(SwiftScribe.class);
                }
            });

Then simply, start the `ThriftServer` with:

    injector.getInstance(ThriftServer.class).start()

You can export as many services as you like as long as the services have unique method
names.  If you have overlaping method names, you will need to run two servers (Thrift does
not support method namspacing).
