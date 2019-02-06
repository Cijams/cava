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
package net.consensys.cava.ssz;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.cava.bytes.Bytes.fromHexString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.units.bigints.UInt256;

import java.math.BigInteger;
import java.util.Arrays;

import com.google.common.base.Charsets;
import org.junit.jupiter.api.Test;

class BytesSSZWriterTest {

  private static class SomeObject {
    private final String name;
    private final int number;
    private final BigInteger longNumber;

    SomeObject(String name, int number, BigInteger longNumber) {
      this.name = name;
      this.number = number;
      this.longNumber = longNumber;
    }
  }

  @Test
  void shouldWriteFullObjects() {
    SomeObject bob = new SomeObject("Bob", 4, BigInteger.valueOf(1234563434344L));

    Bytes bytes = SSZ.encode(writer -> {
      writer.writeString(bob.name);
      writer.writeInt(bob.number, 8);
      writer.writeBigInteger(bob.longNumber, 256);
    });

    assertTrue(SSZ.<Boolean>decode(bytes, reader -> {
      assertEquals("Bob", reader.readString());
      assertEquals(4, reader.readInt(8));
      assertEquals(BigInteger.valueOf(1234563434344L), reader.readBigInteger(256));
      return true;
    }));
  }

  @Test
  void shouldWriteEmptyStrings() {
    assertEquals(fromHexString("00000000"), SSZ.encode(writer -> writer.writeString("")));
  }

  @Test
  void shouldWriteOneCharactersStrings() {
    assertEquals(fromHexString("0100000064"), SSZ.encode(writer -> writer.writeString("d")));
  }

  @Test
  void shouldWriteStrings() {
    assertEquals(fromHexString("03000000646f67"), SSZ.encode(writer -> writer.writeString("dog")));
  }

  @Test
  void shouldWriteSignedIntegers() {
    assertEquals(fromHexString("00"), SSZ.encode(writer -> writer.writeInt(0, 8)));
    assertEquals(fromHexString("00"), SSZ.encode(writer -> writer.writeInt8(0)));

    assertEquals(fromHexString("0000"), SSZ.encode(writer -> writer.writeInt(0, 16)));
    assertEquals(fromHexString("0000"), SSZ.encode(writer -> writer.writeInt16(0)));

    assertEquals(fromHexString("000000"), SSZ.encode(writer -> writer.writeInt(0, 24)));

    assertEquals(fromHexString("00000000"), SSZ.encode(writer -> writer.writeInt(0, 32)));
    assertEquals(fromHexString("00000000"), SSZ.encode(writer -> writer.writeInt32(0)));

    assertEquals(fromHexString("01"), SSZ.encode(writer -> writer.writeInt(1, 8)));
    assertEquals(fromHexString("01"), SSZ.encode(writer -> writer.writeInt8(1)));

    assertEquals(fromHexString("0100"), SSZ.encode(writer -> writer.writeInt(1, 16)));
    assertEquals(fromHexString("0100"), SSZ.encode(writer -> writer.writeInt16(1)));

    assertEquals(fromHexString("01000000"), SSZ.encode(writer -> writer.writeInt(1, 32)));
    assertEquals(fromHexString("01000000"), SSZ.encode(writer -> writer.writeInt32(1)));

    assertEquals(fromHexString("0f"), SSZ.encode(writer -> writer.writeInt8(15)));
    assertEquals(fromHexString("0f00"), SSZ.encode(writer -> writer.writeInt16(15)));
    assertEquals(fromHexString("E803"), SSZ.encode(writer -> writer.writeInt16(1000)));
    assertEquals(fromHexString("0004"), SSZ.encode(writer -> writer.writeInt16(1024)));
    assertEquals(fromHexString("A08601"), SSZ.encode(writer -> writer.writeInt(100000, 24)));

    assertEquals(fromHexString("FF"), SSZ.encode(writer -> writer.writeInt(-1, 8)));
    assertEquals(fromHexString("FFFF"), SSZ.encode(writer -> writer.writeInt(-1, 16)));
    assertEquals(fromHexString("80"), SSZ.encode(writer -> writer.writeInt(-128, 8)));
    assertEquals(fromHexString("80"), SSZ.encode(writer -> writer.writeInt8(-128)));
    assertEquals(fromHexString("0080"), SSZ.encode(writer -> writer.writeInt(-32768, 16)));
    assertEquals(fromHexString("0080"), SSZ.encode(writer -> writer.writeInt16(-32768)));
  }

