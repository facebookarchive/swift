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
package com.facebook.nifty.duplex;

import org.apache.thrift.transport.TTransportFactory;

/***
 * A factory for creating a pair of protocols (one for input, one for output). Gives more
 * flexibility to the factory to enforce protocol restrictions. See {@link TDuplexProtocolFactory}
 * documentation for more details.
 */
public abstract class TDuplexTransportFactory {
    public abstract TTransportPair getTransportPair(TTransportPair transportPair);

    public static TDuplexTransportFactory fromSingleTransportFactory(
            final TTransportFactory transportFactory
    )
    {
        return new TDuplexTransportFactory() {
            @Override
            public TTransportPair getTransportPair(TTransportPair transportPair) {
                return TTransportPair.fromSeparateTransports(
                        transportFactory.getTransport(transportPair.getInputTransport()),
                        transportFactory.getTransport(transportPair.getOutputTransport()));
            }
        };
    }

    public static TDuplexTransportFactory fromSeparateTransportFactories(
            final TTransportFactory inputTransportFactory,
            final TTransportFactory outputTransportFactory
    )
    {
        return new TDuplexTransportFactory() {
            @Override
            public TTransportPair getTransportPair(TTransportPair transportPair) {
                return TTransportPair.fromSeparateTransports(
                        inputTransportFactory.getTransport(transportPair.getInputTransport()),
                        outputTransportFactory.getTransport(transportPair.getOutputTransport())
                );
            }
        };
    }
}
