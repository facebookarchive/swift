/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.miffed;

import com.facebook.miffed.BonkBuilder.Builder;

import javax.annotation.concurrent.Immutable;

@Immutable
@ThriftStruct(name = "Bonk", builder = Builder.class)
public class BonkBuilder
{
    private final String message;
    private final int type;

    public BonkBuilder(
            String message,
            int type)
    {
        this.message = message;
        this.type = type;
    }

    @ThriftField(id = 1)
    public String getMessage()
    {
        return message;
    }

    @ThriftField(id = 2)
    public int getType()
    {
        return type;
    }

    public static class Builder
    {
        private String message;
        private int type;

        @ThriftField(name = "message")
        public Builder setMessage(String message)
        {
            this.message = message;
            return this;
        }

        @ThriftField(name = "message")
        public Builder setType(int type)
        {
            this.type = type;
            return this;
        }

        @ThriftConstructor
        public BonkBuilder create()
        {
            return new BonkBuilder(message, type);
        }
    }
}
