/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.reflection;

import com.facebook.swift.ThriftCodec;
import com.facebook.swift.ThriftCodecManager;
import com.facebook.swift.compiler.TProtocolReader;
import com.facebook.swift.compiler.TProtocolWriter;
import com.facebook.swift.metadata.ThriftConstructorInjection;
import com.facebook.swift.metadata.ThriftExtraction;
import com.facebook.swift.metadata.ThriftFieldExtractor;
import com.facebook.swift.metadata.ThriftFieldInjection;
import com.facebook.swift.metadata.ThriftFieldMetadata;
import com.facebook.swift.metadata.ThriftInjection;
import com.facebook.swift.metadata.ThriftMethodExtractor;
import com.facebook.swift.metadata.ThriftMethodInjection;
import com.facebook.swift.metadata.ThriftParameterInjection;
import com.facebook.swift.metadata.ThriftStructMetadata;
import com.facebook.swift.metadata.ThriftType;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSortedMap;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

public class ReflectionThriftCodec<T> implements ThriftCodec<T> {
  private final ThriftStructMetadata<T> metadata;
  private final SortedMap<Short, ThriftCodec<?>> fields;

  public ReflectionThriftCodec(
      ThriftCodecManager manager,
      ThriftStructMetadata<T> metadata
  ) {
    this.metadata = metadata;
    ImmutableSortedMap.Builder<Short, ThriftCodec<?>> fields = ImmutableSortedMap.naturalOrder();
    for (ThriftFieldMetadata fieldMetadata : metadata.getFields()) {
      fields.put(fieldMetadata.getId(), manager.getCodec(fieldMetadata.getType()));
    }
    this.fields = fields.build();
  }

  @Override
  public ThriftType getType() {
    return ThriftType.struct(metadata);
  }

  @Override
  public T read(TProtocolReader protocol) throws Exception {
    protocol.readStructBegin();

    Map<Short, Object> data = new HashMap<>(metadata.getFields().size());
    while (protocol.nextField()) {
      short fieldId = protocol.getFieldId();

      // do we have a codec for this field
      ThriftCodec<?> codec = fields.get(fieldId);
      if (codec == null) {
        protocol.skipFieldData();
        continue;
      }
      // is this field readable
      ThriftFieldMetadata fieldMetadata = metadata.getField(fieldId);
      if (fieldMetadata.isWriteOnly()) {
        protocol.skipFieldData();
        continue;
      }

      // read the value
      Object value = protocol.readField(codec);
      data.put(fieldId, value);
    }
    protocol.readStructEnd();

    // build the struct
    return constructStruct(data);
  }

  @Override
  public void write(T instance, TProtocolWriter protocol) throws Exception {
    protocol.writeStructBegin(metadata.getStructName());
    for (ThriftFieldMetadata fieldMetadata : metadata.getFields()) {
      // is the field writable?
      if (fieldMetadata.isReadOnly()) {
        continue;
      }

      // get the field value
      Object fieldValue = getFieldValue(instance, fieldMetadata);

      // write the field
      ThriftCodec<Object> codec = (ThriftCodec<Object>) fields.get(fieldMetadata.getId());
      protocol.writeField(fieldMetadata.getName(), fieldMetadata.getId(), codec, fieldValue);
    }
    protocol.writeStructEnd();
  }

  private T constructStruct(Map<Short, Object> data)
      throws Exception {

    // construct instance
    Object instance;
    {
      ThriftConstructorInjection constructor = metadata.getConstructor();
      Object[] parametersValues = new Object[constructor.getParameters().size()];
      for (ThriftParameterInjection parameter : constructor.getParameters()) {
        parametersValues[parameter.getParameterIndex()] = data.get(parameter.getId());
      }

      try {
        instance = constructor.getConstructor().newInstance(parametersValues);
      } catch (InvocationTargetException e) {
        if (e.getTargetException() != null) {
          Throwables.propagateIfInstanceOf(e.getTargetException(), Exception.class);
        }
        throw e;
      }
    }

    // inject fields
    for (ThriftFieldMetadata fieldMetadata : metadata.getFields()) {
      for (ThriftInjection injection : fieldMetadata.getInjections()) {
        if (injection instanceof ThriftFieldInjection) {
          ThriftFieldInjection fieldInjection = (ThriftFieldInjection) injection;
          Object value = data.get(fieldInjection.getId());
          if (value != null) {
            fieldInjection.getField().set(instance, value);
          }
        }
      }
    }

    // inject methods
    for (ThriftMethodInjection methodInjection : metadata.getMethodInjections()) {
      Object[] parametersValues = new Object[methodInjection.getParameters().size()];
      for (ThriftParameterInjection parameter : methodInjection.getParameters()) {
        parametersValues[parameter.getParameterIndex()] = data.get(parameter.getId());
      }

      try {
        methodInjection.getMethod().invoke(instance, parametersValues);
      } catch (InvocationTargetException e) {
        if (e.getTargetException() != null) {
          Throwables.propagateIfInstanceOf(e.getTargetException(), Exception.class);
        }
        throw e;
      }
    }

    // builder method
    if (metadata.getBuilderMethod() != null) {
      ThriftMethodInjection builderMethod = metadata.getBuilderMethod();
      Object[] parametersValues = new Object[builderMethod.getParameters().size()];
      for (ThriftParameterInjection parameter : builderMethod.getParameters()) {
        parametersValues[parameter.getParameterIndex()] = data.get(parameter.getId());
      }

      try {
        instance = builderMethod.getMethod().invoke(instance, parametersValues);
        if (instance == null) {
          throw new IllegalArgumentException("Builder method returned a null instance");

        }
        if (!metadata.getStructClass().isInstance(instance)) {
          throw new IllegalArgumentException(
              String.format(
                  "Builder method returned instance of type %s, but an instance of %s is required",
                  instance.getClass().getName(),
                  metadata.getStructClass().getName()
              )
          );
        }
      } catch (InvocationTargetException e) {
        if (e.getTargetException() != null) {
          Throwables.propagateIfInstanceOf(e.getTargetException(), Exception.class);
        }
        throw e;
      }
    }

    return (T) instance;
  }

  private Object getFieldValue(Object instance, ThriftFieldMetadata field)
      throws Exception {
    try {
      ThriftExtraction extraction = field.getExtraction();
      if (extraction instanceof ThriftFieldExtractor) {
        ThriftFieldExtractor thriftFieldExtractor = (ThriftFieldExtractor) extraction;
        return thriftFieldExtractor.getField().get(instance);
      } else if (extraction instanceof ThriftMethodExtractor) {
        ThriftMethodExtractor thriftMethodExtractor = (ThriftMethodExtractor) extraction;
        return thriftMethodExtractor.getMethod().invoke(instance);
      } else {
        throw new IllegalAccessException(
            "Unsupported field extractor type " + extraction.getClass()
                .getName()
        );
      }
    } catch (InvocationTargetException e) {
      if (e.getTargetException() != null) {
        Throwables.propagateIfInstanceOf(e.getTargetException(), Exception.class);
      }
      throw e;
    }
  }
}
