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
package com.facebook.swift.generator.template;

import com.facebook.swift.generator.SwiftDocumentContext;

import java.net.URI;

public abstract class BaseJavaContext implements JavaContext {

  protected final SwiftDocumentContext swiftDocumentContext;

  public BaseJavaContext(SwiftDocumentContext swiftDocumentContext) {
    this.swiftDocumentContext = swiftDocumentContext;
  }

  public String getSourceIDLPath() throws Exception {
    URI inputBase = swiftDocumentContext.getSwiftGeneratorConfig().getInputBase();
    URI idlUri = swiftDocumentContext.getThriftIdlUri();
    return inputBase.relativize(idlUri).getPath();
  }
}
