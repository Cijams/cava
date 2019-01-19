/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.scuttlebutt.discovery;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.junit.VertxExtension;
import net.consensys.cava.junit.VertxInstance;
import net.consensys.cava.scuttlebutt.Identity;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicReference;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.logl.Level;
import org.logl.LoggerProvider;
import org.logl.logl.SimpleLogger;

@ExtendWith(VertxExtension.class)
class ScuttlebuttLocalDiscoveryServiceTest {

  @Test
  void startStop(@VertxInstance Vertx vertx) throws Exception {
    ScuttlebuttLocalDiscoveryService service = new ScuttlebuttLocalDiscoveryService(
        vertx,
        LoggerProvider.nullProvider().getLogger("test"),
        8008,
        "0.0.0.0",
        "233.0.10.0");
    service.start().join();
    service.stop().join();
  }

  @Test
  void startStart(@VertxInstance Vertx vertx) throws Exception {
    ScuttlebuttLocalDiscoveryService service = new ScuttlebuttLocalDiscoveryService(
        vertx,
        LoggerProvider.nullProvider().getLogger("test"),
        8008,
        "0.0.0.0",
        "233.0.10.0");
    service.start().join();
    service.start().join();
    service.stop().join();
  }

  @Test
  void invalidMulticastAddress(@VertxInstance Vertx vertx) throws Exception {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ScuttlebuttLocalDiscoveryService(
            vertx,
            LoggerProvider.nullProvider().getLogger("test"),
            8008,
            "0.0.0.0",
            "10.0.0.0"));
  }

  @Test
  void stopFirst(@VertxInstance Vertx vertx) throws Exception {
    ScuttlebuttLocalDiscoveryService service = new ScuttlebuttLocalDiscoveryService(
        vertx,
        LoggerProvider.nullProvider().getLogger("test"),
        8008,
        "0.0.0.0",
        "233.0.10.0");
    service.stop().join();
    service.start().join();
    service.stop().join();
  }

  @Test
  void broadcastAndListen(@VertxInstance Vertx vertx) throws Exception {
    LoggerProvider loggerProvider = SimpleLogger.withLogLevel(Level.DEBUG).toPrintWriter(
        new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.err, UTF_8))));
    ScuttlebuttLocalDiscoveryService service = new ScuttlebuttLocalDiscoveryService(
        vertx,
        loggerProvider.getLogger("test"),
        8008,
        8009,
        "127.0.0.1",
        "127.0.0.1",
        false);
    ScuttlebuttLocalDiscoveryService service2 = new ScuttlebuttLocalDiscoveryService(
        vertx,
        loggerProvider.getLogger("test2"),
        8009,
        8008,
        "127.0.0.1",
        "127.0.0.1",
        false);

    try {
      service2.start().join();
      AtomicReference<LocalIdentity> ref = new AtomicReference<>();
      service2.addListener(ref::set);

      LocalIdentity localId = new LocalIdentity("10.0.0.1", 10000, Identity.random());
      service.addIdentityToBroadcastList(localId);
      service.start().join();
      service.broadcast();
      Thread.sleep(1000);
      assertNotNull(ref.get());
      assertEquals(localId, ref.get());
    } finally {
      AsyncCompletion.allOf(service2.stop(), service.stop()).join();
    }
  }

}
