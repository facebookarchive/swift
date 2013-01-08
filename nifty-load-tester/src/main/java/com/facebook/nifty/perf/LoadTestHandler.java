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
package com.facebook.nifty.perf;

import java.lang.InterruptedException;
import java.lang.Thread;
import java.lang.System;
import java.nio.ByteBuffer;

import org.apache.thrift.TException;

public class LoadTestHandler implements LoadTest.Iface {
  public void noop() {
  }

  public void onewayNoop() {
  }

  public void asyncNoop() {
  }

  public long add(long a, long b) {
    return a + b;
  }

  public ByteBuffer echo(ByteBuffer data) {
    return data;
  }

  public void send(ByteBuffer data) {
  }

  public ByteBuffer recv(long recvBytes) {
    return ByteBuffer.allocate((int)recvBytes);
  }

  public ByteBuffer sendrecv(ByteBuffer data, long recvBytes) {
    return recv(recvBytes);
  }

  public void onewaySend(ByteBuffer data) {
  }

  public void onewayThrow(int code) throws TException {
    throw new TException();
  }

  public void throwUnexpected(int code) throws TException {
    throw new TException();
  }

  public void throwError(int code) throws LoadError {
    throw new LoadError(code);
  }

  public void sleep(long microseconds) {
    try {
      long ms = microseconds / 1000;
      int us = (int)(microseconds % 1000);
      Thread.sleep(ms, us);
    }
    catch (InterruptedException e) {
    }
  }

  public void onewaySleep(long microseconds) {
    sleep(microseconds);
  }

  public void badBurn(long microseconds) {
    burnImpl(microseconds);
  }

  public void badSleep(long microseconds) {
    burnImpl(microseconds);
  }

  public void onewayBurn(long microseconds) {
    burnImpl(microseconds);
  }

  public void burn(long microseconds) {
    burnImpl(microseconds);
  }

  private void burnImpl(long microseconds) {
    long end = System.nanoTime() + microseconds;
    while (System.nanoTime() < end) {}
  }
}
