/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service.puma.swift;

import com.facebook.swift.codec.ThriftConstructor;
import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

@ThriftStruct
public class ReadSemanticException extends Exception {
  @ThriftConstructor
  public ReadSemanticException(String message) {
    super(message);
  }

  @ThriftField(1)
  public String getMessage() {
    return super.getMessage();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final ReadSemanticException that = (ReadSemanticException) o;

    if (getMessage() != null ? !getMessage().equals(that.getMessage()) : that.getMessage() != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return getMessage() != null ? getMessage().hashCode() : 0;
  }
}
