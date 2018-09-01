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
import java.net.SocketAddress
import java.net.SocketOption
import java.nio.channels.AlreadyBoundException
import java.nio.channels.ClosedChannelException
import java.nio.channels.NetworkChannel
import java.nio.channels.UnsupportedAddressTypeException

/**
 * A co-routine based network channel.
 */
interface CoroutineNetworkChannel : NetworkChannel {

  /**
   * Indicates if this channel is open.
   *
   * @return `true` if this channel is open.
   */
  override fun isOpen(): Boolean

  /**
   * Closes this channel.
   *
   * After a channel is closed, any further attempt to invoke I/O operations upon it will cause a
   * [ClosedChannelException] to be thrown.
   *
   * @throws IOException If an I/O error occurs.
   */
  override fun close()

  /**
   * Binds the channel's socket to a local address.
   *
   * This method is used to establish an association between the socket and a local address. Once an association is
   * established then the socket remains bound until the channel is closed. If the `local` parameter has the value
   * `null` then the socket will be bound to an address that is assigned automatically.
   *
   * @param local The address to bind the socket, or `null` to bind the socket to an automatically assigned socket
   *         address.
   * @return This channel.
   * @throws AlreadyBoundException If the socket is already bound.
   * @throws UnsupportedAddressTypeException If the type of the given address is not supported.
   * @throws ClosedChannelException If the channel is closed.
   * @throws IOException If some other I/O error occurs.
   * @throws SecurityException If a security manager is installed and it denies an unspecified permission. An
   *         implementation of this interface should specify any required permissions.
   */
  override fun bind(local: SocketAddress?): CoroutineNetworkChannel

  /**
   * Returns the socket address that this channel's socket is bound to.
   *
   * @return The socket address that the socket is bound to, or `null` if the channel's socket is not bound.
   *
   * @throws ClosedChannelException If the channel is closed.
   * @throws IOException If an I/O error occurs.
   */
  override fun getLocalAddress(): SocketAddress

  /**
   * Sets the value of a socket option.
   *
   * @param <T> The type of the socket option value.
   * @param name The socket option.
   * @param value The value of the socket option. A value of `null` may be a valid value for some socket options.
   * @return This channel
   * @throws UnsupportedOperationException If the socket option is not supported by this channel.
   * @throws IllegalArgumentException If the value is not a valid value for this socket option.
   * @throws ClosedChannelException If this channel is closed.
   * @throws IOException If an I/O error occurs.
   * @see java.net.StandardSocketOptions
   */
  override fun <T : Any> setOption(name: SocketOption<T>, value: T?): NetworkChannel

  /**
   * Returns the value of a socket option.
   *
   * @param <T> The type of the socket option value.
   * @param name The socket option.
   * @return The value of the socket option. A value of `null` may be a valid value for some socket options.
   * @throws UnsupportedOperationException If the socket option is not supported by this channel.
   * @throws ClosedChannelException If this channel is closed.
   * @throws IOException If an I/O error occurs.
   * @see java.net.StandardSocketOptions
   */
  override fun <T : Any> getOption(name: SocketOption<T>): T?

  /**
   * Returns a set of the socket options supported by this channel.
   *
   * @return A set of the socket options supported by this channel.
   */
  override fun supportedOptions(): Set<SocketOption<*>>
}

internal class CoroutineNetworkChannelMixin(
  private val channel: NetworkChannel
) : CoroutineNetworkChannel, NetworkChannel by channel {

  override fun bind(local: SocketAddress?): CoroutineNetworkChannel {
    channel.bind(local)
    return this
  }
}
