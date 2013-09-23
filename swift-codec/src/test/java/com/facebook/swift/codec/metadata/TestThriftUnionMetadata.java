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

import com.facebook.swift.codec.UnionBean;
import com.facebook.swift.codec.UnionBuilder;
import com.facebook.swift.codec.UnionConstructor;
import com.facebook.swift.codec.UnionField;
import com.facebook.swift.codec.UnionMethod;
import com.facebook.swift.codec.metadata.ThriftStructMetadata.MetadataType;

import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class TestThriftUnionMetadata
{
    @Test
    public void testField()
    {
        ThriftStructMetadata metadata = testMetadataBuild(UnionField.class, 0, 0);

        verifyFieldInjection(metadata, 1, "stringValue");
        verifyFieldExtraction(metadata, 1, "stringValue");
        verifyFieldInjection(metadata, 2, "longValue");
        verifyFieldExtraction(metadata, 2, "longValue");
        verifyFieldInjection(metadata, 3, "fruitValue");
        verifyFieldExtraction(metadata, 3, "fruitValue");
    }

    @Test
    public void testBean()
    {
        ThriftStructMetadata metadata = testMetadataBuild(UnionBean.class, 0, 3);
        verifyParameterInjection(metadata, 1, "stringValue", 0);
        verifyMethodExtraction(metadata, 1, "stringValue", "getStringValue");
        verifyParameterInjection(metadata, 2, "longValue", 0);
        verifyMethodExtraction(metadata, 2, "longValue", "getLongValue");
        verifyParameterInjection(metadata, 3, "fruitValue", 0);
        verifyMethodExtraction(metadata, 3, "fruitValue", "getFruitValue");
    }

    @Test
    public void testConstructor()
    {
        ThriftStructMetadata metadata = testMetadataBuild(UnionConstructor.class, 1, 0);
        verifyParameterInjection(metadata, 1, "stringValue", 0);
        verifyMethodExtraction(metadata, 1, "stringValue", "getStringValue");
        verifyParameterInjection(metadata, 2, "longValue", 0);
        verifyMethodExtraction(metadata, 2, "longValue", "getLongValue");
        verifyParameterInjection(metadata, 3, "fruitValue", 0);
        verifyMethodExtraction(metadata, 3, "fruitValue", "getFruitValue");
    }

    @Test
    public void testMethod()
    {
        try {
            testMetadataBuild(UnionMethod.class, 0, 1);
            fail();
        }
        catch (MetadataErrorException e) {
            assertEquals(1, e.getSuppressed().length);
            assertEquals(e.getSuppressed()[0].getClass(), MetadataErrorException.class);
            assertTrue(e.getSuppressed()[0].getMessage().contains("setData is not a supported getter or setter"));
        }
    }

    @Test
    public void testBuilder()
    {
        ThriftStructMetadata metadata = testMetadataBuild(UnionBuilder.class, 0, 3);
        verifyParameterInjection(metadata, 1, "stringValue", 0);
        verifyMethodExtraction(metadata, 1, "stringValue", "getStringValue");
        verifyParameterInjection(metadata, 2, "longValue", 0);
        verifyMethodExtraction(metadata, 2, "longValue", "getLongValue");
        verifyParameterInjection(metadata, 3, "fruitValue", 0);
        verifyMethodExtraction(metadata, 3, "fruitValue", "getFruitValue");
    }

    private void verifyFieldInjection(ThriftStructMetadata metadata, int id, String name)
    {
        ThriftInjection injection = metadata.getField(id).getInjections().get(0);
        assertThat(injection).isNotNull().isInstanceOf(ThriftFieldInjection.class);
        ThriftFieldInjection fieldInjection = (ThriftFieldInjection) injection;
        assertEquals(fieldInjection.getField().getName(), name);
    }

    private void verifyFieldExtraction(ThriftStructMetadata metadata, int id, String name)
    {
        assertTrue(metadata.getField(id).getExtraction().isPresent());
        ThriftExtraction extraction = metadata.getField(id).getExtraction().get();
        assertThat(extraction).isInstanceOf(ThriftFieldExtractor.class);
        ThriftFieldExtractor fieldExtractor = (ThriftFieldExtractor) extraction;
        assertEquals(fieldExtractor.getField().getName(), name);
    }

    private void verifyParameterInjection(ThriftStructMetadata metadata, int id, String name, int parameterIndex)
    {
        ThriftInjection injection = metadata.getField(id).getInjections().get(0);
        assertThat(injection).isNotNull().isInstanceOf(ThriftParameterInjection.class);
        ThriftParameterInjection parameterInjection = (ThriftParameterInjection) injection;
        assertEquals(parameterInjection.getId(), id);
        assertEquals(parameterInjection.getName(), name);
        assertEquals(parameterInjection.getParameterIndex(), parameterIndex);
    }

    private void verifyMethodExtraction(ThriftStructMetadata metadata, int id, String name, String methodName)
    {
        assertTrue(metadata.getField(id).getExtraction().isPresent());
        ThriftExtraction extraction = metadata.getField(id).getExtraction().get();
        assertThat(extraction).isInstanceOf(ThriftMethodExtractor.class);
        ThriftMethodExtractor methodExtractor = (ThriftMethodExtractor) extraction;
        assertEquals(methodExtractor.getMethod().getName(), methodName);
        assertEquals(methodExtractor.getName(), name);
    }

    private ThriftStructMetadata testMetadataBuild(Class<?> structClass, int expectedConstructorParameters, int expectedMethodInjections)
    {
        ThriftCatalog catalog = new ThriftCatalog();
        ThriftUnionMetadataBuilder builder = new ThriftUnionMetadataBuilder(catalog, structClass);
        assertNotNull(builder);

        assertNotNull(builder.getMetadataErrors());
        builder.getMetadataErrors().throwIfHasErrors();
        assertEquals(builder.getMetadataErrors().getWarnings().size(), 0);

        ThriftStructMetadata metadata = builder.build();
        assertNotNull(metadata);
        assertEquals(MetadataType.UNION, metadata.getMetadataType());

        verifyField(metadata, 1, "stringValue");
        verifyField(metadata, 2, "longValue");
        verifyField(metadata, 3, "fruitValue");

        if (expectedConstructorParameters == 0) {
            assertTrue(metadata.getConstructorInjection().isPresent());
            ThriftConstructorInjection constructorInjection = metadata.getConstructorInjection().get();
            assertEquals(constructorInjection.getParameters().size(), 0);
        }
        else {
            for (ThriftFieldMetadata fieldMetadata : metadata.getFields(FieldKind.THRIFT_FIELD)) {
                assertTrue(fieldMetadata.getConstructorInjection().isPresent());
                assertEquals(fieldMetadata.getConstructorInjection().get().getParameters().size(), expectedConstructorParameters);
            }
        }

        assertEquals(metadata.getMethodInjections().size(), expectedMethodInjections);

        return metadata;
    }

    private <T> void verifyField(ThriftStructMetadata metadata, int id, String name)
    {
        ThriftFieldMetadata metadataField = metadata.getField(id);
        assertNotNull(metadataField, "metadataField is null");
        assertEquals(metadataField.getId(), id);
        assertEquals(metadataField.getName(), name);
        assertFalse(metadataField.isReadOnly());
        assertFalse(metadataField.isWriteOnly());

        assertTrue(metadataField.getExtraction().isPresent());
        ThriftExtraction extraction = metadataField.getExtraction().get();
        assertEquals(extraction.getId(), id);
        assertEquals(extraction.getName(), name);

        assertNotNull(metadataField.getInjections());
        assertEquals(metadataField.getInjections().size(), 1);
        ThriftInjection injection = metadataField.getInjections().get(0);
        assertEquals(injection.getId(), id);
        assertEquals(injection.getName(), name);
    }
}
