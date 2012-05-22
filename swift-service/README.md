# Swift Service

Swift Service is a simple library annotating services to be exported with
Thrift.

## Service

    @ThriftService("scribe")
    public class InMemoryScribe {
      private final List<LogEntry> messages = new ArrayList<>();
    
      public List<LogEntry> getMessages() {
        return messages;
      }
    
      @ThriftMethod("Log")
      public ResultCode log(List<LogEntry> messages) {
        this.messages.addAll(messages);
        return ResultCode.OK;
      }
    }

## Use

    SwiftScribe scribeService = new SwiftScribe();
    final TProcessor processor = new ThriftServiceProcessor(
        scribeService, 
        new ThriftCodecManager()
    );

    NiftyBootstrap nifty = Guice.createInjector(
        Stage.PRODUCTION,
        new NiftyModule() {
          @Override
          protected void configureNifty() {
            bind().toInstance(
                new ThriftServerDefBuilder()
                    .listen(port)
                    .withProcessor(processor)
                    .build()
            );
          }
        }
    ).getInstance(NiftyBootstrap.class);

    nifty.start();

# Todo
* Add ability to specify parameter, response and exception ids
* Verify exception handling logic
* Generate processor


