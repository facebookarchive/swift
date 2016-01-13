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
package com.facebook.swift.testing;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.testng.ITestContext;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import java.nio.file.Files;
import static java.nio.file.Files.walkFileTree;

public class TestingUtils {

    public static Path getResourcePath(String resourceName) {
        try {
            return Paths.get(Resources.getResource(resourceName).toURI());
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    public static List<Path> listMatchingFiles(Path start, String glob)
            throws IOException {
        final ImmutableList.Builder<Path> list = ImmutableList.builder();
        final PathMatcher matcher = start.getFileSystem().getPathMatcher("glob:" + glob);
        walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (matcher.matches(file)) {
                    list.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return list.build();
    }

    public static String getTestParameter(ITestContext context, String parameterName) {
        String value = context.getCurrentXmlTest().getParameter(parameterName);
        return checkNotNull(value, "test parameter not set: %s", parameterName);
    }

    public static Iterator<Object[]> listDataProvider(Object... list) {
        return listDataProvider(Arrays.asList(list));
    }

    public static Iterator<Object[]> listDataProvider(List<?> list) {
        return Lists.transform(list, new Function<Object, Object[]>() {
            @Override
            public Object[] apply(@Nullable Object input) {
                return new Object[]{input};
            }
        }).iterator();
    }

    public static void deleteRecursively(Path path) throws IOException {

        // Symbolic link friendly recursive delete
        // Inspired by: http://stackoverflow.com/questions/779519/delete-directories-recursively-in-java/8685959#8685959
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                // Try to delete again anyway (see StackOverflow)
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    // Propagate the exception
                    throw exc;
                }

                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
