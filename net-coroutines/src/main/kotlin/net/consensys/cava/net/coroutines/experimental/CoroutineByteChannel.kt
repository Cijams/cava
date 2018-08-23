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
package net.consensys.cava.net.coroutines.experimental

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.ClosedChannelException
import java.nio.channels.GatheringByteChannel
import java.nio.channels.NonReadableChannelException
import java.nio.channels.NonWritableChannelException
import java.nio.channels.ReadableByteChannel
import java.nio.channels.ScatteringByteChannel
import java.nio.channels.SelectableChannel
import java.nio.channels.SelectionKey
import java.nio.channels.WritableByteChannel

interface ReadableCoroutineByteChannel {
  /**
   * Reads a sequence of bytes from this channel into the given buffer.
   *
   * An attempt is made to read up to r bytes from the channel, where r is the number of bytes remaining in the buffer,
   * that is, dst.remaining(), at the moment this method is invoked.
   *
   * @param dst The buffer into which bytes are to be transferred.
   * @return The number of bytes read, possibly zero, or `-1` if the channel has reached end-of-stream.
   * @throws NonReadableChannelException If this channel was not opened for reading.
   * @throws ClosedChannelException If the channel is closed.
   * @throws AsynchronousCloseException If another thread closes this channel while the read operation is in progress.
   * @throws ClosedByInterruptException If another thread interrupts the current thread while the read operation is
   *   in progress, thereby closing the channel and setting the current thread's interrupt status.
   * @throws IOException If some other I/O error occurs.
   */
  suspend fun read(dst: ByteBuffer): Int
}

internal class ReadableCoroutineByteChannelMixin<T>(
  private val channel: T,
  private val selector: CoroutineSelector
) : ReadableCoroutineByteChannel
  where T : SelectableChannel,
        T : ReadableByteChannel {

  override suspend fun read(dst: ByteBuffer): Int {
    while (true) {
      val n = channel.read(dst)
      if (n != 0 || dst.remaining() == 0) {
        return n
      }
      // slow path
      selector.select(channel, SelectionKey.OP_READ)
    }
  }
}

interface WritableCoroutineByteChannel {

  /**
   * Writes a sequence of bytes to this channel from the given buffer.
   *
   * @param src The buffer from which bytes are to be retrieved.
   * @return The number of bytes written, possibly zero.
   * @throws NonWritableChannelException If this channel was not opened for writing.
   * @throws ClosedChannelException If the channel is closed.
   * @throws AsynchronousCloseException If another thread closes this channel while the write operation is in progress.
   * @throws ClosedByInterruptException If another thread interrupts the current thread while the write operation is
   *   in progress, thereby closing the channel and setting the current thread's interrupt status.
   * @throws IOException If some other I/O error occurs.
   */
  suspend fun write(src: ByteBuffer): Int
}

internal class WritableCoroutineByteChannelMixin<T>(
  private val channel: T,
  private val selector: CoroutineSelector
) : WritableCoroutineByteChannel
  where T : SelectableChannel,
        T : WritableByteChannel {

  override suspend fun write(src: ByteBuffer): Int {
    while (true) {
      val n = channel.write(src)
      if (n != 0 || src.remaining() == 0) {
        return n
      }
      // slow path
      selector.select(channel, SelectionKey.OP_WRITE)
    }
  }
}

interface CoroutineByteChannel : ReadableCoroutineByteChannel, WritableCoroutineByteChannel

internal class CoroutineByteChannelMixin<T>(
  private val channel: T,
  private val selector: CoroutineSelector
) : CoroutineByteChannel,
  ReadableCoroutineByteChannel by ReadableCoroutineByteChannelMixin(channel, selector),
  WritableCoroutineByteChannel by WritableCoroutineByteChannelMixin(channel, selector)
  where T : SelectableChannel,
        T : ReadableByteChannel,
        T : WritableByteChannel

/**
 * A channel that can read bytes into a sequence of buffers.
 */
