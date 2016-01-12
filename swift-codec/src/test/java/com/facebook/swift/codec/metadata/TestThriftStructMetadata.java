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

import com.facebook.swift.codec.BonkBean;
import com.facebook.swift.codec.BonkBuilder;
import com.facebook.swift.codec.BonkConstructor;
import com.facebook.swift.codec.BonkField;
import com.facebook.swift.codec.BonkMethod;
import com.facebook.swift.codec.ThriftIdlAnnotation;
import com.facebook.swift.codec.metadata.ThriftStructMetadata.MetadataType;

import org.testng.annotations.Test;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class TestThriftStructMetadata
{
    @Test
    public void testField()
    {
        ThriftStructMetadata metadata = testMetadataBuild(BonkField.class, 0, 0);

        verifyFieldInjection(metadata, 1, "message");
        verifyFieldExtraction(metadata, 1, "message");
        verifyFieldInjection(metadata, 2, "type");
        verifyFieldExtraction(metadata, 2, "type");
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

    @Test
    public void testBean()
    {
        ThriftStructMetadata metadata = testMetadataBuild(BonkBean.class, 0, 2);
        verifyParameterInjection(metadata, 1, "message", 0);
        verifyMethodExtraction(metadata, 1, "message", "getMessage");
        verifyParameterInjection(metadata, 2, "type", 0);
        verifyMethodExtraction(metadata, 2, "type", "getType");
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

    @Test
    public void testConstructor()
    {
        ThriftStructMetadata metadata = testMetadataBuild(BonkConstructor.class, 2, 0);
        verifyParameterInjection(metadata, 1, "message", 0);
        verifyMethodExtraction(metadata, 1, "message", "getMessage");
        verifyParameterInjection(metadata, 2, "type", 1);
        verifyMethodExtraction(metadata, 2, "type", "getType");
    }

    @Test
    public void testMethod()
    {
        ThriftStructMetadata metadata = testMetadataBuild(BonkMethod.class, 0, 1);
        verifyParameterInjection(metadata, 1, "message", 0);
        verifyMethodExtraction(metadata, 1, "message", "getMessage");
        verifyParameterInjection(metadata, 2, "type", 1);
        verifyMethodExtraction(metadata, 2, "type", "getType");
    }

    @Test
    public void testBuilder()
    {
        ThriftStructMetadata metadata = testMetadataBuild(BonkBuilder.class, 0, 2);
        verifyParameterInjection(metadata, 1, "message", 0);
        verifyMethodExtraction(metadata, 1, "message", "getMessage");
        verifyParameterInjection(metadata, 2, "type", 0);
        verifyMethodExtraction(metadata, 2, "type", "getType");
    }

    @Test
    public void testFieldIdlAnnotations()
    {
        ThriftStructMetadata metadata = testMetadataBuild(BonkBean.class, 0, 2);
        Map<String, String> idlAnnotations = metadata.getField(1).getIdlAnnotations();
        assertTrue(idlAnnotations.size() == 1);
        assertTrue(idlAnnotations.get("testkey").equals("testvalue"));
    }

    private ThriftStructMetadata testMetadataBuild(Class<?> structClass, int expectedConstructorParameters, int expectedMethodInjections)
    {
        ThriftCatalog catalog = new ThriftCatalog();
        ThriftStructMetadataBuilder builder = new ThriftStructMetadataBuilder(catalog, structClass);
        assertNotNull(builder);

        assertNotNull(builder.getMetadataErrors());
        builder.getMetadataErrors().throwIfHasErrors();
        assertEquals(builder.getMetadataErrors().getWarnings().size(), 0);

        ThriftStructMetadata metadata = builder.build();
        assertNotNull(metadata);
        assertEquals(MetadataType.STRUCT, metadata.getMetadataType());

        verifyField(metadata, 1, "message");
        verifyField(metadata, 2, "type");

        ThriftConstructorInjection constructorInjection = metadata.getConstructorInjection().get();
        assertNotNull(constructorInjection);
        assertEquals(constructorInjection.getParameters().size(), expectedConstructorParameters);

        assertEquals(metadata.getMethodInjections().size(), expectedMethodInjections);

        return metadata;
    }

    private <T> void verifyField(ThriftStructMetadata metadata, int id, String name)
    {
        ThriftFieldMetadata messageField = metadata.getField(id);
        assertNotNull(messageField, "messageField is null");
        assertEquals(messageField.getId(), id);
        assertEquals(messageField.getName(), name);
        assertFalse(messageField.isReadOnly());
        assertFalse(messageField.isWriteOnly());

        assertTrue(messageField.getExtraction().isPresent());
        ThriftExtraction extraction = messageField.getExtraction().get();
        assertEquals(extraction.getId(), id);
        assertEquals(extraction.getName(), name);

        assertNotNull(messageField.getInjections());
        assertEquals(messageField.getInjections().size(), 1);
        ThriftInjection injection = messageField.getInjections().get(0);
        assertEquals(injection.getId(), id);
        assertEquals(injection.getName(), name);
    }
}
