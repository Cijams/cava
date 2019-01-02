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
package net.consensys.cava.devp2p

import net.consensys.cava.rlp.RLP
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class EndpointTest {

  @Test
  fun shouldHaveExpectedMinimumSize() {
    val endpoint1 = Endpoint("127.0.0.1", 65535, 65535)
    val bytes1 = RLP.encode { r -> endpoint1.writeTo(r) }
    assertEquals(endpoint1.rlpSize(), bytes1.size())

    val endpoint2 = Endpoint("2001:4860:4860::8888", 65535, 65535)
    val bytes2 = RLP.encode { r -> endpoint2.writeTo(r) }
    assertEquals(endpoint2.rlpSize(), bytes2.size())
  }

  @Test
  fun endpointsWithSameHostAndPortsAreEqual() {
    val endpoint1 = Endpoint("127.0.0.1", 7654, 8765)
    val endpoint2 = Endpoint("127.0.0.1", 7654, 8765)
    assertEquals(endpoint1, endpoint2)

    val endpoint3 = Endpoint("127.0.0.1", 7654, null)
    val endpoint4 = Endpoint("127.0.0.1", 7654, null)
    assertEquals(endpoint3, endpoint4)
  }

  @Test
  fun endpointsWithDifferentHostsAreNotEqual() {
    val endpoint1 = Endpoint("127.0.0.1", 7654, 8765)
    val endpoint2 = Endpoint("127.0.0.2", 7654, 8765)
    assertNotEquals(endpoint1, endpoint2)
  }

  @Test
  fun endpointsWithDifferentUDPPortsAreNotEqual() {
    val endpoint1 = Endpoint("127.0.0.1", 7654, 8765)
    val endpoint2 = Endpoint("127.0.0.1", 7655, 8765)
    assertNotEquals(endpoint1, endpoint2)
  }

  @Test
  fun endpointsWithDifferentTCPPortsAreNotEqual() {
    val endpoint1 = Endpoint("127.0.0.1", 7654, 8765)
    val endpoint2 = Endpoint("127.0.0.1", 7654, 8766)
    assertNotEquals(endpoint1, endpoint2)

    val endpoint3 = Endpoint("127.0.0.1", 7654, null)
    assertNotEquals(endpoint1, endpoint3)
  }

  @Test
  fun invalidUDPPortThrowsIllegalArgument() {
    assertThrows<IllegalArgumentException> { Endpoint("127.0.0.1", 76543321, 8765) }
    assertThrows<IllegalArgumentException> { Endpoint("127.0.0.1", 0, 8765) }
  }

  @Test
  fun invalidTCPPortThrowsIllegalArgument() {
    assertThrows<IllegalArgumentException> { Endpoint("127.0.0.1", 7654, 87654321) }
    assertThrows<IllegalArgumentException> { Endpoint("127.0.0.1", 7654, 0) }
  }

  @Test
  fun shouldEncodeThenDecode() {
    val endpoint1 = Endpoint("127.0.0.1", 7654, 8765)
    val encoding1 = RLP.encode { writer -> endpoint1.writeTo(writer) }

    val endpoint2: Endpoint = RLP.decode(encoding1) { reader -> Endpoint.readFrom(reader) }
    assertEquals(endpoint1, endpoint2)

    val endpoint3 = Endpoint("127.0.0.1", 7654, null)
    val encoding2 = RLP.encode { writer -> endpoint3.writeTo(writer) }

    val endpoint4: Endpoint = RLP.decode(encoding2) { reader -> Endpoint.readFrom(reader) }
    assertEquals(endpoint3, endpoint4)
  }
}
