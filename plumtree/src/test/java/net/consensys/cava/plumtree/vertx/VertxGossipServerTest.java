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
package net.consensys.cava.plumtree.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.junit.VertxExtension;
import net.consensys.cava.junit.VertxInstance;
import net.consensys.cava.plumtree.EphemeralPeerRepository;

import java.util.concurrent.atomic.AtomicReference;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class VertxGossipServerTest {

  @Test
  void gossipDeadBeefToOtherNode(@VertxInstance Vertx vertx) throws Exception {

    AtomicReference<Bytes> messageReceived1 = new AtomicReference<>();
    AtomicReference<Bytes> messageReceived2 = new AtomicReference<>();

    VertxGossipServer server1 = new VertxGossipServer(
        vertx,
        "127.0.0.1",
        10000,
        bytes -> bytes,
        new EphemeralPeerRepository(),
        messageReceived1::set,
        (message, peer) -> true);
    VertxGossipServer server2 = new VertxGossipServer(
        vertx,
        "127.0.0.1",
        10001,
        bytes -> bytes,
        new EphemeralPeerRepository(),
        messageReceived2::set,
        (message, peer) -> true);

    server1.start().join();
    server2.start().join();

    server1.connectTo("127.0.0.1", 10001).join();
    server1.gossip(Bytes.fromHexString("deadbeef"));
    Thread.sleep(1000);
    assertEquals(Bytes.fromHexString("deadbeef"), messageReceived2.get());

    server1.stop().join();
    server2.stop().join();
  }

  @Test
  void gossipDeadBeefToTwoOtherNodes(@VertxInstance Vertx vertx) throws Exception {

    AtomicReference<Bytes> messageReceived1 = new AtomicReference<>();
    AtomicReference<Bytes> messageReceived2 = new AtomicReference<>();
    AtomicReference<Bytes> messageReceived3 = new AtomicReference<>();

    VertxGossipServer server1 = new VertxGossipServer(
        vertx,
        "127.0.0.1",
        10000,
        bytes -> bytes,
        new EphemeralPeerRepository(),
        messageReceived1::set,
        (message, peer) -> true);
    VertxGossipServer server2 = new VertxGossipServer(
        vertx,
        "127.0.0.1",
        10001,
        bytes -> bytes,
        new EphemeralPeerRepository(),
        messageReceived2::set,
        (message, peer) -> true);
    VertxGossipServer server3 = new VertxGossipServer(
        vertx,
        "127.0.0.1",
        10002,
        bytes -> bytes,
        new EphemeralPeerRepository(),
        messageReceived3::set,
        (message, peer) -> true);

    server1.start().join();
    server2.start().join();
    server3.start().join();

    server1.connectTo("127.0.0.1", 10001).join();
    server3.connectTo("127.0.0.1", 10001).join();
    server1.gossip(Bytes.fromHexString("deadbeef"));
    Thread.sleep(1000);
    assertEquals(Bytes.fromHexString("deadbeef"), messageReceived2.get());
    assertEquals(Bytes.fromHexString("deadbeef"), messageReceived3.get());

    server1.stop().join();
    server2.stop().join();
  }

  @Test
  void gossipCollision(@VertxInstance Vertx vertx) throws Exception {
    AtomicReference<Bytes> messageReceived1 = new AtomicReference<>();
    AtomicReference<Bytes> messageReceived2 = new AtomicReference<>();

    EphemeralPeerRepository peerRepository1 = new EphemeralPeerRepository();
    EphemeralPeerRepository peerRepository3 = new EphemeralPeerRepository();

    VertxGossipServer server1 = new VertxGossipServer(
        vertx,
        "127.0.0.1",
        10000,
        bytes -> bytes,
        peerRepository1,
        messageReceived1::set,
        (message, peer) -> true);
    VertxGossipServer server2 = new VertxGossipServer(
        vertx,
        "127.0.0.1",
        10001,
        bytes -> bytes,
        new EphemeralPeerRepository(),
        messageReceived2::set,
        (message, peer) -> true);
    VertxGossipServer server3 = new VertxGossipServer(
        vertx,
        "127.0.0.1",
        10002,
        bytes -> bytes,
        peerRepository3,
        messageReceived2::set,
        (message, peer) -> true);

    server1.start().join();
    server2.start().join();
    server3.start().join();

    server1.connectTo("127.0.0.1", 10001).join();
    server2.connectTo("127.0.0.1", 10002).join();
    server1.connectTo("127.0.0.1", 10002).join();
    server1.gossip(Bytes.fromHexString("deadbeef"));
    Thread.sleep(1000);
    assertEquals(Bytes.fromHexString("deadbeef"), messageReceived2.get());
    Thread.sleep(1000);

    assertTrue(peerRepository1.lazyPushPeers().size() == 1 || peerRepository3.lazyPushPeers().size() == 1);

    server1.stop().join();
    server2.stop().join();
    server3.stop().join();
  }
}
