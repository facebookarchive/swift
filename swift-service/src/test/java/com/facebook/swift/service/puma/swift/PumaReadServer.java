/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service.puma.swift;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

public class PumaReadServer implements PumaReadService {
  private ReadSemanticException exception;

  public void setException(ReadSemanticException exception) {
    this.exception = exception;
  }

  @Override
  public List<ReadResultQueryInfo> getResult(List<ReadQueryInfoTimeString> reader)
      throws ReadSemanticException {

    if (exception != null) {
      throw exception;
    }

    ImmutableList.Builder<ReadResultQueryInfo> result = ImmutableList.builder();
    for (ReadQueryInfoTimeString readQueryInfoTimeString : reader) {
      result.add(new ReadResultQueryInfo(1, data(readQueryInfoTimeString.getSelectList())));
    }
    return result.build();
  }

  @Override
  public List<ReadResultQueryInfoTimeString> getResultTimeString(List<ReadQueryInfoTimeString> reader)
      throws ReadSemanticException {

    if (exception != null) {
      throw exception;
    }

    ImmutableList.Builder<ReadResultQueryInfoTimeString> result = ImmutableList.builder();
    for (ReadQueryInfoTimeString readQueryInfoTimeString : reader) {
      result.add(
          new ReadResultQueryInfoTimeString(
              readQueryInfoTimeString.getStartTime(),
              data(readQueryInfoTimeString.getSelectList())
          )
      );
    }
    return result.build();
  }

  @Override
  public List<ReadResultQueryInfo> mergeQueryAggregation(
      MergeAggregationQueryInfo mergeAggregationQueryInfo
  )
      throws ReadSemanticException {
    throw new UnsupportedOperationException();
  }

  @Override
  public long latestQueryableTime(String category, String appName, List<Integer> bucketNumbers)
      throws ReadSemanticException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Long> latestQueryableTimes(
      String category,
      String appName,
      List<Integer> bucketNumbers
  )
      throws ReadSemanticException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
  }

  private Map<String, String> data(List<String> columns) {
    ImmutableMap.Builder<String, String> data = ImmutableMap.builder();
    for (String column : columns) {
      data.put(column, column);
    }
    return data.build();
  }
}