  @Test
  void shouldWriteSignedLongs() {
    assertEquals(fromHexString("00"), SSZ.encode(writer -> writer.writeLong(0, 8)));
    assertEquals(fromHexString("01"), SSZ.encode(writer -> writer.writeLong(1, 8)));
    assertEquals(fromHexString("0f"), SSZ.encode(writer -> writer.writeLong(15, 8)));

    assertEquals(fromHexString("e803"), SSZ.encode(writer -> writer.writeLong(1000, 16)));
    assertEquals(fromHexString("0004"), SSZ.encode(writer -> writer.writeLong(1024, 16)));
    assertEquals(fromHexString("A08601"), SSZ.encode(writer -> writer.writeLong(100000L, 24)));
    assertEquals(fromHexString("A0860100"), SSZ.encode(writer -> writer.writeLong(100000L, 32)));
    assertEquals(fromHexString("A086010000000000"), SSZ.encode(writer -> writer.writeLong(100000L, 64)));
    assertEquals(fromHexString("A086010000000000"), SSZ.encode(writer -> writer.writeInt64(100000L)));

    assertEquals(fromHexString("FF"), SSZ.encode(writer -> writer.writeLong(-1, 8)));
    assertEquals(fromHexString("FFFF"), SSZ.encode(writer -> writer.writeLong(-1, 16)));
    assertEquals(fromHexString("80"), SSZ.encode(writer -> writer.writeLong(-128, 8)));
    assertEquals(fromHexString("0080"), SSZ.encode(writer -> writer.writeLong(-32768, 16)));
    assertEquals(fromHexString("0000000000000080"), SSZ.encode(writer -> writer.writeInt64(-9223372036854775808L)));
  }

  @Test
  void shouldWriteSignedBigIntegers() {
    assertEquals(fromHexString("0186A0"), SSZ.encode(writer -> writer.writeBigInteger(BigInteger.valueOf(100000), 24)));
    assertEquals(fromHexString("EB16"), SSZ.encode(writer -> writer.writeBigInteger(BigInteger.valueOf(-5354), 16)));
    assertEquals(fromHexString("8000"), SSZ.encode(writer -> writer.writeBigInteger(BigInteger.valueOf(-32768), 16)));
    assertEquals(
        fromHexString("01F81D7AF1971CEDD9BBA5EFCEE1"),
        SSZ.encode(writer -> writer.writeBigInteger(BigInteger.valueOf(127).pow(16), 112)));
  }

  @Test
  void shouldWriteUnsignedIntegers() {
    assertEquals(fromHexString("00"), SSZ.encode(writer -> writer.writeUInt(0, 8)));
    assertEquals(fromHexString("00"), SSZ.encode(writer -> writer.writeUInt8(0)));

    assertEquals(fromHexString("0000"), SSZ.encode(writer -> writer.writeUInt(0, 16)));
    assertEquals(fromHexString("0000"), SSZ.encode(writer -> writer.writeUInt16(0)));

    assertEquals(fromHexString("000000"), SSZ.encode(writer -> writer.writeUInt(0, 24)));

    assertEquals(fromHexString("00000000"), SSZ.encode(writer -> writer.writeUInt(0, 32)));
    assertEquals(fromHexString("00000000"), SSZ.encode(writer -> writer.writeUInt32(0)));

    assertEquals(fromHexString("01"), SSZ.encode(writer -> writer.writeUInt(1, 8)));
    assertEquals(fromHexString("01"), SSZ.encode(writer -> writer.writeUInt8(1)));

    assertEquals(fromHexString("0100"), SSZ.encode(writer -> writer.writeUInt(1, 16)));
    assertEquals(fromHexString("0100"), SSZ.encode(writer -> writer.writeUInt16(1)));

    assertEquals(fromHexString("01000000"), SSZ.encode(writer -> writer.writeUInt(1, 32)));
    assertEquals(fromHexString("01000000"), SSZ.encode(writer -> writer.writeUInt32(1)));

    assertEquals(fromHexString("0f"), SSZ.encode(writer -> writer.writeUInt8(15)));
    assertEquals(fromHexString("0f00"), SSZ.encode(writer -> writer.writeUInt16(15)));
    assertEquals(fromHexString("e803"), SSZ.encode(writer -> writer.writeUInt16(1000)));
    assertEquals(fromHexString("0004"), SSZ.encode(writer -> writer.writeUInt16(1024)));
    assertEquals(fromHexString("A08601"), SSZ.encode(writer -> writer.writeUInt(100000, 24)));

    assertEquals(fromHexString("FF"), SSZ.encode(writer -> writer.writeUInt(255, 8)));
    assertEquals(fromHexString("FFFF"), SSZ.encode(writer -> writer.writeUInt(65535, 16)));
    assertEquals(fromHexString("80"), SSZ.encode(writer -> writer.writeUInt(128, 8)));
    assertEquals(fromHexString("80"), SSZ.encode(writer -> writer.writeUInt8(128)));
    assertEquals(fromHexString("0080"), SSZ.encode(writer -> writer.writeUInt(32768, 16)));
    assertEquals(fromHexString("0080"), SSZ.encode(writer -> writer.writeUInt16(32768)));
  }

