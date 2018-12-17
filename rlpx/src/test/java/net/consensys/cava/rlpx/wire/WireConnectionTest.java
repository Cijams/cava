/*
 * Copyright 2018 ConsenSys AG.
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
package net.consensys.cava.rlpx.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.SECP256K1;
import net.consensys.cava.junit.BouncyCastleExtension;
import net.consensys.cava.rlpx.RLPxMessage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.logl.LoggerProvider;

@ExtendWith(BouncyCastleExtension.class)
class WireConnectionTest {

  private static final Bytes nodeId = SECP256K1.KeyPair.random().publicKey().bytes();
  private static final Bytes peerNodeId = SECP256K1.KeyPair.random().publicKey().bytes();

  @Test
  void disconnectIfNoHelloExchanged() {
    AtomicReference<RLPxMessage> capturedDisconnect = new AtomicReference<>();
    WireConnection conn = new WireConnection(
        "abc",
        nodeId,
        peerNodeId,
        LoggerProvider.nullProvider().getLogger("rlpx"),
        capturedDisconnect::set,
        () -> {
        },
        new LinkedHashMap<>(),
        3,
        "abc",
        10000);

    conn.messageReceived(new RLPxMessage(45, Bytes.EMPTY));
    assertEquals(1, capturedDisconnect.get().messageId());
    DisconnectMessage msg = DisconnectMessage.read(capturedDisconnect.get().content());

    assertEquals(2, msg.reason());
  }

  @Test
  void disconnectIfNoHelloReceived() {
    AtomicReference<RLPxMessage> capturedDisconnect = new AtomicReference<>();
    WireConnection conn = new WireConnection(
        "abc",
        nodeId,
        peerNodeId,
        LoggerProvider.nullProvider().getLogger("rlpx"),
        capturedDisconnect::set,
        () -> {
        },
        new LinkedHashMap<>(),
        4,
        "abc",
        10000);
    conn.sendHello();
    conn.messageReceived(new RLPxMessage(45, Bytes.EMPTY));
    assertEquals(1, capturedDisconnect.get().messageId());
    DisconnectMessage msg = DisconnectMessage.read(capturedDisconnect.get().content());

    assertEquals(2, msg.reason());
  }

  @Test
  void disconnectIfNoMapping() {
    AtomicReference<RLPxMessage> capturedDisconnect = new AtomicReference<>();
    WireConnection conn = new WireConnection(
        "abc",
        nodeId,
        peerNodeId,
        LoggerProvider.nullProvider().getLogger("rlpx"),
        capturedDisconnect::set,
        () -> {
        },
        new LinkedHashMap<>(),
        28,
        "abc",
        10000);
    conn.sendHello();
    conn.messageReceived(
        new RLPxMessage(
            0,
            HelloMessage.create(Bytes.fromHexString("deadbeef"), 30303, 3, "blah", Collections.emptyList()).toBytes()));
    conn.messageReceived(new RLPxMessage(45, Bytes.EMPTY));
    assertEquals(1, capturedDisconnect.get().messageId());
    DisconnectMessage msg = DisconnectMessage.read(capturedDisconnect.get().content());

    assertEquals(2, msg.reason());
  }

  @Test
  void disconnectIfNoNodeID() {
    AtomicReference<RLPxMessage> capturedDisconnect = new AtomicReference<>();
    WireConnection conn = new WireConnection(
        "abc",
        nodeId,
        peerNodeId,
        LoggerProvider.nullProvider().getLogger("rlpx"),
        capturedDisconnect::set,
        () -> {
        },
        new LinkedHashMap<>(),
        32,
        "abc",
        10000);
    conn.sendHello();
    conn.messageReceived(
        new RLPxMessage(0, HelloMessage.create(Bytes.EMPTY, 30303, 4, "blah", Collections.emptyList()).toBytes()));

    assertEquals(1, capturedDisconnect.get().messageId());
    DisconnectMessage msg = DisconnectMessage.read(capturedDisconnect.get().content());

    assertEquals(DisconnectReason.NULL_NODE_IDENTITY_RECEIVED.code, msg.reason());
  }

  @Test
  void disconnectIfNodeIDMismatches() {
    AtomicReference<RLPxMessage> capturedDisconnect = new AtomicReference<>();
    WireConnection conn = new WireConnection(
        "abc",
        nodeId,
        peerNodeId,
        LoggerProvider.nullProvider().getLogger("rlpx"),
        capturedDisconnect::set,
        () -> {
        },
        new LinkedHashMap<>(),
        32,
        "abc",
        10000);
    conn.sendHello();
    conn.messageReceived(
        new RLPxMessage(
            0,
            HelloMessage.create(Bytes.of(1, 2, 3, 4), 30303, 3, "blah", Collections.emptyList()).toBytes()));

    assertEquals(1, capturedDisconnect.get().messageId());
    DisconnectMessage msg = DisconnectMessage.read(capturedDisconnect.get().content());

    assertEquals(DisconnectReason.UNEXPECTED_IDENTITY.code, msg.reason());
  }

  @Test
  void disconnectIfConnectedToSelf() {
    AtomicReference<RLPxMessage> capturedDisconnect = new AtomicReference<>();
    WireConnection conn = new WireConnection(
        "abc",
        nodeId,
        nodeId,
        LoggerProvider.nullProvider().getLogger("rlpx"),
        capturedDisconnect::set,
        () -> {
        },
        new LinkedHashMap<>(),
        33,
        "abc",
        10000);
    conn.sendHello();
    conn.messageReceived(
        new RLPxMessage(0, HelloMessage.create(nodeId, 30303, 1, "blah", Collections.emptyList()).toBytes()));

    assertEquals(1, capturedDisconnect.get().messageId());
    DisconnectMessage msg = DisconnectMessage.read(capturedDisconnect.get().content());

    assertEquals(DisconnectReason.CONNECTED_TO_SELF.code, msg.reason());
  }

  @Test
  void disconnectIfInvalidP2PConnection() {
    AtomicReference<RLPxMessage> capturedDisconnect = new AtomicReference<>();
    WireConnection conn = new WireConnection(
        "abc",
        nodeId,
        peerNodeId,
        LoggerProvider.nullProvider().getLogger("rlpx"),
        capturedDisconnect::set,
        () -> {
        },
        new LinkedHashMap<>(),
        5,
        "abc",
        10000);
    conn.sendHello();
    conn.messageReceived(
        new RLPxMessage(0, HelloMessage.create(peerNodeId, 30303, 6, "blah", Collections.emptyList()).toBytes()));

    assertEquals(1, capturedDisconnect.get().messageId());
    DisconnectMessage msg = DisconnectMessage.read(capturedDisconnect.get().content());

    assertEquals(DisconnectReason.INCOMPATIBLE_DEVP2P_VERSION.code, msg.reason());
  }
}
