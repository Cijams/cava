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
package net.consensys.cava.eth.repository

import kotlinx.coroutines.runBlocking
import net.consensys.cava.bytes.Bytes
import net.consensys.cava.bytes.Bytes32
import net.consensys.cava.crypto.SECP256K1
import net.consensys.cava.eth.Address
import net.consensys.cava.eth.Block
import net.consensys.cava.eth.BlockBody
import net.consensys.cava.eth.BlockHeader
import net.consensys.cava.eth.Hash
import net.consensys.cava.eth.Transaction
import net.consensys.cava.junit.BouncyCastleExtension
import net.consensys.cava.junit.LuceneIndexWriter
import net.consensys.cava.junit.LuceneIndexWriterExtension
import net.consensys.cava.kv.MapKeyValueStore
import net.consensys.cava.units.bigints.UInt256
import net.consensys.cava.units.ethereum.Gas
import net.consensys.cava.units.ethereum.Wei
import org.apache.lucene.index.IndexWriter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(BouncyCastleExtension::class, LuceneIndexWriterExtension::class)
internal class BlockchainRepositoryTest {

  @Test
  @Throws(Exception::class)
  fun storeAndRetrieveBlock(@LuceneIndexWriter writer: IndexWriter) = runBlocking {
    val genesisHeader = BlockHeader(
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.fromBytes(Bytes32.random()),
      UInt256.fromBytes(Bytes32.random()),
      Gas.valueOf(3000),
      Gas.valueOf(2000),
      Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random()
    )
    val genesisBlock = Block(genesisHeader, BlockBody(emptyList(), emptyList()))
    val repo = BlockchainRepository
      .init(
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        BlockchainIndex(writer),
        genesisBlock
      )
    val header = BlockHeader(
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.fromBytes(Bytes32.random()),
      UInt256.fromBytes(Bytes32.random()),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random()
    )
    val body = BlockBody(
      listOf(
        Transaction(
          UInt256.valueOf(1),
          Wei.valueOf(2),
          Gas.valueOf(2),
          Address.fromBytes(Bytes.random(20)),
          Wei.valueOf(2),
          Bytes.random(12),
          SECP256K1.KeyPair.random()
        )
      ),
      emptyList()
    )
    val block = Block(header, body)
    repo.storeBlock(block)
    val read = repo.retrieveBlock(block.header().hash().toBytes())
    assertEquals(block, read)
    assertEquals(block.header(), repo.retrieveBlockHeader(block.header().hash()))
  }

