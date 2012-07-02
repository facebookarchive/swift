/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.codec.metadata;

public class MetadataErrorException extends RuntimeException
{
    public MetadataErrorException(String formatString, Object... args)
    {
        super("Error: " + String.format(formatString, args));
    }

    public MetadataErrorException(Throwable cause, String formatString, Object... args)
    {
        super("Error: " + String.format(formatString, args), cause);
    }
}
