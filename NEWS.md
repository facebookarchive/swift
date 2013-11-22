News
====

* 0.11.0

The nifty client code has only one timeout when reading: readTimeout.

It controls the amount of time that a client is willing for the server
to complete a response.

This release introduces a second timeout, receiveTimeout and changes
the semantics of readTimeout:

- receiveTimeout now reflects the amount of time that a client is
  willing to wait for the server complete a response.

- readTimeout is the amount of a time that can pass without the server
  sending any data.

In the most general sense, readTimeout is the maximum time to first
byte and receiveTimeout is the maximum time to last byte for a
response. However, readTimeout also controls the amount of time that a
server can fall silent during sending out a response before the client
times out.

As long as a server makes progress and keeps sending out data
continuously, the readTimeout will not fire. The receiveTimout will
fire even if while the server sends data.
