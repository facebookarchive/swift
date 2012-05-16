/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service.puma.swift;

import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;

import java.util.List;

@ThriftService
public interface PumaReadService extends AutoCloseable {
  @ThriftMethod
  List<ReadResultQueryInfo> getResult(List<ReadQueryInfoTimeString> reader)
      throws ReadSemanticException;

  @ThriftMethod
  List<ReadResultQueryInfoTimeString> getResultTimeString(List<ReadQueryInfoTimeString> reader)
      throws ReadSemanticException;

  @ThriftMethod
  List<ReadResultQueryInfo> mergeQueryAggregation(
      MergeAggregationQueryInfo mergeAggregationQueryInfo
  )
      throws ReadSemanticException;

  @ThriftMethod
  long latestQueryableTime(String category, String appName, List<Integer> bucketNumbers)
      throws ReadSemanticException;

  @ThriftMethod
  List<Long> latestQueryableTimes(String category, String appName, List<Integer> bucketNumbers)
      throws ReadSemanticException;
}
