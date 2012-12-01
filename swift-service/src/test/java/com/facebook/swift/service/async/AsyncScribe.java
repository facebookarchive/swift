package com.facebook.swift.service.async;

import com.facebook.swift.service.LogEntry;
import com.facebook.swift.service.ResultCode;
import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.thrift.TException;

import java.util.List;

@ThriftService
public interface AsyncScribe extends AutoCloseable
{
    @ThriftMethod(value = "Log")
    public ListenableFuture<ResultCode> log(List<LogEntry> logEntries)
        throws TException;
}
