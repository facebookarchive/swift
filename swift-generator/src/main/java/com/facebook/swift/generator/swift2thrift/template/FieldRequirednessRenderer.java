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
package com.facebook.swift.generator.swift2thrift.template;

import com.facebook.swift.codec.ThriftField;
import org.stringtemplate.v4.AttributeRenderer;

import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;

public class FieldRequirednessRenderer implements AttributeRenderer
{
    @Override
    public String toString(Object o, String formatString, Locale locale)
    {
        checkArgument(o instanceof ThriftField.Requiredness);

        ThriftField.Requiredness requiredness = (ThriftField.Requiredness)o;

        switch (requiredness) {
            case NONE:
                return "";

            case REQUIRED:
                return "required";

            case OPTIONAL:
                return "optional";

            default:
                throw new IllegalArgumentException("Invalid value for field requiredness");
        }
    }
}
