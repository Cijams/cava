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
package net.consensys.cava.scuttlebutt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.consensys.cava.crypto.sodium.Signature;

import org.junit.jupiter.api.Test;

class InviteTest {

  @Test
  void invalidPort() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Invite("localhost", -1, Identity.random(), Signature.KeyPair.random().secretKey()));
  }

  @Test
  void testToString() {
    Identity identity = Identity.random();
    Signature.SecretKey secretKey = Signature.KeyPair.random().secretKey();

    Invite invite = new Invite("localhost", 8008, identity, secretKey);
    assertEquals(
        "localhost:8008:"
            + identity.publicKeyAsBase64String()
            + "."
            + identity.curveName()
            + "~"
            + secretKey.bytes().toBase64String(),
        invite.toString());
  }

}
