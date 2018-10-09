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
package net.consensys.cava.eth.reference;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.SECP256K1.Signature;
import net.consensys.cava.eth.Address;
import net.consensys.cava.eth.Block;
import net.consensys.cava.eth.BlockBody;
import net.consensys.cava.eth.BlockHeader;
import net.consensys.cava.eth.Hash;
import net.consensys.cava.eth.Transaction;
import net.consensys.cava.io.Resources;
import net.consensys.cava.junit.BouncyCastleExtension;
import net.consensys.cava.units.bigints.UInt256;
import net.consensys.cava.units.ethereum.Gas;
import net.consensys.cava.units.ethereum.Wei;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.errorprone.annotations.MustBeClosed;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(BouncyCastleExtension.class)
class BlockRLPTestSuite {

  private static ObjectMapper mapper = new ObjectMapper();

  @ParameterizedTest(name = "{index}. block {0}[{1}]")
  @MethodSource("readBlockChainTests")
  void testBlockRLP(String name, long blockIndex, Block block, String rlp, String hash) {
    Block rlpBlock = Block.fromHexString(rlp);
    assertEquals(block, rlpBlock);
    assertEquals(Bytes.fromHexString(rlp), block.toBytes());
    assertEquals(Hash.fromHexString(hash), block.header().hash());
    assertEquals(Hash.fromHexString(hash), rlpBlock.header().hash());
  }

  @MustBeClosed
  private static Stream<Arguments> readBlockChainTests() throws IOException {
    return Resources.find("**/BlockchainTests/**/*.json").flatMap(url -> {
      try (InputStream is = url.openConnection().getInputStream()) {
        return readTestCase(is);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Stream<Arguments> readTestCase(InputStream is) throws IOException {
    Map<String, Map> test = mapper.readerFor(Map.class).readValue(is);
    String name = test.keySet().iterator().next();
    Map testData = test.get(name);
    List<Map> blocks = (List<Map>) testData.get("blocks");
    return IntStream.range(0, blocks.size()).boxed().filter(i -> blocks.get(i).containsKey("blockHeader")).map(i -> {
      Map block = blocks.get(i);
      return Arguments.of(name, i, createBlock(block), block.get("rlp"), ((Map) block.get("blockHeader")).get("hash"));
    });
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static BlockHeader createBlockHeader(Map headerData) {
    checkNotNull(headerData, headerData.toString());
    return new BlockHeader(
        Hash.fromHexString((String) headerData.get("parentHash")),
        Hash.fromHexString((String) headerData.get("uncleHash")),
        Address.fromHexString((String) headerData.get("coinbase")),
        Hash.fromHexString((String) headerData.get("stateRoot")),
        Hash.fromHexString((String) headerData.get("transactionsTrie")),
        Hash.fromHexString((String) headerData.get("receiptTrie")),
        Bytes.fromHexString((String) headerData.get("bloom")),
        UInt256.fromHexString((String) headerData.get("difficulty")),
        UInt256.fromHexString((String) headerData.get("number")),
        Gas.valueOf(UInt256.fromHexString((String) headerData.get("gasLimit"))),
        Gas.valueOf(UInt256.fromHexString((String) headerData.get("gasUsed"))),
        Instant.ofEpochSecond(Bytes.fromHexString((String) headerData.get("timestamp")).toLong()),
        Bytes.fromHexString((String) headerData.get("extraData")),
        Hash.fromHexString((String) headerData.get("mixHash")),
        Bytes.fromHexString((String) headerData.get("nonce")));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Block createBlock(Map blockData) {
    Map headerData = (Map) blockData.get("blockHeader");
    BlockHeader header = createBlockHeader(headerData);
    List<Transaction> transactions = new ArrayList<>();
    for (Object txDataObj : (List) blockData.get("transactions")) {
      Map txData = (Map) txDataObj;

      Address toAddress = null;
      String toAddressString = (String) txData.get("to");
      if (toAddressString != null) {
        Bytes toAddressBytes = Bytes.fromHexString(toAddressString);
        toAddress = toAddressBytes.isEmpty() ? null : Address.fromBytes(toAddressBytes);
      }

      transactions.add(
          new Transaction(
              UInt256.fromHexString((String) txData.get("nonce")),
              Wei.valueOf(UInt256.fromHexString((String) txData.get("gasPrice"))),
              Gas.valueOf(UInt256.fromHexString((String) txData.get("gasLimit"))),
              toAddress,
              Wei.valueOf(UInt256.fromHexString((String) txData.get("value"))),
              Bytes.fromHexString((String) txData.get("data")),
              Signature.create(
                  (byte) ((int) Bytes.fromHexString((String) txData.get("v")).get(0) - 27),
                  Bytes.fromHexString((String) txData.get("r")).toUnsignedBigInteger(),
                  Bytes.fromHexString((String) txData.get("s")).toUnsignedBigInteger())));
    }
    List<BlockHeader> ommers = new ArrayList<>();
    for (Object ommerDataObj : (List) blockData.get("uncleHeaders")) {
      ommers.add(createBlockHeader((Map) ommerDataObj));
    }

    return new Block(header, new BlockBody(transactions, ommers));
  }
}
