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
package com.facebook.swift.generator;

/**
 * Code generation tweaks.
 */
public enum SwiftGeneratorTweak
{
    ADD_THRIFT_EXCEPTION,     // Add TException to generated method signatures
    EXTEND_RUNTIME_EXCEPTION, // Make generated exceptions extend RuntimeException instead of Exception
    ADD_CLOSEABLE_INTERFACE,  // Make generated Services extend Closeable and add a close() method
    USE_PLAIN_JAVA_NAMESPACE,  // Use the java namespace, not the java.swift namespace
    FALLBACK_TO_PLAIN_JAVA_NAMESPACE  // First try to use java.swift namespace if it is present in IDL,
                                      // otherwise use the java namespace
}