interface ScatteringCoroutineByteChannel : ReadableCoroutineByteChannel {
  /**
   * Reads a sequence of bytes from this channel into a subsequence of the given buffers.
   *
   * @param dsts The buffers into which bytes are to be transferred.
   * @param offset The offset within the buffer array of the first buffer into which bytes are to be transferred;
   *   must be non-negative and no larger than `dsts.length`.
   * @param length The maximum number of buffers to be accessed; must be non-negative and no larger than
   *   `dsts.length - offset`.
   * @return The number of bytes read, possibly zero, or `-1` if the channel has reached end-of-stream.
   * @throws IndexOutOfBoundsException If the preconditions on the offset and length parameters do not hold.
   * @throws NonReadableChannelException If this channel was not opened for reading.
   * @throws ClosedChannelException If the channel is closed.
   * @throws AsynchronousCloseException If another thread closes this channel while the read operation is in progress.
   * @throws ClosedByInterruptException If another thread interrupts the current thread while the read operation is
   *   in progress, thereby closing the channel and setting the current thread's interrupt status.
   * @throws IOException If some other I/O error occurs.
   */
  suspend fun read(dsts: Array<ByteBuffer>, offset: Int = 0, length: Int = dsts.size): Long
}

internal class ScatteringCoroutineByteChannelMixin<T>(
  private val channel: T,
  private val selector: CoroutineSelector
) : ScatteringCoroutineByteChannel,
  ReadableCoroutineByteChannel by ReadableCoroutineByteChannelMixin(channel, selector)
  where T : SelectableChannel,
        T : ScatteringByteChannel {

  override suspend fun read(dsts: Array<ByteBuffer>, offset: Int, length: Int): Long {
    while (true) {
      val n = channel.read(dsts, offset, length)
      if (n != 0L || buffersAreEmpty(dsts, offset, length)) {
        return n
      }
      // slow path
      selector.select(channel, SelectionKey.OP_READ)
    }
  }
}

/**
 * A channel that can write bytes from a sequence of buffers.
 */
interface GatheringCoroutineByteChannel : WritableCoroutineByteChannel {
  /**
   * Writes a sequence of bytes to this channel from a subsequence of the given buffers.
   *
   * @param srcs The buffers from which bytes are to be retrieved.
   * @param offset The offset within the buffer array of the first buffer from which bytes are to be retrieved;
   *   must be non-negative and no larger than `srcs.length`.
   * @param length The maximum number of buffers to be accessed; must be non-negative and no larger than
   *   `srcs.length - offset`.
   * @return The number of bytes written, possibly zero.
   * @throws IndexOutOfBoundsException If the preconditions on the offset and length parameters do not hold.
   * @throws NonWritableChannelException If this channel was not opened for writing.
   * @throws ClosedChannelException If the channel is closed.
   * @throws AsynchronousCloseException If another thread closes this channel while the write operation is in progress.
   * @throws ClosedByInterruptException If another thread interrupts the current thread while the write operation is
   *   in progress, thereby closing the channel and setting the current thread's interrupt status.
   * @throws IOException If some other I/O error occurs.
   */
  suspend fun write(srcs: Array<ByteBuffer>, offset: Int = 0, length: Int = srcs.size): Long
}

internal class GatheringCoroutineByteChannelMixin<T>(
  private val channel: T,
  private val selector: CoroutineSelector
) : GatheringCoroutineByteChannel,
  WritableCoroutineByteChannel by WritableCoroutineByteChannelMixin(channel, selector)
  where T : SelectableChannel,
        T : GatheringByteChannel {

  override suspend fun write(srcs: Array<ByteBuffer>, offset: Int, length: Int): Long {
    while (true) {
      val n = channel.write(srcs, offset, length)
      if (n != 0L || buffersAreEmpty(srcs, offset, length)) {
        return n
      }
      // slow path
      selector.select(channel, SelectionKey.OP_WRITE)
    }
  }
}

private fun buffersAreEmpty(buffers: Array<ByteBuffer>, offset: Int, length: Int): Boolean {
  while (offset < length) {
    if (buffers[offset].remaining() != 0) {
      return false
    }
  }
  return true
}
