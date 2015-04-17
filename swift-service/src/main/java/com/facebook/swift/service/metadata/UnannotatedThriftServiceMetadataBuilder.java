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
package com.facebook.swift.service.metadata;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.facebook.swift.codec.metadata.ThriftCatalog;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/*
class ScalaReflectionUtil{
}

object ScalaReflectionUtil{

    def describe(t: Type) { //scala.reflect.universe.runtime.Type ) { //reflect.universe.runtime.Tyet: Type) {
        println("DECLARATIONS")
        println(t.declarations.mkString("\n"))
        println("MEMBERS")
        println(t.members.mkString("\n"))
        println("ERASURE")
        println(t.erasure)
        println("SYMBOL")
        println(t.typeSymbol)
        println("CTOR")
        println(t.typeConstructor)
      }

      val mirror = runtimeMirror(getClass.getClassLoader)
      def describe(someClass: Class[_]) {
        val classSymbol = mirror.classSymbol(someClass)
        println(s"ANNOTATIONS: ${classSymbol.annotations}")
        println(classSymbol)
        val scalaType: Type = classSymbol.toType
        println(scalaType)
        describe(scalaType)
      }
    
}
*/

/**
 * Created ThriftServiceMetadata objects for ThriftServiceProcessor is a way
 * that's a little more loosey-goosey. Facilitates providing thrift services of
 * existing interfaces.
 */
public class UnannotatedThriftServiceMetadataBuilder implements ThriftServiceMetadataBuilder
{
    private static final Logger LOG = LoggerFactory.getLogger(UnannotatedThriftServiceMetadataBuilder.class);

    private ThriftCatalog catalog;

    /**
     * Skip methods containing these substrings
     */
    private ArrayList<String> ignoredMethodNameSubstrings = new ArrayList<String>();

    /**
     * Skip methods that cannot by Swift2Thrifted instead of raising an error.
     */
    private boolean skipBadMethods = false;

    public UnannotatedThriftServiceMetadataBuilder(ThriftCatalog catalog) {
        Preconditions.checkNotNull(catalog, "catalog is null");
        this.catalog = catalog;
    }

    public ThriftServiceMetadata build(Class<?> serviceClass) {
        LOG.debug("Grokking " + serviceClass.getName());

        Preconditions.checkNotNull(serviceClass, "serviceClass is null");
        if (ThriftServiceMetadata.hasThriftServiceAnnotation(serviceClass))
        {
            // Use annotated policy is there was an @ThriftService annotation.
            return new ThriftServiceMetadata(serviceClass, catalog);
        }

        final boolean allowUnannotated = true;

        String serviceName = serviceClass.getSimpleName();

        // Unannotated policy.
        ArrayList<ThriftMethodMetadata> methods = new ArrayList<ThriftMethodMetadata>();
        for (Method method : serviceClass.getMethods()) {
            if (!isMethodsAllowed(method)) {
                continue;
            }

            ThriftMethodMetadata meta = null;
            if (skipBadMethods) {
                try {
                    meta = new ThriftMethodMetadata(serviceName, method, catalog, allowUnannotated);
                } catch (IllegalArgumentException e) {
                    LOG.warn("Could not add " + serviceName + "." + method.getName() + ": " + e.getMessage());
                    continue;
                }
            }
            else {
                meta = new ThriftMethodMetadata(serviceName, method, catalog, allowUnannotated);
            }
            LOG.debug("Added method: " + serviceName + "." + method.getName() );
            methods.add(meta);
        }

        ImmutableList.Builder<ThriftServiceMetadata> parentServiceBuilder = ImmutableList.builder();
        /*
        for (Class<?> parent : serviceClass.getInterfaces()) {
            parentServiceBuilder.add(this.build(parent));
        }
        */

        return new ThriftServiceMetadata(
                // name =
                serviceName,
                // documentation,
                ImmutableList.<String> of(),
                // methods
                ImmutableList.copyOf(methods),
                // parents class methods
                parentServiceBuilder.build());
    }

    private boolean isMethodsAllowed(Method method) {
        String methodName = method.getName();
        for (String ignore : ignoredMethodNameSubstrings)
        {
            if (methodName.contains(ignore)) {
                return false;
            }
        }
        return true;
    }

    void addNameFilter(String ignore) {
        ignoredMethodNameSubstrings.add(ignore);
    }

    void skipBadMethods(boolean skipBadMethods) {
        this.skipBadMethods = skipBadMethods;
    }

    public static UnannotatedThriftServiceMetadataBuilder makeScalaBuilder(ThriftCatalog catalog) {
        UnannotatedThriftServiceMetadataBuilder result = new UnannotatedThriftServiceMetadataBuilder(catalog);
        result.addNameFilter("$");
        result.skipBadMethods(false);
        //result.skipBadMethods(true);
        return result;
    }
}