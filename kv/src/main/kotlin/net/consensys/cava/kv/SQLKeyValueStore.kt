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
package net.consensys.cava.kv

import com.jolbox.bonecp.BoneCP
import com.jolbox.bonecp.BoneCPConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import net.consensys.cava.bytes.Bytes
import java.io.IOException

/**
 * A key-value store backed by a relational database.
 *
 * @param jdbcurl The JDBC url to connect to the database.
 * @param tableName the name of the table to use for storage.
 * @param keyColumn the key column of the store.
 * @param valueColumn the value column of the store.
 * @param dispatcher The co-routine context for blocking tasks.
 * @return A key-value store.
 * @throws IOException If an I/O error occurs.
 * @constructor Open a relational database backed key-value store.
 */
class SQLKeyValueStore
@Throws(IOException::class)
constructor(
  jdbcurl: String,
  val tableName: String = "store",
  val keyColumn: String = "key",
  val valueColumn: String = "value",
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : KeyValueStore {

  companion object {
    /**
     * Open a relational database backed key-value store.
     *
     * @param jdbcUrl The JDBC url to connect to the database.
     * @return A key-value store.
     * @throws IOException If an I/O error occurs.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun open(jdbcUrl: String) = SQLKeyValueStore(jdbcUrl)

    /**
     * Open a relational database backed key-value store.
     *
     * @param jdbcUrl The JDBC url to connect to the database.
     * @param tableName the name of the table to use for storage.
     * @param keyColumn the key column of the store.
     * @param valueColumn the value column of the store.
     * @return A key-value store.
     * @throws IOException If an I/O error occurs.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun open(jdbcUrl: String, tableName: String, keyColumn: String, valueColumn: String) =
      SQLKeyValueStore(jdbcUrl, tableName, keyColumn, valueColumn)
  }

  private val connectionPool: BoneCP

  init {
    val config = BoneCPConfig()
    config.jdbcUrl = jdbcurl

    connectionPool = BoneCP(config)
  }

  override suspend fun get(key: Bytes): Bytes? = withContext(dispatcher) {
      connectionPool.asyncConnection.await().use {
        val stmt = it.prepareStatement("SELECT $valueColumn FROM $tableName WHERE $keyColumn = ?")
        stmt.setBytes(1, key.toArrayUnsafe())
        stmt.execute()

        val rs = stmt.resultSet

        if (rs.next()) {
          Bytes.wrap(rs.getBytes(1))
        } else {
          null
        }
      }
  }

  override suspend fun put(key: Bytes, value: Bytes) = withContext(dispatcher) {
    connectionPool.asyncConnection.await().use {
        val stmt = it.prepareStatement("INSERT INTO $tableName($keyColumn, $valueColumn) VALUES(?,?)")
        stmt.setBytes(1, key.toArrayUnsafe())
        stmt.setBytes(2, value.toArrayUnsafe())
        stmt.execute()
        Unit
      }
  }

  /**
   * Closes the underlying connection pool.
   */
  override fun close() = connectionPool.shutdown()
}
