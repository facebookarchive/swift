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

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftUnion;
import com.facebook.swift.codec.ThriftUnionId;
import org.testng.annotations.Test;

import java.util.concurrent.locks.Lock;

import static org.fest.assertions.Assertions.assertThat;

public class TestThriftUnionMetadataBuilder
{
    @Test
    public void testNoId()
            throws Exception
    {
        ThriftUnionMetadataBuilder builder = new ThriftUnionMetadataBuilder(new ThriftCatalog(), NoId.class);

        MetadataErrors metadataErrors = builder.getMetadataErrors();

        assertThat(metadataErrors.getErrors())
                .as("metadata errors")
                .hasSize(1);

        assertThat(metadataErrors.getWarnings())
                .as("metadata warnings")
                .isEmpty();

        assertThat(metadataErrors.getErrors().get(0).getMessage())
                .as("error message")
                .containsIgnoringCase("not have an id");
    }

    @ThriftUnion
    public final static class NoId
    {
        @ThriftUnionId
        public short id;

        @ThriftField
        public String getField1()
        {
            return null;
        }

        @ThriftField
        public void setField1(String value)
        {
        }
    }

    @Test
    public void testMultipleIds()
            throws Exception
    {
        ThriftUnionMetadataBuilder builder = new ThriftUnionMetadataBuilder(new ThriftCatalog(), MultipleIds.class);

        MetadataErrors metadataErrors = builder.getMetadataErrors();

        assertThat(metadataErrors.getErrors())
                .as("metadata errors")
                .hasSize(1);

        assertThat(metadataErrors.getWarnings())
                .as("metadata warnings")
                .isEmpty();

        assertThat(metadataErrors.getErrors().get(0).getMessage())
                .as("error message")
                .containsIgnoringCase("multiple ids");
    }

    @ThriftUnion
    public final static class MultipleIds
    {
        @ThriftUnionId
        public void setId(short id)
        {
        }

        @ThriftField(name = "foo", value = 1)
        public void setField1(String value)
        {
        }

        @ThriftField(name = "foo", value = 2)
        public void setField2(String value)
        {
        }

        @ThriftField(name = "foo")
        public String getField1()
        {
            return null;
        }

        @ThriftField(name = "foo")
        public String getField2()
        {
            return null;
        }
    }

    @Test
    public void testMultipleNames()
            throws Exception
    {
        ThriftUnionMetadataBuilder builder = new ThriftUnionMetadataBuilder(new ThriftCatalog(), MultipleNames.class);

        MetadataErrors metadataErrors = builder.getMetadataErrors();

        assertThat(metadataErrors.getErrors())
                .as("metadata errors")
                .isEmpty();

        assertThat(metadataErrors.getWarnings())
                .as("metadata warnings")
                .hasSize(1);

        assertThat(metadataErrors.getWarnings().get(0).getMessage())
                .as("error message")
                .containsIgnoringCase("multiple names");
    }

    @ThriftUnion
    public final static class MultipleNames
    {
        @ThriftUnionId
        public void setId(short id)
        {
        }

        @ThriftField(value = 1, name = "foo")
        public String getFoo()
        {
            return null;
        }

        @ThriftField(value = 1, name = "bar")
        public void setFoo(String value)
        {
        }
    }

    @Test
    public void testUnsupportedType()
            throws Exception
    {
        ThriftUnionMetadataBuilder builder = new ThriftUnionMetadataBuilder(new ThriftCatalog(), UnsupportedJavaType.class);

        MetadataErrors metadataErrors = builder.getMetadataErrors();

        assertThat(metadataErrors.getErrors())
                .as("metadata errors")
                .hasSize(1);

        assertThat(metadataErrors.getWarnings())
                .as("metadata warnings")
                .isEmpty();

        assertThat(metadataErrors.getErrors().get(0).getMessage())
                .as("error message")
                .containsIgnoringCase("not a supported Java type");
    }

    @ThriftUnion
    public final static class UnsupportedJavaType
    {
        @ThriftUnionId
        public void setId(short id)
        {
        }

        @ThriftField(1)
        public Lock unsupportedJavaType;
    }

    @Test
    public void testMultipleTypes()
            throws Exception
    {
        ThriftUnionMetadataBuilder builder = new ThriftUnionMetadataBuilder(new ThriftCatalog(), MultipleTypes.class);

        MetadataErrors metadataErrors = builder.getMetadataErrors();

        assertThat(metadataErrors.getErrors())
                .as("metadata errors")
                .hasSize(1);

        assertThat(metadataErrors.getWarnings())
                .as("metadata warnings")
                .isEmpty();

        assertThat(metadataErrors.getErrors().get(0).getMessage())
                .as("error message")
                .containsIgnoringCase("multiple types");
    }

    @ThriftUnion
    public final static class MultipleTypes
    {
        @ThriftUnionId
        public void setId(short id)
        {
        }

        @ThriftField(1)
        public int getFoo()
        {
            return 0;
        }

        @ThriftField
        public void setFoo(short value)
        {
        }
    }

    @Test
    public void testNonFinalUnionOk()
    {
        ThriftUnionMetadataBuilder builder = new ThriftUnionMetadataBuilder(new ThriftCatalog(), NotFinalUnion.class);
        builder.build();
    }

    @ThriftUnion
    public static class NotFinalUnion
    {
        @ThriftUnionId public short id;
    }
}
