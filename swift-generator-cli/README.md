# Swift Code Generator Tool

The code generator tool transltes existing .thrift files into Swift interfaces.

## Installing the code generator

If you just want the latest release of the generator tool copied to your /tmp directory, use the following command line:

      mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:get -DremoteRepositories=central::default::http://repo1.maven.apache.org/maven2 -Dartifact=com.facebook.swift:swift-generator-cli:RELEASE:jar:standalone -Ddest=/tmp/

If you want a specific version, you should first determine which version of Swift you will be working with. You can look here at the [available releases](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.facebook.swift%22%20AND%20a%3A%22swift-generator-cli%22), and after you've determined which version you want, and replace RELEASE above with the version you will be using.

You can also replace '/tmp/' with the directory from which you would like to run the tool.

After downloading the tool, you should be able to run it like this:

    $ java -jar /tmp/swift-generator-cli-0.7.0-standalone.jar
    Usage: SwiftGenerator [options] Thrift IDL input files
      Options:
        -default_package
           Use this package if there is no package specified in the IDL for java
        -generate_beans
           Generate thrift types as mutable beans
           Default: false
        -generate_included_files
           Generate code for included IDL files as well as specified IDL files
           Default: false
        -out
           Output directory
           Default: /home/andrewcox/MyProject
        -override_package
           Force generation of code in a specific package
        -tweak
           Enable specific code generation tweaks
           Default: []
        -use_java_namespace
           Use 'java' namespace instead of 'java.swift' namespace
           Default: false

## Usage

Add a 'namespace java.swift' to your .thrift file. This will differentiate the package that will hold your swift interfaces from the package which will hold your standard java thrift generated code (in case you are using both in the same application).

When running the generator tool, you should start it from the root directory where all .thrift files which are included by your file can be found. Then just invoke it like this, for example:

     java -jar /tmp/swift-generator-cli-0.7.0-standalone.jar common/fb303/if/fb303.thrift

### Tweaks

Most of the options should be self-explanatory from the usage message. The exception is -tweak. This can be used to affect various parts of the interface generation. Here they are:

     public enum SwiftGeneratorTweak
     {
          ADD_THRIFT_EXCEPTION,     // Add TException to generated method signatures
          EXTEND_RUNTIME_EXCEPTION, // Make generated exceptions extend RuntimeException instead of Exception
          ADD_CLOSEABLE_INTERFACE   // Make generated Services extend Closeable and add a close() method
     }

To include two or more tweaks, just pass them separately (e.g. '-tweak ADD_THRIFT_EXCEPTION -tweak ADD_CLOSEABLE_INTERFACE')

### Example

Suppose there are two .thrift files in my project, laid out as shown:

    $ find . -name '*.thrift'
    ./scribe/if/scribe.thrift
    ./common/fb303/if/fb303.thrift

Here's how to invoke the generator:

    $ java -jar /tmp/swift-generator-cli-0.7.0-standalone.jar -generate_included_files -tweak ADD_CLOSEABLE_INTERFACE -tweak ADD_THRIFT_EXCEPTION scribe/if/scribe.thrift
    15:18:18.258 [main] INFO  c.f.swift.generator.SwiftGenerator - Parsing Thrift IDL from [file:/data/users/andrewcox/fbcode-git/scribe/if/scribe.thrift]...
    15:18:18.418 [main] INFO  c.f.swift.generator.SwiftGenerator - IDL parsing complete, writing java code...
    15:18:18.649 [main] INFO  c.f.swift.generator.SwiftGenerator - Java code generation complete.

And now it should have generated some Swift interfaces for you:

    $ find gen-swift/ -type f
    gen-swift/com/facebook/swift/scribe/SourceInfo.java
    gen-swift/com/facebook/swift/scribe/Scribe.java
    gen-swift/com/facebook/swift/scribe/LogEntry.java
    gen-swift/com/facebook/swift/scribe/MessageList.java
    gen-swift/com/facebook/swift/scribe/ResultCode.java
    gen-swift/com/facebook/swift/fb303/CountersInformation.java
    gen-swift/com/facebook/swift/fb303/FacebookService.java
    gen-swift/com/facebook/swift/fb303/FbStatus.java
