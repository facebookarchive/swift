/*
 * Copyright (C) 2012-2016 Facebook, Inc.
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

import org.apache.thrift.protocol.TProtocol;

/***
 * Represents a pair of protocols: one for input and one for output.
 */
public class TProtocolPair {
    private final TProtocol inputProtocol;
    private final TProtocol outputProtocol;

    protected TProtocolPair(TProtocol inputProtocol, TProtocol outputProtocol)
    {
        this.inputProtocol = inputProtocol;
        this.outputProtocol = outputProtocol;
    }

    public TProtocol getInputProtocol()
    {
        return inputProtocol;
    }

    public TProtocol getOutputProtocol()
    {
        return outputProtocol;
    }

    public static TProtocolPair fromSeparateProtocols(final TProtocol inputProtocol,
                                                      final TProtocol outputProtocol)
    {
        return new TProtocolPair(inputProtocol, outputProtocol);
    }

    public static TProtocolPair fromSingleProtocol(final TProtocol protocol)
    {
        return new TProtocolPair(protocol, protocol);
    }
}
