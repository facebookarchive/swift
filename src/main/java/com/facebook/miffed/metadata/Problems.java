/*
 * Copyright 2010 Proofpoint, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.miffed.metadata;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.ConfigurationException;
import com.google.inject.spi.Message;

import java.util.List;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

public class Problems
{
    private final List<Message> errors = Lists.newArrayList();
    private final List<Message> warnings = Lists.newArrayList();
    private final Monitor monitor;

    public interface Monitor
    {
        void onError(Message errorMessage);

        void onWarning(Message warningMessage);
    }

    public static final NullMonitor NULL_MONITOR = new NullMonitor();

    private static final class NullMonitor implements Problems.Monitor
    {
        @Override
        public void onError(Message unused)
        {
        }

        @Override
        public void onWarning(Message unused)
        {
        }
    }

    public Problems()
    {
        this.monitor = NULL_MONITOR;
    }

    public Problems(Monitor monitor)
    {
        this.monitor = monitor;
    }

    public void throwIfHasErrors() throws ConfigurationException
    {
        if (!errors.isEmpty()) {
            throw getException();
        }
    }

    public List<Message> getErrors()
    {
        return ImmutableList.copyOf(errors);
    }

    public void addError(String format, Object... params)
    {
        Message message = new Message("Error: " + format(format, params));
        errors.add(message);
        monitor.onError(message);
    }

    public void addError(Throwable e, String format, Object... params)
    {
        Message message = new Message(emptyList(), "Error: " + format(format, params), e);
        errors.add(message);
        monitor.onError(message);
    }

    public List<Message> getWarnings()
    {
        return ImmutableList.copyOf(warnings);
    }

    public void addWarning(String format, Object... params)
    {
        Message message = new Message("Warning: " + format(format, params));
        warnings.add(message);
        monitor.onWarning(message);
    }

    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        for (Message error : errors) {
            builder.append(error.getMessage()).append('\n');
        }
        for (Message warning : warnings) {
            builder.append(warning.getMessage()).append('\n');
        }
        return builder.toString();
    }

    private ConfigurationException getException()
    {
        ImmutableList<Message> messages
                = new ImmutableList.Builder<Message>()
                .addAll(errors)
                .addAll(warnings)
                .build();

        return new ConfigurationException(messages);
    }

    public static ConfigurationException exceptionFor(String format, Object... params)
    {
        Problems problems = new Problems();
        problems.addError(format, params);
        return problems.getException();
    }

    public static ConfigurationException exceptionFor(Throwable e, String format, Object... params)
    {
        Problems problems = new Problems();
        problems.addError(e, format, params);
        return problems.getException();
    }
}
