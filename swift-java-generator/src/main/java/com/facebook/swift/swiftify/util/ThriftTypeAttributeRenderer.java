package com.facebook.swift.swiftify.util;

import com.facebook.swift.parser.model.BaseType;
import com.facebook.swift.parser.model.IdentifierType;
import com.facebook.swift.parser.model.ListType;
import com.facebook.swift.parser.model.MapType;
import com.facebook.swift.parser.model.SetType;
import com.facebook.swift.parser.model.ThriftType;
import org.stringtemplate.v4.AttributeRenderer;

import java.util.Locale;
import java.util.Map;

public class ThriftTypeAttributeRenderer implements AttributeRenderer
{
    private final Map<String, ThriftType> typedefMap;

    public ThriftTypeAttributeRenderer(Map<String, ThriftType> typedefMap)
    {
        this.typedefMap = typedefMap;
    }

    @Override
    public String toString(Object o, String formatString, Locale locale)
    {
        if (o instanceof BaseType) {
            BaseType t = (BaseType)o;
            return BaseType.getJavaTypeName(((BaseType) o).getType());
        } else if (o instanceof MapType) {
            MapType t = ((MapType) o);
            return "Map<" +
                   toString(t.getKeyType(), formatString, locale) + ", " +
                   toString(t.getValueType(), formatString, locale) + ">";
        } else if (o instanceof ListType) {
            ListType t = (ListType)o;
            return "Iterable<" + toString(t.getType(), null, locale) + ">";
        } else if (o instanceof SetType) {
            SetType t = (SetType) o;
            return "Set<" + toString(t.getType(), null, locale) + ">";
        } else if (o instanceof IdentifierType) {
            IdentifierType t = (IdentifierType) o;
            if (typedefMap.containsKey(t.getName())) {
                ThriftType mappedType = typedefMap.get(t.getName());
                return toString(mappedType, formatString, locale);
            }
            return t.getName();
        }

        throw new IllegalArgumentException("Cannot render unrecognized ThriftType");
    }
}
