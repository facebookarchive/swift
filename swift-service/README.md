# Swift Service

Swift Service is a simple library annotating services to be exported with
Thrift.

# Clients and Servers

In Swift a Thrift client is simply a Java interface annotated with
`@ThriftService` and a Thrift server is simply a Java class annotated with
`@ThriftService`.  All of the annotations documented below apply to both
clients and servers.  In addition to annotating the class or interface
directly, Swift supports annotations on a super classes or interfaces.  This
means that you can create a server by implementing the client interface without
having to annotate twice.  For example the scribe server below implements the
scribe interface:

    @ThriftService
    public interface Scribe{
      @ThriftMethod
      ResultCode log(List<LogEntry> messages);
    }

    public class SwiftScribe implements Scribe {
      private final List<LogEntry> messages = new ArrayList<>();
    
      public List<LogEntry> getMessages() {
        return messages;
      }
    
      @Override
      public ResultCode log(List<LogEntry> messages) {
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
    public interface Scribe {
      @ThriftMethod("Log")
      ResultCode process(List<LogEntry> messages);
    }

# Parameters

In Thrift method parameters are encoded as a Thrift struct, so each parameter
must have a name and an id.  Swift assumes the parameters are numbered starting
with one, and assuming you have compiled with debug symbols, the Thrift
parameter names match the Java parameter names.  If you want to use a different
id or name, simply annotate the parameter as follows:

    @ThriftService
    public interface Scribe {
      @ThriftMethod
      ResultCode log(@ThriftField(value = 3, name = "mesg") List<LogEntry> messages);
    }

# Exceptions

As with method parameters, Thrift encodes the response as a struct with field
zero being a standard return and exceptions be stored in higher number fields.
If the Java method throws only one exception annotated with @ThriftStruct,
Swift will assume the result struct field id is 1.  Otherwise you will need to
add the extremely verbose `@ThriftException` annotations as follows:

    @ThriftMethod(exception = {
          @ThriftException(type = MyException.class, id = 1),
          @ThriftException(type = MyOther.class, id = 2)
    })
    void doSomething() throws MyException, MyOther {
    }   

# Client Manager

Swift client implementations can be create using the `ClientManager`.  The
following code creates a Scribe client:

    ThriftClientManager clientManager = new ThriftClientManager();
    Scribe scribe = clientManager.createClient(fromParts("localhost", port), Scribe.class);

## Client Cleanup

Each client opens a socket that should be closed when the client is no longer
in use.  Swift automatically, add a `close()` method to every client to allow
for cleanup of the resources.  This mean if your client interface extend
AutoClose, you can use Java 7 resource management to clean up the socket.

    @ThriftService
    public interface Scribe extends AutoCloseable {
      @ThriftMethod
      ResultCode log(List<LogEntry> messages);
    }

    ThriftClientManager clientManager = new ThriftClientManager();
    try (Scribe scribe = clientManager.createClient(fromParts("localhost", port), Scribe.class)) {
      return scribe.log(entries);
    }

# Server TProcessor

Swift services can be integrated into existing Thrift server with the
`ThriftServiceProcessor` class which implements the core Thrift service
interface `TProcessor`.  The following code creates a Scribe server TProcessor:

    SwiftScribe scribeService = new SwiftScribe();
    TProcessor processor = new ThriftServiceProcessor(scribeService, new ThriftCodecManager());


# Todo
* Add ability to specify parameter, response and exception ids
* Verify exception handling logic
* Generate processor


