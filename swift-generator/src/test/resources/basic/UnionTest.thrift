#!/usr/local/bin/thrift -java

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

union foo_unique_types {
  1: string bar;
  2: i32 baz;
}

union foo_duplicate_types {
  1: i32 bar;
  2: i32 baz;
}

union foo_same_erasure {
  1: list<i32> bar;
  2: list<string> baz;
}

union foo_complicated_container {
  1: list<set<i32>> bar;
  2: list<set<string>> baz;
}
