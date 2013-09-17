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
import com.facebook.swift.codec.ThriftStruct;
import org.testng.annotations.Test;

import java.util.concurrent.locks.Lock;

import static org.fest.assertions.Assertions.assertThat;

public class TestThriftStructMetadataBuilder
{
    @Test
    public void testNoId()
            throws Exception
    {
        ThriftStructMetadataBuilder builder = new ThriftStructMetadataBuilder(new ThriftCatalog(), NoId.class);

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

    @ThriftStruct
    public static class NoId
    {
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
        ThriftStructMetadataBuilder builder = new ThriftStructMetadataBuilder(new ThriftCatalog(), MultipleIds.class);

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

    @ThriftStruct
    public static class MultipleIds
    {
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
        ThriftStructMetadataBuilder builder = new ThriftStructMetadataBuilder(new ThriftCatalog(), MultipleNames.class);

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

    @ThriftStruct
    public static class MultipleNames
    {
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
        ThriftStructMetadataBuilder builder = new ThriftStructMetadataBuilder(new ThriftCatalog(), UnsupportedJavaType.class);

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

    @ThriftStruct
    public static class UnsupportedJavaType
    {
        @ThriftField(1)
        public Lock unsupportedJavaType;
    }

    @Test
    public void testMultipleTypes()
            throws Exception
    {
        ThriftStructMetadataBuilder builder = new ThriftStructMetadataBuilder(new ThriftCatalog(), MultipleTypes.class);

        MetadataErrors metadataErrors = builder.getMetadataErrors();

        assertThat(metadataErrors.getErrors())
                .as("metadata errors")
                .isEmpty();

        assertThat(metadataErrors.getWarnings())
                .as("metadata warnings")
                .hasSize(1);

        assertThat(metadataErrors.getWarnings().get(0).getMessage())
                .as("error message")
                .containsIgnoringCase("multiple types");
    }

    @ThriftStruct
    public static class MultipleTypes
    {
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
}
