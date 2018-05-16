# Project Status: 🚨 Unmaintained 🚨

This project is archived and no longer maintained. At the time of archiving,
open issues and pull requests were closed and tagged with `2018-05-archive`.
For pre-existing users who need an open source alternative, we recommend taking
a look at [airlift/drift](https://github.com/airlift/drift).

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
