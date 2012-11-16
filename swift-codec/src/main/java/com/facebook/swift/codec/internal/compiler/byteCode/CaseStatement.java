/*
 * Copyright (C) 2012 Facebook, Inc.
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
package com.facebook.swift.codec.internal.compiler.byteCode;

import javax.annotation.concurrent.Immutable;

@Immutable
public class CaseStatement
{
    public static CaseStatement caseStatement(int key, String label)
    {
        return new CaseStatement(label, key);
    }

    private final int key;
    private final String label;

    CaseStatement(String label, int key)
    {
        this.label = label;
        this.key = key;
    }

    public String getLabel()
    {
        return label;
    }

    public int getKey()
    {
        return key;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("CaseStatement");
        sb.append("{label='").append(label).append('\'');
        sb.append(", value=").append(key);
        sb.append('}');
        return sb.toString();
    }
}