  @Test
  void shouldWriteUnsignedLongs() {
    assertEquals(fromHexString("00"), SSZ.encode(writer -> writer.writeULong(0, 8)));
    assertEquals(fromHexString("01"), SSZ.encode(writer -> writer.writeULong(1, 8)));
    assertEquals(fromHexString("0f"), SSZ.encode(writer -> writer.writeULong(15, 8)));

    assertEquals(fromHexString("e803"), SSZ.encode(writer -> writer.writeULong(1000, 16)));
    assertEquals(fromHexString("0004"), SSZ.encode(writer -> writer.writeULong(1024, 16)));
    assertEquals(fromHexString("A08601"), SSZ.encode(writer -> writer.writeULong(100000L, 24)));
    assertEquals(fromHexString("A0860100"), SSZ.encode(writer -> writer.writeULong(100000L, 32)));
    assertEquals(fromHexString("A086010000000000"), SSZ.encode(writer -> writer.writeULong(100000L, 64)));
    assertEquals(fromHexString("A086010000000000"), SSZ.encode(writer -> writer.writeUInt64(100000L)));

    assertEquals(fromHexString("FF"), SSZ.encode(writer -> writer.writeULong(255, 8)));
    assertEquals(fromHexString("FFFF"), SSZ.encode(writer -> writer.writeULong(65535, 16)));
    assertEquals(fromHexString("80"), SSZ.encode(writer -> writer.writeULong(128, 8)));
    assertEquals(fromHexString("0080"), SSZ.encode(writer -> writer.writeULong(32768, 16)));
    assertEquals(fromHexString("0000000000000080"), SSZ.encode(writer -> {
      writer.writeUInt64(Long.parseUnsignedLong("9223372036854775808"));
    }));
  }

  @Test
  void shouldWriteUInt256Integers() {
    assertEquals(
        fromHexString("0000000000000000000000000000000000000000000000000000000000000000"),
        SSZ.encode(writer -> writer.writeUInt256(UInt256.valueOf(0L))));
    assertEquals(
        fromHexString("A086010000000000000000000000000000000000000000000000000000000000"),
        SSZ.encode(writer -> writer.writeUInt256(UInt256.valueOf(100000L))));
    assertEquals(
        fromHexString("AB00000000F10000000000000000000000000000000000000000000000000004"),
        SSZ.encode(
            writer -> writer.writeUInt256(
                UInt256.fromHexString("0x0400000000000000000000000000000000000000000000000000f100000000ab"))));
  }

  @Test
  void shouldWriteBooleans() {
    assertEquals(fromHexString("00"), SSZ.encode(writer -> writer.writeBoolean(false)));
    assertEquals(fromHexString("01"), SSZ.encode(writer -> writer.writeBoolean(true)));
  }

  @Test
  void shouldWriteAddresses() {
    assertEquals(
        fromHexString("8EE1CEEFA5BBD9ED1C978EE1CEEFA5BBD9ED1C97"),
        SSZ.encode(writer -> writer.writeAddress(Bytes.fromHexString("8EE1CEEFA5BBD9ED1C978EE1CEEFA5BBD9ED1C97"))));
    assertThrows(
        IllegalArgumentException.class,
        () -> SSZ.encode(writer -> writer.writeAddress(Bytes.fromHexString("beef"))));
  }

