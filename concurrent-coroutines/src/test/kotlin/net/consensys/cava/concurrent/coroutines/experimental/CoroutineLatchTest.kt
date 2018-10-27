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
package net.consensys.cava.concurrent.coroutines.experimental

import kotlinx.coroutines.experimental.TimeoutCancellationException
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CoroutineLatchTest {

  @Test
  fun shouldntSuspendWhenLatchIsOpen() = runBlocking {
    withTimeout(1) {
      CoroutineLatch(0).await()
    }
    withTimeout(1) {
      CoroutineLatch(-1).await()
    }
  }

  @Test
  fun shouldUnsuspendWhenLatchOpens() = runBlocking {
    val latch = CoroutineLatch(2)
    assertFalse(latch.isOpen)
    assertEquals(2, latch.count)

    var ok = false
    var done = false
    val job = async {
      latch.await()
      assertTrue(ok, "failed to suspend")
      done = true
    }

    Thread.sleep(100)
    assertFalse(latch.countDown())
    assertFalse(latch.isOpen)
    assertEquals(1, latch.count)

    Thread.sleep(100)
    assertFalse(done, "woke up too early")

    ok = true
    assertTrue(latch.countDown())
    assertTrue(latch.isOpen)
    assertEquals(0, latch.count)
    job.await()
    assertTrue(done, "failed to wakeup")
  }

  @Test
  fun shouldSuspendWhenLatchCloses() = runBlocking {
    val latch = CoroutineLatch(-1)
    assertTrue(latch.isOpen)
    assertEquals(-1, latch.count)

    withTimeout(1) {
      latch.await()
    }

    assertFalse(latch.countUp())
    assertTrue(latch.isOpen)
    assertEquals(0, latch.count)

    withTimeout(1) {
      latch.await()
    }

    assertTrue(latch.countUp())
    assertFalse(latch.isOpen)
    assertEquals(1, latch.count)

    var ok = false
    var done = false
    val job = async {
      latch.await()
      assertTrue(ok, "failed to suspend")
      done = true
    }

    ok = true
    assertTrue(latch.countDown())
    assertTrue(latch.isOpen)
    job.await()
    assertTrue(done, "failed to wakeup")
  }

  @Test
  fun shouldTimeoutWhenBlocked() {
    assertThrows<TimeoutCancellationException> {
      runBlocking {
        withTimeout(1) {
          CoroutineLatch(1).await()
        }
      }
    }
  }
}
