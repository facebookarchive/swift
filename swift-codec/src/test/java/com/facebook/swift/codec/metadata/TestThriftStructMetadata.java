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
import com.facebook.swift.codec.idlannotations.BeanWIthConflictingIdlAnnotationMapsForField;
import com.facebook.swift.codec.idlannotations.BeanWithMatchingIdlAnnotationsMapsForField;
import com.facebook.swift.codec.idlannotations.BeanWithOneIdlAnnotationMapForField;
import com.facebook.swift.codec.idlannotations.ExceptionWithIdlAnnotations;
import com.facebook.swift.codec.idlannotations.StructWithIdlAnnotations;
import com.facebook.swift.codec.idlannotations.UnionWithIdlAnnotations;
import com.facebook.swift.codec.metadata.ThriftStructMetadata.MetadataType;

import org.testng.annotations.Test;

import java.lang.reflect.Type;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class TestThriftStructMetadata
{
    @Test
    public void testField() throws Exception
    {
        ThriftStructMetadata metadata = testStructMetadataBuild(BonkField.class, 0, 0);

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
    public void testBean() throws Exception
    {
        ThriftStructMetadata metadata = testStructMetadataBuild(BonkBean.class, 0, 2);
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
    public void testConstructor() throws Exception
    {
        ThriftStructMetadata metadata = testStructMetadataBuild(BonkConstructor.class, 2, 0);
        verifyParameterInjection(metadata, 1, "message", 0);
        verifyMethodExtraction(metadata, 1, "message", "getMessage");
        verifyParameterInjection(metadata, 2, "type", 1);
        verifyMethodExtraction(metadata, 2, "type", "getType");
    }

    @Test
    public void testMethod() throws Exception
    {
        ThriftStructMetadata metadata = testStructMetadataBuild(BonkMethod.class, 0, 1);
        verifyParameterInjection(metadata, 1, "message", 0);
        verifyMethodExtraction(metadata, 1, "message", "getMessage");
        verifyParameterInjection(metadata, 2, "type", 1);
        verifyMethodExtraction(metadata, 2, "type", "getType");
    }

    @Test
    public void testBuilder() throws Exception
    {
        ThriftStructMetadata metadata = testStructMetadataBuild(BonkBuilder.class, 0, 2);
        verifyParameterInjection(metadata, 1, "message", 0);
        verifyMethodExtraction(metadata, 1, "message", "getMessage");
        verifyParameterInjection(metadata, 2, "type", 0);
        verifyMethodExtraction(metadata, 2, "type", "getType");
    }

    @Test
    public void testFieldWithOneIdlAnnotationMap() throws Exception
    {
        /**
         * Single field with IDL annotation map on getter, but not on setter: should be okay
         */
        ThriftStructMetadata metadata = testStructMetadataBuild(BeanWithOneIdlAnnotationMapForField.class, 0, 1);
        Map<String, String> idlAnnotations = metadata.getField(2).getIdlAnnotations();
        assertEquals(idlAnnotations.size(), 2);
        assertEquals(idlAnnotations.get("testkey1"), "testvalue1");
        assertEquals(idlAnnotations.get("testkey2"), "testvalue2");
    }

    @Test
    public void testFieldWithMatchingIdlAnnotationMaps() throws Exception
    {
        /**
         * Single field with matching IDL annotation maps on setter vs getter: should be okay
         */
        ThriftStructMetadata metadata = testStructMetadataBuild(BeanWithMatchingIdlAnnotationsMapsForField.class, 0, 1);
        Map<String, String> idlAnnotations = metadata.getField(2).getIdlAnnotations();
        assertEquals(idlAnnotations.size(), 2);
        assertEquals(idlAnnotations.get("testkey1"), "testvalue1");
        assertEquals(idlAnnotations.get("testkey2"), "testvalue2");
    }

    @Test(expectedExceptions = MetadataErrorException.class)
    public void testFieldWithConflictingIdlAnnotationMap() throws Exception
    {
        /**
         * Single field with conflicting IDL annotation maps on setter vs getter: should fail
         */
        testStructMetadataBuild(BeanWIthConflictingIdlAnnotationMapsForField.class, 0, 1);
    }

    @Test
    public void testStructWithIdlAnnotationsMap() throws Exception
    {
        ThriftStructMetadata metadata = testStructMetadataBuild(StructWithIdlAnnotations.class, 0, 0);
        Map<String, String> idlAnnotations = metadata.getIdlAnnotations();
        assertEquals(idlAnnotations.size(), 2);
        assertEquals(idlAnnotations.get("testkey1"), "testvalue1");
        assertEquals(idlAnnotations.get("testkey2"), "testvalue2");
    }

    @Test
    public void testUnionWithIdlAnnotationsMap() throws Exception
    {
        ThriftStructMetadata metadata = testUnionMetadataBuild(UnionWithIdlAnnotations.class, 0, 2);
        Map<String, String> idlAnnotations = metadata.getIdlAnnotations();
        assertEquals(idlAnnotations.size(), 2);
        assertEquals(idlAnnotations.get("testkey1"), "testvalue1");
        assertEquals(idlAnnotations.get("testkey2"), "testvalue2");
    }

    @Test
    public void testExceptionWithIdlAnnotationsMap() throws Exception
    {
        ThriftStructMetadata metadata = testStructMetadataBuild(ExceptionWithIdlAnnotations.class, 2, 0);
        Map<String, String> idlAnnotations = metadata.getIdlAnnotations();
        assertEquals(idlAnnotations.size(), 1);
        assertEquals(idlAnnotations.get("message"), "message");
    }

    private ThriftStructMetadata testStructMetadataBuild(
            Class<?> structClass,
            int expectedConstructorParameters,
            int expectedMethodInjections)
            throws Exception
    {
        return testMetadataBuild(
                ThriftStructMetadataBuilder.class,
                structClass,
                expectedConstructorParameters,
                expectedMethodInjections);
    }

    private ThriftStructMetadata testUnionMetadataBuild(
            Class<?> structClass,
            int expectedConstructorParameters,
            int expectedMethodInjections)
            throws Exception
    {
        return testMetadataBuild(
                ThriftUnionMetadataBuilder.class,
                structClass,
                expectedConstructorParameters,
                expectedMethodInjections);
    }

    private <T extends AbstractThriftMetadataBuilder> ThriftStructMetadata testMetadataBuild(
            Class<T> metadataBuilderType,
            Class<?> structClass,
            int expectedConstructorParameters,
            int expectedMethodInjections)
            throws Exception
    {
        ThriftStructMetadata metadata = buildMetadata(structClass, metadataBuilderType);
        assertNotNull(metadata);
        assertTrue(
                MetadataType.UNION == metadata.getMetadataType() ||
                MetadataType.STRUCT == metadata.getMetadataType());

        verifyField(metadata, 1, "message");
        verifyField(metadata, 2, "type");

        ThriftConstructorInjection constructorInjection = metadata.getConstructorInjection().get();
        assertNotNull(constructorInjection);
        assertEquals(constructorInjection.getParameters().size(), expectedConstructorParameters);

        assertEquals(metadata.getMethodInjections().size(), expectedMethodInjections);

        return metadata;
    }

    private <T extends AbstractThriftMetadataBuilder> ThriftStructMetadata buildMetadata(
            Class<?> structClass,
            Class<T> metadataBuilderType)
            throws Exception
    {
        ThriftCatalog catalog = new ThriftCatalog();
        AbstractThriftMetadataBuilder builder =
                metadataBuilderType.getConstructor(ThriftCatalog.class, Type.class)
                                   .newInstance(catalog, structClass);
        assertNotNull(builder);

        assertNotNull(builder.getMetadataErrors());
        builder.getMetadataErrors().throwIfHasErrors();
        assertEquals(builder.getMetadataErrors().getWarnings().size(), 0);

        return builder.build();
    }

    private <T> void verifyField(ThriftStructMetadata metadata, int id, String name)
    {
        ThriftFieldMetadata messageField = metadata.getField(id);
        assertNotNull(messageField, "field '" + name + "' is null");
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
