/*
 * Copyright (C) 2012-2013 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.nifty.core;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.ThreadNameDeterminer;

import javax.annotation.PreDestroy;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

public final class NiftyTimer
    extends HashedWheelTimer
    implements Closeable
{
    public NiftyTimer(String prefix, long tickDuration, TimeUnit unit, int ticksPerWheel)
    {
        super(new ThreadFactoryBuilder().setNameFormat(prefix + "-timer-%s").setDaemon(true).build(),
              ThreadNameDeterminer.CURRENT,
              tickDuration,
              unit,
              ticksPerWheel);
    }

    public NiftyTimer(String prefix)
    {
        this(prefix, 100, TimeUnit.MILLISECONDS, 512);
    }

    @PreDestroy
    @Override
    public void close()
    {
        stop();
    }
}