  @Test
  @Throws(Exception::class)
  fun storeChainHead(@LuceneIndexWriter writer: IndexWriter) = runBlocking {
    val genesisHeader = BlockHeader(
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.fromBytes(Bytes32.random()),
      UInt256.fromBytes(Bytes32.random()),
      Gas.valueOf(3000),
      Gas.valueOf(2000),
      Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random()
    )
    val genesisBlock = Block(genesisHeader, BlockBody(emptyList(), emptyList()))
    val repo = BlockchainRepository
      .init(
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        BlockchainIndex(writer),
        genesisBlock
      )

    val header = BlockHeader(
      genesisHeader.hash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.fromBytes(Bytes32.random()),
      genesisHeader.number().add(UInt256.valueOf(1)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random()
    )
    val biggerNumber = BlockHeader(
      header.hash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.fromBytes(Bytes32.random()),
      header.number().add(UInt256.valueOf(1)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random()
    )
    val biggerNumber2 = BlockHeader(
      biggerNumber.hash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.fromBytes(Bytes32.random()),
      header.number().add(UInt256.valueOf(2)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random()
    )
    val biggerNumber3 = BlockHeader(
      biggerNumber2.hash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.fromBytes(Bytes32.random()),
      header.number().add(UInt256.valueOf(3)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random()
    )

    repo.storeBlockHeader(header)
    repo.storeBlockHeader(biggerNumber)
    repo.storeBlockHeader(biggerNumber2)
    repo.storeBlockHeader(biggerNumber3)

    assertEquals(biggerNumber3.hash(), repo.retrieveChainHeadHeader()!!.hash())
  }

  @Test
  @Throws(Exception::class)
  fun storeChainHeadBlocks(@LuceneIndexWriter writer: IndexWriter) = runBlocking {
    val genesisHeader = BlockHeader(
      null,
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(0),
      UInt256.fromBytes(Bytes32.random()),
      Gas.valueOf(3000),
      Gas.valueOf(2000),
      Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random()
    )
    val genesisBlock = Block(genesisHeader, BlockBody(emptyList(), emptyList()))
    val repo = BlockchainRepository.init(
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        BlockchainIndex(writer),
        genesisBlock
      )

    val header = BlockHeader(
      genesisHeader.hash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(1),
      genesisHeader.number().add(UInt256.valueOf(1)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random()
    )
    val biggerNumber = BlockHeader(
      header.hash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(2),
      header.number().add(UInt256.valueOf(1)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random()
    )
    val biggerNumber2 = BlockHeader(
      biggerNumber.hash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(3),
      header.number().add(UInt256.valueOf(2)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random()
    )
    val biggerNumber3 = BlockHeader(
      biggerNumber2.hash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(4),
      header.number().add(UInt256.valueOf(3)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random()
    )

    repo.storeBlock(Block(header, BlockBody(emptyList(), emptyList())))
    repo.storeBlock(Block(biggerNumber, BlockBody(emptyList(), emptyList())))
    repo.storeBlock(Block(biggerNumber2, BlockBody(emptyList(), emptyList())))
    repo.storeBlock(Block(biggerNumber3, BlockBody(emptyList(), emptyList())))

    assertEquals(biggerNumber3.hash(), repo.retrieveChainHeadHeader()!!.hash())
  }

  @Test
  fun StoreChainHeadDifferentOrder(@LuceneIndexWriter writer: IndexWriter) = runBlocking {
    val genesisHeader = BlockHeader(
      null,
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(0),
      UInt256.fromBytes(Bytes32.random()),
      Gas.valueOf(3000),
      Gas.valueOf(2000),
      Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random()
    )
    val genesisBlock = Block(genesisHeader, BlockBody(emptyList(), emptyList()))
    val repo = BlockchainRepository.init(
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      BlockchainIndex(writer),
      genesisBlock
    )

    val header = BlockHeader(
      genesisHeader.hash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(1),
      genesisHeader.number().add(UInt256.valueOf(1)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random()
    )
    val biggerNumber = BlockHeader(
      header.hash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(2),
      header.number().add(UInt256.valueOf(1)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random()
    )
    val biggerNumber2 = BlockHeader(
      biggerNumber.hash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(3),
      header.number().add(UInt256.valueOf(2)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random()
    )
    val biggerNumber3 = BlockHeader(
      biggerNumber2.hash(),
      Hash.fromBytes(Bytes32.random()),
      Address.fromBytes(Bytes.random(20)),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random(),
      UInt256.valueOf(4),
      header.number().add(UInt256.valueOf(3)),
      Gas.valueOf(3),
      Gas.valueOf(2),
      Instant.now().truncatedTo(ChronoUnit.SECONDS),
      Bytes.of(2, 3, 4),
      Hash.fromBytes(Bytes32.random()),
      Bytes32.random()
    )

    repo.storeBlock(Block(biggerNumber3, BlockBody(emptyList(), emptyList())))
    repo.storeBlock(Block(biggerNumber2, BlockBody(emptyList(), emptyList())))
    repo.storeBlock(Block(biggerNumber, BlockBody(emptyList(), emptyList())))
    repo.storeBlock(Block(header, BlockBody(emptyList(), emptyList())))

    assertEquals(biggerNumber3.hash(), repo.retrieveChainHeadHeader()!!.hash())
  }
}
