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

import net.consensys.cava.crypto.SECP256K1
import net.consensys.cava.rlp.RLPReader
import net.consensys.cava.rlp.RLPWriter

internal data class Node(
  val endpoint: Endpoint,
  val nodeId: SECP256K1.PublicKey
) {

  companion object {
    fun readFrom(reader: RLPReader): Node {
      val endpoint = Endpoint.readFrom(reader)
      val nodeId = SECP256K1.PublicKey.fromBytes(reader.readValue())
      return Node(endpoint, nodeId)
    }
  }

  internal fun writeTo(writer: RLPWriter) {
    endpoint.writeTo(writer)
    writer.writeValue(nodeId.bytes())
  }

  internal fun rlpSize(): Int = 1 + endpoint.rlpSize() + 3 + 64
}

internal fun Peer.toNode(): Node =
  endpoint?.let { endpoint -> Node(endpoint, nodeId) } ?: throw IllegalArgumentException("Peer has no endpoint")
