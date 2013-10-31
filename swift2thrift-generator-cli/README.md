# Swift2Thrift Code Generator

Swift2Thrift generates `.thrift` files from Swift-annotated Java files. The generated `.thrift` files can be fed to a Thrift compiler to generate client and/or server code for other languages.

## Usage

```
$ java -jar swift2thrift-generator-cli-0.9.0-SNAPSHOT-standalone.jar
Usage: Swift2ThriftGenerator [options] <Swift-class-name...>
  Options:
    -allow_multiple_packages
       Allow input classes to reside in different packages. The value of this
       flag defines the generated java.swift namespace. Note that Swift classes
       generated from the resultant Thrift file will all reside in one Java package
    -package, -default_package
       Default package for unqualified classes
       Default: <empty string>
    -map
       Map of external type or service to include file
    -namespace
       Namespace for a particular language to include
    -out
       Thrift IDL output file, defaults to stdout
    -v, -verbose
       Show verbose messages
       Default: false
```

**Important notes:**

1. You should pass (fully-qualified) class names, not `.java` (or `.class`) filenames.
2. Java ignores the classpath (set via `-classpath` or `$CLASSPATH`) if run with `-jar`. Therefore, run Swift2Thrift with `-classpath` (or `-cp` or `$CLASSPATH`) that contains your Swift classes, but not with `-jar`.

## Tips

* Use `-out` to save the generated file. Omit `-out` to see the result on `stdout` while tweaking options.
* Use `-package` to save typing. Compare `java -cp ..... -package my.long.package.name Class1 Class2 Class3` to `java -cp ..... my.long.package.name.Class1 my.long.package.name.Class2 my.long.package.name.Class3`.
* Use `-map` to have `include` lines in the generated `.thrift` file for types/services you already have `.thrift` files for. Example: `-map other.package.OtherThriftStruct path/to/file.thrift`. Can be used multiple times.
* Use `-namespace` to generate Thrift namespace declarations for other languages. Useful if you use the generated `.thrift` files to generate code (clients/servers) for other languages. Example: `-namespace cpp mynamespace`. Can be used multiple times.
* To include nested classes on the command line, use `'MyTopLevelClass$MyNestedClass'`. If running from the shell, be sure to use single quotes or the shell will eat the `$MyNestedClass`.
* Swift2Thrift checks that all input classes are in the same Java package (whether `-package` is used or not) and errors if not. You can run Swift2Thrift several times to generate multiple `.thrift` files if your input classes are in different packages.
* JavaDoc and method ordering can be preserved in the generated `.thrift` files by compiling your classes with `javac -cp swift-javadoc-0.9.0-SNAPSHOT.jar <rest-of-the-command-line>` before feeding them to Swift2Thrift. Alternatively, you can use `@ThriftDocumentation` and `@ThriftOrder` in your Java sources (but this clutters them, thus not recommended).

## Example

```bash
MY_CLASSES=swift-service/target/test-classes  # just an example
java -cp swift2thrift-generator-cli/target/swift2thrift-generator-cli-0.9.0-SNAPSHOT-standalone.jar:$MY_CLASSES \
    com.facebook.swift.generator.swift2thrift.Main -package com.facebook.swift.service.annotations \
    DerivedServiceOne -map BaseService path/to/base.thrift \
    -namespace py mycompany.thrift -namespace java com.mycompany -namespace cpp mycompany
```

Input: [The file below](../swift-service/src/test/java/com/facebook/swift/service/annotations/DerivedServiceOne.java) compiled into `swift-service/target/test-classes/com/facebook/swift/service/annotations/DerivedServiceOne.class`
```java
package com.facebook.swift.service.annotations;

import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;

@ThriftService("DerivedServiceOne")
public interface DerivedServiceOne extends BaseService
{
    @ThriftMethod
    public void fooOne();
}
```

Output:
```
namespace java.swift com.facebook.swift.service.annotations
namespace py mycompany.thrift
namespace java com.mycompany
namespace cpp mycompany

include "path/to/base.thrift"



service DerivedServiceOne extends base.BaseService {
  void fooOne();
}
```
