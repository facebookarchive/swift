News
====

* 0.11.0

Add support for Nifty 0.11.0 receiveTimeout.

The nifty code now supports two timeouts:

- receiveTimeout now reflects the amount of time that a client is
  willing to wait for the server complete a response.

- readTimeout is the amount of a time that can pass without the server
  sending any data.

Before 0.11.0, readTimeout had the semantics of receiveTimeout. 

The default value for readTimeout changed from 1 minute to 10 seconds.

If a client configuration sets thrift.client.read-timeout, this setting
must be changed to be thrift.client.receive-timeout. 

See the Nifty CHANGES.md file for more details.

