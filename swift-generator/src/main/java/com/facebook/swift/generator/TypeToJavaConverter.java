package com.facebook.swift.generator;

import com.facebook.swift.parser.model.BaseType;
import com.facebook.swift.parser.model.IdentifierType;
import com.facebook.swift.parser.model.ListType;
import com.facebook.swift.parser.model.MapType;
import com.facebook.swift.parser.model.SetType;
import com.facebook.swift.parser.model.ThriftType;
import com.facebook.swift.parser.model.VoidType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.util.EnumMap;
import java.util.List;

public class TypeToJavaConverter
{
    private final TypeRegistry typeRegistry;
    private final List<Converter> converters;

    public TypeToJavaConverter(final TypeRegistry typeRegistry)
    {
        this.typeRegistry = typeRegistry;
        final ImmutableList.Builder<Converter> builder = ImmutableList.builder();
        builder.add(new VoidConverter());
        builder.add(new BaseConverter());
        builder.add(new IdentifierConverter());
        builder.add(new SetConverter());
        builder.add(new ListConverter());
        builder.add(new MapConverter());
        converters = builder.build();
    }

    public String convertType(final ThriftType thriftType)
    {
        for (Converter converter : converters) {
            if (converter.accept(thriftType)) {
                return converter.convert(thriftType);
            }
        }
        throw new IllegalArgumentException("Thrift type %s is unknown!");
    }

    private static interface Converter
    {
        boolean accept(ThriftType type);

        String convert(ThriftType type);
    }

    private static class VoidConverter implements Converter
    {
        public boolean accept(final ThriftType type)
        {
            return type.getClass() == VoidType.class;
        }

        public String convert(final ThriftType type)
        {
            return "void";
        }
    }

    private static class BaseConverter implements Converter
    {
        private static final EnumMap<BaseType.Type, String> JAVA_TYPE_MAP;

        static {
            final EnumMap<BaseType.Type, String> javaTypeMap = Maps.newEnumMap(BaseType.Type.class);
            javaTypeMap.put(BaseType.Type.BOOL, "boolean");
            javaTypeMap.put(BaseType.Type.BYTE, "byte");
            javaTypeMap.put(BaseType.Type.I16, "short");
            javaTypeMap.put(BaseType.Type.I32, "int");
            javaTypeMap.put(BaseType.Type.I64, "long");
            javaTypeMap.put(BaseType.Type.DOUBLE, "double");
            javaTypeMap.put(BaseType.Type.STRING, "String");
            javaTypeMap.put(BaseType.Type.BINARY, "byte []");
            JAVA_TYPE_MAP = javaTypeMap;
        }

        public boolean accept(final ThriftType type)
        {
            return type.getClass() == BaseType.class;
        }

        public String convert(final ThriftType type)
        {
            final BaseType.Type baseType = ((BaseType) type).getType();
            return JAVA_TYPE_MAP.get(baseType);
        }
    }

    private class IdentifierConverter implements Converter
    {
        public boolean accept(final ThriftType type)
        {
            return type.getClass() == IdentifierType.class;
        }

        public String convert(final ThriftType type)
        {
            final String name = ((IdentifierType) type).getName();
            if (name.indexOf('.') == -1) {
                return typeRegistry.findType(typeRegistry.getDefaultThriftNamespace(), name).getSimpleName();
            }
            else {
                return typeRegistry.findType(name).getClassName();
            }
        }
    }

    private class SetConverter implements Converter
    {
        public boolean accept(final ThriftType type)
        {
            return type.getClass() == SetType.class;
        }

        public String convert(final ThriftType type)
        {
        final SetType setType = SetType.class.cast(type);

        final String actualType = convertType(setType.getType());

        return "Set<" + actualType + ">";
    }
    }

    private class ListConverter implements Converter
    {
        public boolean accept(final ThriftType type)
        {
            return type.getClass() == ListType.class;
        }

        public String convert(final ThriftType type)
        {
        final ListType listType = ListType.class.cast(type);

        final String actualType = convertType(listType.getType());

        return "List<" + actualType + ">";
    }
    }

    private class MapConverter implements Converter
    {
        public boolean accept(final ThriftType type)
        {
            return type.getClass() == MapType.class;
        }

        public String convert(final ThriftType type)
        {
        final MapType mapType = MapType.class.cast(type);

        final String actualKeyType = convertType(mapType.getKeyType());
        final String actualValueType = convertType(mapType.getValueType());

        return String.format("Map<%s, %s>", actualKeyType, actualValueType);
    }
    }
}

