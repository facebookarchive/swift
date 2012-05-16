/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service.puma.swift;

import com.facebook.swift.codec.ThriftConstructor;
import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;
import com.google.common.collect.ImmutableList;

import java.util.List;

@ThriftStruct
public class MergeAggregationQueryInfo {
  private final List<SingleQueryInfo> queries;
  private final long startTime;
  private final long endTime;
  private final int numSteps;

  @ThriftConstructor
  public MergeAggregationQueryInfo(
      List<SingleQueryInfo> queries,
      long startTime,
      long endTime,
      int numSteps
  ) {
    if (queries != null) {
      this.queries = ImmutableList.copyOf(queries);
    } else {
      this.queries = ImmutableList.of();
    }
    this.startTime = startTime;
    this.endTime = endTime;
    this.numSteps = numSteps;
  }

  @ThriftField(1)
  public List<SingleQueryInfo> getQueries() {
    return queries;
  }

  @ThriftField(2)
  public long getStartTime() {
    return startTime;
  }

  @ThriftField(3)
  public long getEndTime() {
    return endTime;
  }

  @ThriftField(4)
  public int getNumSteps() {
    return numSteps;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("MergeAggregationQueryInfo");
    sb.append("{queries=").append(queries);
    sb.append(", startTime=").append(startTime);
    sb.append(", endTime=").append(endTime);
    sb.append(", numSteps=").append(numSteps);
    sb.append('}');
    return sb.toString();
  }
}
