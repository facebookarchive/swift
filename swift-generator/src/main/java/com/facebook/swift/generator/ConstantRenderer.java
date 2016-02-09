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

import com.facebook.swift.parser.model.BaseType;
import com.facebook.swift.parser.model.Const;
import com.facebook.swift.parser.model.ConstIdentifier;
import com.facebook.swift.parser.model.ConstInteger;
import com.facebook.swift.parser.model.ConstList;
import com.facebook.swift.parser.model.ConstMap;
import com.facebook.swift.parser.model.ConstString;
import com.facebook.swift.parser.model.ConstValue;
import com.facebook.swift.parser.model.IdentifierType;
import com.facebook.swift.parser.model.ListType;
import com.facebook.swift.parser.model.MapType;
import com.facebook.swift.parser.model.SetType;
import com.facebook.swift.parser.model.ThriftType;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

public class ConstantRenderer
{
    private final TypeToJavaConverter typeConverter;
    private final String defaultNamespace;
    private final TypeRegistry typeRegistry;
    private final TypedefRegistry typedefRegistry;

    public ConstantRenderer(
            TypeToJavaConverter typeConverter,
            String defaultNamespace,
            TypeRegistry typeRegistry,
            TypedefRegistry typedefRegistry)
    {
        this.typeConverter = typeConverter;
        this.defaultNamespace = defaultNamespace;
        this.typeRegistry = typeRegistry;
        this.typedefRegistry = typedefRegistry;
    }

    public String render(Const constant)
    {
        return render(constant.getType(), constant.getValue());
    }

    public String render(ThriftType thriftType, ConstValue value)
    {
        if (thriftType instanceof BaseType) {
            return renderBaseConstant((BaseType) thriftType, value);
        }
        else if (thriftType instanceof ListType) {
            checkArgument(value instanceof ConstList);
            return renderListConstant((ListType) thriftType, (ConstList) value);
        }
        else if (thriftType instanceof MapType) {
            checkArgument(value instanceof ConstMap);
            return renderMapType((MapType) thriftType, (ConstMap) value);
        }
        else if (thriftType instanceof SetType) {
            checkArgument(value instanceof ConstList);
            return renderSetConstant((SetType) thriftType, (ConstList) value);
        }
        else if (thriftType instanceof IdentifierType) {
            String typeName = ((IdentifierType) thriftType).getName();
            ThriftType resolvedType = typedefRegistry.findType(defaultNamespace, typeName);
            checkState(resolvedType != null, format("Could not resolve type named '%s'", typeName));
            return render(resolvedType, value);
        }
        else {
            throw new IllegalStateException("Not yet implemented");
        }
    }

    private String renderIdentifierType(IdentifierType identifierType, ConstIdentifier value)
    {
        return value.value();
    }

    private String renderMapType(MapType mapType, ConstMap value)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("ImmutableMap.<");
        sb.append(typeConverter.convert(mapType.getKeyType(), false));
        sb.append(", ");
        sb.append(typeConverter.convert(mapType.getValueType(), false));
        sb.append(">builder()\n");

        Set<Map.Entry<ConstValue, ConstValue>> entries = value.value().entrySet();
        for (Map.Entry<ConstValue, ConstValue> entry : entries) {
            if (entry.getKey() instanceof ConstIdentifier) {
                throw new IllegalStateException("Not yet implemented");
            }
            if (entry.getValue() instanceof ConstIdentifier) {
                throw new IllegalStateException("Not yet implemented");
            }
            sb.append(format("    .put(%s, %s)\n",
                             render(mapType.getKeyType(), entry.getKey()),
                             render(mapType.getValueType(), entry.getValue())));
        }
        sb.append("    .build()");

        return sb.toString();
    }

    private String renderListConstant(ListType listType, ConstList value)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("ImmutableList.<");
        sb.append(typeConverter.convert(listType.getElementType(), false));
        sb.append(">builder()\n");

        List<ConstValue> elements = value.value();
        for (ConstValue element : elements) {
            if (element instanceof ConstIdentifier) {
                throw new IllegalStateException("Not yet implemented");
            }
            sb.append(format("    .add(%s)\n", render(listType.getElementType(), element)));
        }
        sb.append("    .build()");

        return sb.toString();
    }

    private String renderSetConstant(SetType setType, ConstList value)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("ImmutableSet.<");
        sb.append(typeConverter.convert(setType.getElementType(), false));
        sb.append(">builder()\n");

        List<ConstValue> elements = value.value();
        for (ConstValue element : elements) {
            if (element instanceof ConstIdentifier) {
                throw new IllegalStateException("Not yet implemented");
            }
            sb.append(format("    .add(%s)\n", render(setType.getElementType(), element)));
        }
        sb.append("    .build()");

        return sb.toString();
    }

    private String renderBaseConstant(BaseType baseType, ConstValue value)
    {
        switch (baseType.getType()) {
            case BOOL:
            case BYTE:
            case I16:
            case I32:
            case I64:
                checkArgument(value instanceof ConstInteger);
                return renderIntegerConstant(baseType, (ConstInteger) value);

            case STRING:
                checkArgument(value instanceof ConstString);
                return "\"" + value.value() + "\"";

            case DOUBLE:
                // Allow auto-promotion of integer types
                checkArgument(value.value() instanceof Number);
                return Double.toString(((Number) value.value()).doubleValue());

            default:
                throw new IllegalArgumentException();
        }
    }

    private String renderIntegerConstant(BaseType baseType, ConstInteger value)
    {
        switch (baseType.getType()) {
            case BOOL:
                checkArgument(
                        value.value() == 0 || value.value() == 1,
                        "Bool constant out of range '" + value.value() + "' (must be 0 or 1)");
                return Boolean.toString(value.value() == 0 ? false : true);

            case BYTE:
                checkArgument(
                        value.value() >= Byte.MIN_VALUE && value.value() <= Byte.MAX_VALUE,
                        "Byte constant out of range: '" + value.value() + "'");
                return Byte.toString((byte) (long) value.value());

            case I16:
                return Short.toString(Shorts.checkedCast(value.value()));

            case I32:
                return Integer.toString(Ints.checkedCast(value.value()));

            case I64:
                return Long.toString(value.value()) + "L";

            default:
                throw new IllegalStateException();
        }
    }
}
