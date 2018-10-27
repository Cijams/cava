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

import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.newCoroutineContext
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import net.consensys.cava.concurrent.AsyncResult
import net.consensys.cava.concurrent.CompletableAsyncResult
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletionException
import java.util.function.BiConsumer
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Starts new co-routine and returns its result as an implementation of [AsyncResult].
 * The running co-outine is cancelled when the resulting future is cancelled or otherwise completed.
 *
 * Co-routine context is inherited from a [CoroutineScope], additional context elements can be specified with [context]
 * argument. If the context does not have any dispatcher nor any other [ContinuationInterceptor], then
 * [Dispatchers.Default] is used. The parent job is inherited from a [CoroutineScope] as well, but it can also be
 * overridden with corresponding [coroutineContext] element.
 *
 * By default, the co-routine is immediately scheduled for execution. Other options can be specified via `start`
 * parameter. See [CoroutineStart] for details. A value of [CoroutineStart.LAZY] is not supported (since
 * `AsyncResult` framework does not provide the corresponding capability) and produces [IllegalArgumentException].
 *
 * See [newCoroutineContext][CoroutineScope.newCoroutineContext] for a description of debugging facilities that are
 * available for newly created co-routine.
 *
 * @param context Additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param start Co-routine start option. The default value is [CoroutineStart.DEFAULT].
 * @param block The co-routine code.
 */
fun <T> CoroutineScope.asyncResult(
  context: CoroutineContext = Dispatchers.Default,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> T
): AsyncResult<T> {
  require(!start.isLazy) { "$start start is not supported" }
  val newContext = this.newCoroutineContext(context)
  val job = Job(newContext[Job])
  val coroutine = AsyncResultCoroutine<T>(newContext + job)
  job.invokeOnCompletion { coroutine.result.cancel() }
  coroutine.result.whenComplete { _, exception -> job.cancel(exception) }
  start(block, receiver = coroutine, completion = coroutine) // use the specified start strategy
  return coroutine.result
}

private class AsyncResultCoroutine<T>(
  override val context: CoroutineContext,
  val result: CompletableAsyncResult<T> = AsyncResult.incomplete()
) : Continuation<T>, CoroutineScope {
  override val coroutineContext: CoroutineContext get() = context
  override val isActive: Boolean get() = context[Job]!!.isActive
  override fun resume(value: T) {
    result.complete(value)
  }

  override fun resumeWithException(exception: Throwable) {
    result.completeExceptionally(exception)
  }
}

/**
 * Converts this deferred value to an [AsyncResult].
 * The deferred value is cancelled when the returned [AsyncResult] is cancelled or otherwise completed.
 */
fun <T> Deferred<T>.asAsyncResult(): AsyncResult<T> {
  val asyncResult = AsyncResult.incomplete<T>()
  asyncResult.whenComplete { _, exception -> cancel(exception) }
  invokeOnCompletion {
    try {
      asyncResult.complete(getCompleted())
    } catch (exception: Exception) {
      asyncResult.completeExceptionally(exception)
    }
  }
  return asyncResult
}

/**
 * Converts this [AsyncResult] to an instance of [Deferred].
 * The [AsyncResult] is cancelled when the resulting deferred is cancelled.
 */
fun <T> AsyncResult<T>.asDeferred(): Deferred<T> {
  // Fast path if already completed
  if (isDone) {
    return try {
      @Suppress("UNCHECKED_CAST")
      CompletableDeferred(get() as T)
    } catch (e: Throwable) {
      // unwrap original cause from CompletionException
      val original = (e as? CompletionException)?.cause ?: e
      CompletableDeferred<T>().also { it.cancel(original) }
    }
  }
  val result = CompletableDeferred<T>()
  whenComplete { value, exception ->
    if (exception == null) {
      result.complete(value)
    } else {
      result.cancel(exception)
    }
  }
  result.invokeOnCompletion { this.cancel() }
  return result
}

/**
 * Awaits for completion of the [AsyncResult] without blocking a thread.
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * stops waiting for the [AsyncResult] and immediately resumes with [CancellationException].
 *
 * Note, that [AsyncResult] does not support prompt removal of listeners, so on cancellation of this wait
 * a few small objects will remain in the [AsyncResult] stack of completion actions until it completes itself.
 * However, care is taken to clear the reference to the waiting coroutine itself, so that its memory can be
 * released even if the [AsyncResult] never completes.
 */
suspend fun <T> AsyncResult<T>.await(): T {
  // fast path when CompletableFuture is already done (does not suspend)
  if (isDone) {
    try {
      @Suppress("UNCHECKED_CAST")
      return get() as T
    } catch (e: CompletionException) {
      throw e.cause ?: e // unwrap original cause from CompletionException
    }
  }
  // slow path -- suspend
  return suspendCancellableCoroutine { cont: CancellableContinuation<T> ->
    val consumer = ContinuationBiConsumer(cont)
    whenComplete(consumer)
    cont.invokeOnCancellation {
      consumer.cont = null // shall clear reference to continuation
    }
  }
}

private class ContinuationBiConsumer<T>(
  @Volatile @JvmField var cont: Continuation<T>?
) : BiConsumer<T?, Throwable?> {
  override fun accept(result: T?, exception: Throwable?) {
    val cont = this.cont ?: return // atomically read current value unless null
    if (exception == null) // the future has been completed normally
      cont.resume(result!!)
    else // the future has completed with an exception
      cont.resumeWithException(exception)
  }
}
