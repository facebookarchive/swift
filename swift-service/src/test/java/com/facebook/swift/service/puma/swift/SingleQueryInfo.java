/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service.puma.swift;

import com.facebook.swift.codec.ThriftConstructor;
import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

@ThriftStruct
public class SingleQueryInfo {
  private final String logicalTableName;
  private final Map<String, String> filter;
  private final List<String> selectList;

  @ThriftConstructor
  public SingleQueryInfo(
      String logicalTableName,
      Map<String, String> filter,
      List<String> selectList
  ) {
    this.logicalTableName = logicalTableName;
    if (filter != null) {
      this.filter = ImmutableMap.copyOf(filter);
    } else {
      this.filter = ImmutableMap.of();
    }
    if (selectList != null) {
      this.selectList = ImmutableList.copyOf(selectList);
    } else {
      this.selectList = ImmutableList.of();
    }
  }

  @ThriftField(1)
  public String getLogicalTableName() {
    return logicalTableName;
  }

  @ThriftField(2)
  public Map<String, String> getFilter() {
    return filter;
  }

  @ThriftField(3)
  public List<String> getSelectList() {
    return selectList;
  }
}
