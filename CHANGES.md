Changes
=======

* 0.13.0

- Server overload behavior: you can set the maximum number of queued requests, and server will throw a TApplicationException if the queue length would be exceeded
- swift-generator support for optional fields: optional fields that would otherwise generate into primitive types will used boxed types instead
- swift2thrift-generator support for optional fields: fields annotation as requiredness=OPTIONAL will generate into optional fields in thrift IDL
- BREAKING CHANGE: @ThriftStruct and @ThriftUnion classes used with this release (and all future releases) must all be declared 'final'
- BREAKING CHANGE: The @ThriftField annotation field 'required' (a boolean) was changed to 'requiredness' (an enum covering optional)
- Many bug fixes compared to 0.12.0

* 0.11.0

- Add a config option (thrift.client.receive-timeout) to ThriftClientConfig
- Change default value for thrift.client.read-timeout from 1 minute to 10 seconds
- Set default value for thrift.client.receive-timeout to 1 minute
- Add constructors to ThriftClientManager that also take a receiveTimeout.


