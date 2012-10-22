package com.facebook.swift.generator.template;

import com.facebook.swift.generator.SwiftJavaType;
import com.facebook.swift.generator.TypeRegistry;
import com.facebook.swift.generator.TypeToJavaConverter;
import com.facebook.swift.parser.model.AbstractStruct;
import com.facebook.swift.parser.model.Service;
import com.facebook.swift.parser.model.ThriftField;
import com.facebook.swift.parser.model.ThriftMethod;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

public class ContextGenerator
{
    private final TypeRegistry typeRegistry;
    private final TypeToJavaConverter typeConverter;

    public ContextGenerator(final TypeRegistry typeRegistry)
    {
        this.typeRegistry = typeRegistry;
        this.typeConverter = new TypeToJavaConverter(typeRegistry);
    }

    public ServiceContext serviceFromThrift(final Service service)
    {
        final SwiftJavaType javaType = typeRegistry.findType(typeRegistry.getDefaultThriftNamespace(), service.getName());
        final SwiftJavaType parentType = typeRegistry.findType(service.getParent().orNull());

        return new ServiceContext(service.getName(),
                                  javaType.getPackage(),
                                  javaType.getSimpleName(),
                                  parentType == null ? null : parentType.getClassName());
    }

    public StructContext structFromThrift(final AbstractStruct struct)
    {
        final SwiftJavaType javaType = typeRegistry.findType(typeRegistry.getDefaultThriftNamespace(), struct.getName());

        return new StructContext(struct.getName(),
                                 javaType.getPackage(),
                                 javaType.getSimpleName());
    }

    public MethodContext methodFromThrift(final ThriftMethod method)
    {
        return new MethodContext(method.getName(),
                                 method.isOneway(),
                                 mangleCase(method.getName()),
                                 typeConverter.convertType(method.getReturnType()));
    }

    public FieldContext fieldFromThrift(final ThriftField field)
    {
        Preconditions.checkState(field.getIdentifier().isPresent(), "exception %s has no identifier!", field.getName());

        return new FieldContext(field.getName(),
                                field.getIdentifier().get().shortValue(),
                                typeConverter.convertType(field.getType()),
                                mangleCase(field.getName()),
                                getterName(field),
                                setterName(field));
    }

    public ExceptionContext exceptionFromThrift(final ThriftField field)
    {
        Preconditions.checkState(field.getIdentifier().isPresent(), "exception %s has no identifier!", field.getName());
        return new ExceptionContext(typeConverter.convertType(field.getType()), field.getIdentifier().get().shortValue());
    }

    private String mangleCase(final String src)
    {
        final StringBuilder sb = new StringBuilder();
        if (!StringUtils.isBlank(src)) {
            boolean upCase = false;
            for (int i = 0; i < src.length(); i++) {
                if (src.charAt(i) == '_') {
                    upCase = true;
                    continue;
                }
                else {
                    sb.append(upCase ? Character.toUpperCase(src.charAt(i)) : src.charAt(i));
                    upCase = false;
                }
            }
        }
        return sb.toString();
    }

    private String getterName(final ThriftField field)
    {
        final String type = typeConverter.convertType(field.getType());
        return ("boolean".equals(type) ? "is" : "get") + StringUtils.capitalize(mangleCase(field.getName()));
    }

    private String setterName(final ThriftField field)
    {
        return "set" + StringUtils.capitalize(mangleCase(field.getName()));
    }
}
