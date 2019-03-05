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
import net.consensys.cava.bytes.Bytes32
import net.consensys.cava.rlp.RLP
import net.consensys.cava.units.bigints.UInt256

/**
 *
 * Inform a peer of the sender's current LES state. This message should be sent after the initial handshake and prior to
 * any LES related messages. The following keys should be present (except the optional ones) in order to be accepted by
 * a LES/1 node: (value types are noted after the key string)
 *
 * @link https://github.com/ethereum/wiki/wiki/Light-client-protocol
 */
internal data class StatusMessage(
  val protocolVersion: Int,
  val networkId: Int,
  val headTd: UInt256,
  val headHash: Bytes32,
  val headNum: UInt256,
  val genesisHash: Bytes32,
  val serveHeaders: Boolean?,
  val serveChainSince: UInt256?,
  val serveStateSince: UInt256?,
  val txRelay: Boolean?,
  val flowControlBufferLimit: UInt256,
  val flowControlMaximumRequestCostTable: UInt256,
  val flowControlMinimumRateOfRecharge: UInt256,
  val announceType: Int
) {

  fun toBytes(): Bytes {
    return RLP.encode { writer ->
      writer.writeList { listWriter ->
        listWriter.writeString("protocolVersion")
        listWriter.writeInt(protocolVersion)
      }
      writer.writeList { listWriter ->
        listWriter.writeString("networkId")
        listWriter.writeInt(networkId)
      }
      writer.writeList { listWriter ->
        listWriter.writeString("headTd")
        listWriter.writeUInt256(headTd)
      }
      writer.writeList { listWriter ->
        listWriter.writeString("headHash")
        listWriter.writeValue(headHash)
      }
      writer.writeList { listWriter ->
        listWriter.writeString("headNum")
        listWriter.writeUInt256(headNum)
      }
      writer.writeList { listWriter ->
        listWriter.writeString("genesisHash")
        listWriter.writeValue(genesisHash)
      }
      if (serveHeaders != null && serveHeaders) {
        writer.writeList { listWriter -> listWriter.writeString("serveHeaders") }
      }
      serveChainSince?.let {
        writer.writeList { listWriter ->
          listWriter.writeString("serveChainSince")
          listWriter.writeUInt256(serveChainSince)
        }
      }
      serveStateSince?.let {
        writer.writeList { listWriter ->
          listWriter.writeString("serveStateSince")
          listWriter.writeUInt256(serveStateSince)
        }
      }
      if (txRelay != null && txRelay) {
        writer.writeList { listWriter -> listWriter.writeString("txRelay") }
      }
      writer.writeList { listWriter ->
        listWriter.writeString("flowControl/BL")
        listWriter.writeUInt256(flowControlBufferLimit)
      }
      writer.writeList { listWriter ->
        listWriter.writeString("flowControl/MRC")
        listWriter.writeUInt256(flowControlMaximumRequestCostTable)
      }
      writer.writeList { listWriter ->
        listWriter.writeString("flowControl/MRR")
        listWriter.writeUInt256(flowControlMinimumRateOfRecharge)
      }
      writer.writeList { listWriter ->
        listWriter.writeString("announceType")
        listWriter.writeInt(announceType)
      }
    }
  }

  companion object {

    /**
     * Reads a status message from bytes, and associates it with a connection ID.
     *
     * @param bytes the bytes of the message
     * @return a new StatusMessage built from the bytes
     */
    fun read(bytes: Bytes): StatusMessage {
      return RLP.decode(bytes) { reader ->
        val parameters = HashMap<String, Any>()
        while (!reader.isComplete) {
          reader.readList<Any> { eltReader ->
            val key = eltReader.readString()

            if ("protocolVersion" == key || "networkId" == key || "announceType" == key) {
              parameters[key] = eltReader.readInt()
            } else if ("headHash" == key || "genesisHash" == key) {
              parameters[key] = Bytes32.wrap(eltReader.readValue())
            } else if ("headTd" == key ||
              "headNum" == key ||
              "serveChainSince" == key ||
              "serveStateSince" == key ||
              "flowControl/BL" == key ||
              "flowControl/MRC" == key ||
              "flowControl/MRR" == key
            ) {
              parameters[key] = eltReader.readUInt256()
            } else if ("serveHeaders" == key || "txRelay" == key) {
              parameters[key] = true
            }
            null
          }
        }

        StatusMessage(
          parameters["protocolVersion"] as Int,
          parameters["networkId"] as Int,
          parameters["headTd"] as UInt256,
          parameters["headHash"] as Bytes32,
          parameters["headNum"] as UInt256,
          parameters["genesisHash"] as Bytes32,
          parameters["serveHeaders"] as Boolean?,
          parameters["serveChainSince"] as UInt256,
          parameters["serveStateSince"] as UInt256,
          parameters["txRelay"] as Boolean?,
          parameters["flowControl/BL"] as UInt256,
          parameters["flowControl/MRC"] as UInt256,
          parameters["flowControl/MRR"] as UInt256,
          parameters["announceType"] as Int
        )
      }
    }
  }
}
