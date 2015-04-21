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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * <p>MetadataErrors collects all known issues with metadata extraction.  This allows all known
 * problems to be reported together instead of one at a time.</p>
 *
 * <p>This code is heavily based on https://github.com/dain/platform/blob/master/configuration/src/main/java/com/proofpoint/configuration/Problems.java</p>
 */
@NotThreadSafe
public class MetadataErrors
{
    private final List<MetadataErrorException> errors = Lists.newArrayList();
    private final List<MetadataWarningException> warnings = Lists.newArrayList();
    private final Monitor monitor;

    public interface Monitor
    {
        void onError(MetadataErrorException errorMessage);

        void onWarning(MetadataWarningException warningMessage);
    }

    public static final NullMonitor NULL_MONITOR = new NullMonitor();

    private static final class NullMonitor implements MetadataErrors.Monitor
    {
        @Override
        public void onError(MetadataErrorException unused)
        {
        }

        @Override
        public void onWarning(MetadataWarningException unused)
        {
        }
    }

    public MetadataErrors()
    {
        this.monitor = NULL_MONITOR;
    }

    public MetadataErrors(Monitor monitor)
    {
        this.monitor = monitor;
    }

    public void throwIfHasErrors()
            throws MetadataErrorException
    {
        if (!errors.isEmpty()) {
            MetadataErrorException exception = new MetadataErrorException(
                    "Metadata extraction encountered %s errors and %s warnings",
                    errors.size(),
                    warnings.size()
            );
            for (MetadataErrorException error : errors) {
                exception.addSuppressed(error);
            }
            for (MetadataWarningException warning : warnings) {
                exception.addSuppressed(warning);
            }
            throw exception;
        }
    }

    public List<MetadataErrorException> getErrors()
    {
        return ImmutableList.copyOf(errors);
    }

    public void addError(String format, Object... params)
    {
        MetadataErrorException message = new MetadataErrorException(format, params);
        errors.add(message);
        monitor.onError(message);
    }

    public void addError(Throwable e, String format, Object... params)
    {
        MetadataErrorException message = new MetadataErrorException(e, format, params);
        errors.add(message);
        monitor.onError(message);
    }

    public List<MetadataWarningException> getWarnings()
    {
        return ImmutableList.copyOf(warnings);
    }

    public void addWarning(String format, Object... params)
    {
        MetadataWarningException message = new MetadataWarningException(format, params);
        warnings.add(message);
        monitor.onWarning(message);
    }

    public void addWarning(Throwable e, String format, Object... params)
    {
        MetadataWarningException message = new MetadataWarningException(e, format, params);
        warnings.add(message);
        monitor.onWarning(message);
    }

    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        for (MetadataErrorException error : errors) {
            builder.append(error.getMessage()).append('\n');
        }
        for (MetadataWarningException warning : warnings) {
            builder.append(warning.getMessage()).append('\n');
        }
        return builder.toString();
    }
}
