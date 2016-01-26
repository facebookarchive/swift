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
package com.facebook.swift.codec.metadata;

import com.facebook.swift.codec.ThriftProtocolType;

import java.lang.reflect.Type;

/**
 * An interface to either a resolved {@link ThriftType} or the information to compute one.
 *
 * Used when computing struct/union metadata, as a placeholder for field types that might
 * not be directly resolvable yet (in cases of recursive types).
 */
public interface ThriftTypeReference
{
    Type getJavaType();

    ThriftProtocolType getProtocolType();

    boolean isRecursive();

    ThriftType get();
}