  @Test
  void shouldWriteHashes() {
    assertEquals(
        fromHexString("ED1C978EE1CEEFA5BBD9ED1C8EE1CEEFA5BBD9ED1C978EE1CEEFA5BBD9ED1C97"),
        SSZ.encode(
            writer -> writer
                .writeHash(Bytes32.fromHexString("ED1C978EE1CEEFA5BBD9ED1C8EE1CEEFA5BBD9ED1C978EE1CEEFA5BBD9ED1C97"))));
    assertThrows(
        IllegalArgumentException.class,
        () -> SSZ.encode(writer -> writer.writeAddress(Bytes.fromHexString("beef"))));
  }

  @Test
  void shouldWriteVarargsListsOfInts() {
    assertEquals(fromHexString("03000000030405"), SSZ.encodeIntList(8, 3, 4, 5));
  }

  @Test
  void shouldWriteUtilListsOfInts() {
    assertEquals(fromHexString("03000000030405"), SSZ.encodeIntList(8, Arrays.asList(3, 4, 5)));
  }

  @Test
  void shouldWriteVarargsListsOfLongInts() {
    assertEquals(fromHexString("03000000030405"), SSZ.encodeLongIntList(8, 3, 4, 5));
  }

  @Test
  void shouldWriteUtilListsOfLongInts() {
    assertEquals(
        fromHexString("03000000030405"),
        SSZ.encodeLongIntList(8, Arrays.asList((long) 3, (long) 4, (long) 5)));
  }

  @Test
  void shouldWriteVarargsListsOfBigIntegers() {
    assertEquals(
        fromHexString("03000000030405"),
        SSZ.encodeBigIntegerList(8, BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(5)));
  }

  @Test
  void shouldWriteUtilListsOfBigIntegers() {
    assertEquals(
        fromHexString("03000000030405"),
        SSZ.encodeBigIntegerList(
            8,
            Arrays.asList(BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(5))));
  }

  @Test
  void shouldWriteVarargsListsOfUnsignedInts() {
    assertEquals(fromHexString("03000000FDFEFF"), SSZ.encodeUIntList(8, 253, 254, 255));
  }

  @Test
  void shouldWriteUtilListsOfUnsignedInts() {
    assertEquals(fromHexString("03000000FDFEFF"), SSZ.encodeUIntList(8, Arrays.asList(253, 254, 255)));
  }

  @Test
  void shouldWriteVarargsListsOfUnsignedLongs() {
    assertEquals(fromHexString("03000000FDFEFF"), SSZ.encodeULongIntList(8, 253, 254, 255));
  }

  @Test
  void shouldWriteUtilListsOfUnsignedLongs() {
    assertEquals(
        fromHexString("03000000FDFEFF"),
        SSZ.encodeULongIntList(8, Arrays.asList((long) 253, (long) 254, (long) 255)));
  }

  @Test
  void shouldWriteVaragsListsOfStrings() {
    assertEquals(
        fromHexString("1800000003000000626F62040000006A616E65050000006A616E6574"),
        SSZ.encodeStringList("bob", "jane", "janet"));
  }

  @Test
  void shouldWriteUtilListsOfStrings() {
    assertEquals(
        fromHexString("1800000003000000626F62040000006A616E65050000006A616E6574"),
        SSZ.encodeStringList(Arrays.asList("bob", "jane", "janet")));
  }

  @Test
  void shouldWriteVarargsListsOfBytes() {
    assertEquals(
        fromHexString("1800000003000000626F62040000006A616E65050000006A616E6574"),
        SSZ.encodeBytesList(
            Bytes.wrap("bob".getBytes(Charsets.UTF_8)),
            Bytes.wrap("jane".getBytes(Charsets.UTF_8)),
            Bytes.wrap("janet".getBytes(Charsets.UTF_8))));
  }

  @Test
  void shouldWriteUtilListOfBytes() {
    assertEquals(
        fromHexString("1800000003000000626F62040000006A616E65050000006A616E6574"),
        SSZ.encodeBytesList(
            Arrays.asList(
                Bytes.wrap("bob".getBytes(Charsets.UTF_8)),
                Bytes.wrap("jane".getBytes(Charsets.UTF_8)),
                Bytes.wrap("janet".getBytes(Charsets.UTF_8)))));
  }

  @Test
  void shouldWritePreviouslyEncodedValues() {
    Bytes output = SSZ.encode(writer -> writer.writeSSZ(SSZ.encodeByteArray("abc".getBytes(UTF_8))));
    assertEquals("abc", SSZ.decodeString(output));
  }
}
