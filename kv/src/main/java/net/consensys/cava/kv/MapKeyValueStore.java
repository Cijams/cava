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
package net.consensys.cava.kv;

import net.consensys.cava.bytes.Bytes;

import java.util.Map;

/**
 * A key-value store backed by an in-memory Map.
 */
public interface MapKeyValueStore extends KeyValueStore {

  /**
   * Open an in-memory key-value store.
   *
   * This store will use a {@link java.util.HashMap} as a backing store.
   *
   * @return A key-value store.
   */
  static MapKeyValueStore open() {
    return new net.consensys.cava.kv.experimental.MapKeyValueStore();
  }

  /**
   * Open an in-memory key-value store.
   *
   * @param map The backing map for this store.
   * @return A key-value store.
   */
  static MapKeyValueStore open(Map<Bytes, Bytes> map) {
    return new net.consensys.cava.kv.experimental.MapKeyValueStore(map);
  }
}
