/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.service.puma;

import com.facebook.nifty.core.NiftyBootstrap;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.ThriftClientManager;
import com.facebook.swift.service.ThriftServiceProcessor;
import com.facebook.swift.service.puma.swift.PumaReadServer;
import com.facebook.swift.service.puma.swift.PumaReadService;
import com.facebook.swift.service.puma.swift.ReadQueryInfoTimeString;
import com.facebook.swift.service.puma.swift.ReadResultQueryInfoTimeString;
import com.facebook.swift.service.puma.swift.ReadSemanticException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.thrift.TProcessor;
import org.testng.annotations.Test;

import java.util.List;

import static com.facebook.swift.service.SwiftServerHelper.createNiftyBootstrap;
import static com.facebook.swift.service.SwiftServerHelper.getRandomPort;
import static com.google.common.net.HostAndPort.fromParts;
import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * Demonstrates creating Puma.
 */
public class TestPuma {

  private final ImmutableList<ReadQueryInfoTimeString> requestArgs = ImmutableList.of(
      new ReadQueryInfoTimeString(
          "foo",
          "now",
          "later",
          42,
          ImmutableMap.of("a", "b"),
          ImmutableList.of("apple", "banana")
      ),
      new ReadQueryInfoTimeString(
          "bar",
          "snack",
          "attack",
          33,
          ImmutableMap.of("c", "d"),
          ImmutableList.of("cheetos", "doritos")
      )
  );

  @Test(groups = "fast")
  public void testPumaDirect() throws Exception {
    PumaReadServer puma = new PumaReadServer();

    List<ReadResultQueryInfoTimeString> results = puma.getResultTimeString(requestArgs);
    verifyResults(results);
  }

  @Test(groups = "fast")
  public void testPumaSwift() throws Exception {
    // create server and start
    PumaReadServer puma = new PumaReadServer();
    TProcessor processor = new ThriftServiceProcessor(puma, new ThriftCodecManager());
    int port = getRandomPort();
    NiftyBootstrap bootstrap = createNiftyBootstrap(processor, port);

    // create client
    ThriftClientManager clientManager = new ThriftClientManager();
    try (PumaReadService pumaClient = clientManager.createClient(
        fromParts("localhost", port),
        PumaReadService.class
    )) {
      // invoke puma
      List<ReadResultQueryInfoTimeString> results = pumaClient.getResultTimeString(requestArgs);
      verifyResults(results);

    } finally {
      // stop server
      bootstrap.stop();
    }
  }

  @Test(groups = "fast")
  public void testPumaDirectException() throws Exception {
    PumaReadServer puma = new PumaReadServer();
    ReadSemanticException exception = new ReadSemanticException("my exception");
    puma.setException(exception);

    try {
      puma.getResultTimeString(requestArgs);
      fail("Expected ReadSemanticException");
    } catch (ReadSemanticException e) {
      assertEquals(e, exception);
    }
  }

  @Test(groups = "fast")
  public void testPumaSwiftException() throws Exception {
    PumaReadServer puma = new PumaReadServer();
    ReadSemanticException exception = new ReadSemanticException("my exception");
    puma.setException(exception);

    ThriftClientManager clientManager = new ThriftClientManager();

    TProcessor processor = new ThriftServiceProcessor(puma, new ThriftCodecManager());
    int port = getRandomPort();
    NiftyBootstrap bootstrap = createNiftyBootstrap(processor, port);
    try (PumaReadService pumaClient = clientManager.createClient(
        fromParts("localhost", port),
        PumaReadService.class
    )) {

      pumaClient.getResultTimeString(requestArgs);
      fail("Expected ReadSemanticException");
    } catch (ReadSemanticException e) {
      assertEquals(e, exception);
    } finally {
      bootstrap.stop();
    }
  }

  private void verifyResults(List<ReadResultQueryInfoTimeString> results) {
    assertThat(results)
        .as("results")
        .hasSize(2)
        .containsSequence(
            new ReadResultQueryInfoTimeString(
                "now",
                ImmutableMap.of(
                    "apple",
                    "apple",
                    "banana",
                    "banana"
                )
            ),
            new ReadResultQueryInfoTimeString(
                "snack",
                ImmutableMap.of(
                    "cheetos",
                    "cheetos",
                    "doritos",
                    "doritos"
                )
            )
        );
  }
}
