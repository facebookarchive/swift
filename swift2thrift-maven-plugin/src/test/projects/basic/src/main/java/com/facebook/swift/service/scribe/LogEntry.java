/*
 * Copyright (C) 2015 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.swift.service.scribe;

import com.facebook.swift.codec.*;
import com.facebook.swift.codec.ThriftField.Requiredness;
import java.util.*;

import static com.google.common.base.Objects.toStringHelper;

@ThriftStruct("LogEntry")
public final class LogEntry
{
    public LogEntry() {
    }

    private String category;

    @ThriftField(value=1, name="category", requiredness=Requiredness.NONE)
    public String getCategory() { return category; }

    @ThriftField
    public void setCategory(final String category) { this.category = category; }

    private String message;

    @ThriftField(value=2, name="message", requiredness=Requiredness.NONE)
    public String getMessage() { return message; }

    @ThriftField
    public void setMessage(final String message) { this.message = message; }

    @Override
    public String toString()
    {
        return toStringHelper(this)
            .add("category", category)
            .add("message", message)
            .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LogEntry other = (LogEntry)o;

        return
            Objects.equals(category, other.category) &&
            Objects.equals(message, other.message);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(new Object[] {
            category,
            message
        });
    }
}
