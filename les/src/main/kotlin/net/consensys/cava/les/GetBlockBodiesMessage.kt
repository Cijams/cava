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
package net.consensys.cava.les

import net.consensys.cava.bytes.Bytes
import net.consensys.cava.eth.Hash
import net.consensys.cava.rlp.RLP

internal data class GetBlockBodiesMessage(val reqID: Long, val blockHashes: List<Hash>) {

  fun toBytes(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeLong(reqID)
      writer.writeList(blockHashes) { eltWriter, hash -> eltWriter.writeValue(hash.toBytes()) }
    }
  }

  companion object {

    fun read(bytes: Bytes): GetBlockBodiesMessage {
      return RLP.decodeList(
        bytes
      ) { reader ->
        GetBlockBodiesMessage(
          reader.readLong(),
          reader.readListContents { elementReader -> Hash.fromBytes(elementReader.readValue()) })
      }
    }
  }
}
