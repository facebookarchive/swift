# Swift

Swift is an easy-to-use, annotation-based Java library for creating Thrift serializable types and services.

# Swift Codec

[Swift Codec](swift-codec/README.md) is a simple library specifying how Java objects are convered to and from Thrift.  This library is simmilar to JaxRS (XML) and Jackson (JSON), but for Thirft.  Swift codec supports field, method, costructor, and builder injection.  For example:

    @ThriftStruct
    public class LogEntry {
      private final String category;
      private final String message;
    
      @ThriftConstructor
      public LogEntry(
          @ThriftField(name = "category") String category,
          @ThriftField(name = "message") String message
      ) {
        this.category = category;
        this.message = message;
      }
    
      @ThriftField(1)
      public String getCategory() {
        return category;
      }
    
      @ThriftField(2)
      public String getMessage() {
        return message;
      }
    }    


# Swift Service

[Swift Service](swift-service/README.md) is a simple library annotating services to be exported with Thrift.   For example:

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

