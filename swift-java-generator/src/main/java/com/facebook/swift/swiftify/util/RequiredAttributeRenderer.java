package com.facebook.swift.swiftify.util;

import com.facebook.swift.parser.model.ThriftField;
import org.stringtemplate.v4.AttributeRenderer;

import java.util.Locale;

public class RequiredAttributeRenderer implements AttributeRenderer
{
    @Override
    public String toString(Object o, String formatString, Locale locale)
    {
        ThriftField.Required required = (ThriftField.Required) o;
        if (required == ThriftField.Required.REQUIRED) {
            return "true";
        } else {
            return "false";
        }
    }
}
