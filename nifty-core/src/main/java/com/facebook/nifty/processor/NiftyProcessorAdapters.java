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
package com.facebook.nifty.processor;

import com.facebook.nifty.core.RequestContext;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;

public class NiftyProcessorAdapters
{
    public static NiftyProcessor processorFromTProcessor(final TProcessor standardThriftProcessor)
    {
        checkProcessMethodSignature();

        return new NiftyProcessor()
        {
            @Override
            public boolean process(TProtocol in, TProtocol out, RequestContext requestContext) throws TException
            {
                return standardThriftProcessor.process(in, out);
            }
        };
    }

    public static NiftyProcessorFactory factoryFromTProcessor(final TProcessor standardThriftProcessor)
    {
        checkProcessMethodSignature();

        return new NiftyProcessorFactory()
        {
            @Override
            public NiftyProcessor getProcessor(TTransport transport)
            {
                return processorFromTProcessor(standardThriftProcessor);
            }
        };
    }

    public static NiftyProcessorFactory factoryFromTProcessorFactory(final TProcessorFactory standardThriftProcessorFactory)
    {
        checkProcessMethodSignature();

        return new NiftyProcessorFactory()
        {
            @Override
            public NiftyProcessor getProcessor(TTransport transport)
            {
                return processorFromTProcessor(standardThriftProcessorFactory.getProcessor(transport));
            }
        };
    }

    /**
     * Catch the mismatch early if someone tries to pass our internal variant of TProcessor with
     * a different signature on the process() method into these adapters.
     */
    private static void checkProcessMethodSignature()
    {
        try {
            TProcessor.class.getMethod("process", TProtocol.class, TProtocol.class);
        }
        catch (NoSuchMethodException e) {
            // Facebook's TProcessor variant needs processor adapters from a different package
            throw new IllegalStateException("The loaded TProcessor class is not supported by version of the adapters");
        }
    }
}
