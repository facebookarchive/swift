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

import com.google.common.base.Preconditions;

import javax.annotation.concurrent.Immutable;
import java.lang.reflect.Method;

/**
 * Coercions map from native types to types known by the Thrift protocol.
 */
@Immutable
public class TypeCoercion
{
    /**
     * thriftType.java type represents the native type.
     * thriftType.uncoercedType represents the protocol type.
     * 
     * Expecting thriftType.protcolType to be COERCED. But it looks like in some cases it represented to protocol type.
     */
    private final ThriftType thriftType;

    /**
     * Maps from native type to protocol type.
     */
    private final Method toThrift;
    
    /**
     * Maps from protcol type to native type.
     */
    private final Method fromThrift;
    
    /**
     * Must be null, or of the type that owns the methods.
     */
    private final Object methodObject;



    public TypeCoercion(ThriftType thriftType, Method toThrift, Method fromThrift)
    {
        this(thriftType, toThrift, fromThrift, null);
    }

    public TypeCoercion(ThriftType thriftType, Method toThrift, Method fromThrift, Object methodObject)
    {
        Preconditions.checkNotNull(thriftType, "thriftType is null");
        Preconditions.checkNotNull(toThrift, "toThrift is null");
        Preconditions.checkNotNull(fromThrift, "fromThrift is null");

        this.thriftType = thriftType;
        this.toThrift = toThrift;
        this.fromThrift = fromThrift;
        // TODO verify (if not null), that methods belong to methodObject.
        this.methodObject = methodObject;
    }

    final public ThriftType getThriftType()
    {
        return thriftType;
    }

    final public Method getToThrift()
    {
        return toThrift;
    }

    final public Method getFromThrift()
    {
        return fromThrift;
    }

    final public Object getMethodObject() {
        return methodObject;
    }
    
    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("TypeCoercion");
        sb.append("{thriftType=").append(thriftType);
        sb.append(", toThrift=").append(toThrift);
        sb.append(", fromThrift=").append(fromThrift);
        sb.append('}');
        return sb.toString();
    }
}
