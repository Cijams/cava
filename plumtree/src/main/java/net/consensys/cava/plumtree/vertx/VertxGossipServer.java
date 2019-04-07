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
package net.consensys.cava.plumtree.vertx;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.CompletableAsyncCompletion;
import net.consensys.cava.plumtree.MessageHashing;
import net.consensys.cava.plumtree.MessageSender;
import net.consensys.cava.plumtree.MessageValidator;
import net.consensys.cava.plumtree.Peer;
import net.consensys.cava.plumtree.PeerRepository;
import net.consensys.cava.plumtree.State;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServer;

/**
 * Vert.x implementation of the plumtree gossip.
 *
 * This implementation is provided as an example and relies on a simplistic JSON serialization of messages.
 *
 */
public final class VertxGossipServer {

  private static final ObjectMapper mapper = new ObjectMapper();

  private static final class Message {

    public MessageSender.Verb verb;
    public String hash;
    public String payload;
  }
  private final class SocketHandler {

    private final Peer peer;

    SocketHandler(Peer peer) {
      this.peer = peer;
      state.addPeer(peer);
    }

    private Bytes buffer = Bytes.EMPTY;

    void handle(Buffer data) {
      buffer = Bytes.concatenate(buffer, Bytes.wrapBuffer(data));
      Message message;
      try {
        JsonParser parser = mapper.getFactory().createParser(buffer.toArrayUnsafe());
        message = parser.readValueAs(Message.class);
        buffer = buffer.slice((int) parser.getCurrentLocation().getByteOffset());
      } catch (IOException e) {
        return;
      }

      switch (message.verb) {
        case IHAVE:
          state.receiveIHaveMessage(peer, Bytes.fromHexString(message.payload));
          break;
        case GOSSIP:
          state.receiveGossipMessage(peer, Bytes.fromHexString(message.payload));
          break;
        case GRAFT:
          state.receiveGraftMessage(peer, Bytes.fromHexString(message.payload));
          break;
        case PRUNE:
          state.receivePruneMessage(peer);
          break;
      }
    }

    void close(Void aVoid) {
      state.removePeer(peer);
    }
  }

  private final AtomicBoolean started = new AtomicBoolean(false);
  private final Vertx vertx;
  private final int port;
  private final MessageHashing messageHashing;
  private final String networkInterface;
  private final PeerRepository peerRepository;
  private final Consumer<Bytes> payloadListener;
  private final MessageValidator payloadValidator;
  private State state;
  private NetServer server;
  private NetClient client;

  public VertxGossipServer(
      Vertx vertx,
      String networkInterface,
      int port,
      MessageHashing messageHashing,
      PeerRepository peerRepository,
      Consumer<Bytes> payloadListener,
      MessageValidator payloadValidator) {
    this.vertx = vertx;
    this.networkInterface = networkInterface;
    this.port = port;
    this.messageHashing = messageHashing;
    this.peerRepository = peerRepository;
    this.payloadListener = payloadListener;
    this.payloadValidator = payloadValidator;
  }

  public AsyncCompletion start() {
    if (started.compareAndSet(false, true)) {
      CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
      server = vertx.createNetServer();
      client = vertx.createNetClient();
      server.connectHandler(socket -> {
        Peer peer = new SocketPeer(socket);
        SocketHandler handler = new SocketHandler(peer);
        socket.handler(handler::handle).closeHandler(handler::close);
      });
      server.listen(port, networkInterface, res -> {
        if (res.failed()) {
          completion.completeExceptionally(res.cause());
        } else {
          state = new State(peerRepository, messageHashing, (verb, peer, hash, payload) -> {
            Message message = new Message();
            message.verb = verb;
            message.hash = hash.toHexString();
            message.payload = payload == null ? null : payload.toHexString();
            try {
              ((SocketPeer) peer).socket().write(Buffer.buffer(mapper.writeValueAsBytes(message)));
            } catch (JsonProcessingException e) {
              throw new RuntimeException(e);
            }
          }, payloadListener, payloadValidator);

          completion.complete();
        }
      });

      return completion;
    } else {
      return AsyncCompletion.completed();
    }
  }

  public AsyncCompletion stop() {
    if (started.compareAndSet(true, false)) {
      CompletableAsyncCompletion completion = AsyncCompletion.incomplete();

      state.stop();
      client.close();
      server.close(res -> {
        if (res.failed()) {
          completion.completeExceptionally(res.cause());
        } else {
          completion.complete();
        }
      });

      return completion;
    }
    return AsyncCompletion.completed();
  }

  public AsyncCompletion connectTo(String host, int port) {
    if (!started.get()) {
      throw new IllegalStateException("Server has not started");
    }
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    client.connect(port, host, res -> {
      if (res.failed()) {
        completion.completeExceptionally(res.cause());
      } else {
        completion.complete();
        Peer peer = new SocketPeer(res.result());
        SocketHandler handler = new SocketHandler(peer);
        res.result().handler(handler::handle).closeHandler(handler::close);
      }
    });

    return completion;
  }

  /**
   * Gossip a message to all known peers.
   *
   * @param message the payload to propagate
   */
  public void gossip(Bytes message) {
    if (!started.get()) {
      throw new IllegalStateException("Server has not started");
    }
    state.sendGossipMessage(message);
  }
}
