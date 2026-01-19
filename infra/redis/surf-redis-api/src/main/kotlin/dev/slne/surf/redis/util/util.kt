package dev.slne.surf.redis.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.reactor.awaitSingle
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2

/**
 * Converts this [Mono] into a [Deferred] by subscribing to it.
 *
 * The returned [Deferred] completes:
 * - successfully with the single emitted value
 * - exceptionally if the [Mono] signals an error
 *
 * If the [Mono] completes without emitting any value, the [Deferred] completes
 * exceptionally with [NoSuchElementException].
 *
 * Cancellation of the returned [Deferred] cancels the underlying subscription.
 *
 * Note: This variant uses a manual Reactive Streams [Subscriber]. Prefer
 * [asDeferred(scope)] if you already have a [CoroutineScope] and want to rely on
 * `kotlinx-coroutines-reactor` bridging.
 */
fun <T : Any> Mono<T>.asDeferred(): Deferred<T> {
    val deferred = CompletableDeferred<T>()

    subscribe(object : Subscriber<T> {
        private var value: T? = null

        override fun onSubscribe(s: Subscription) {
            deferred.invokeOnCompletion {
                if (deferred.isCancelled) s.cancel()
            }

            s.request(Long.MAX_VALUE)
        }

        override fun onComplete() {
            deferred.complete(
                value ?: throw NoSuchElementException("Mono completed without emitting any value")
            )
            value = null
        }

        override fun onNext(t: T) {
            value = t
        }

        override fun onError(t: Throwable) {
            deferred.completeExceptionally(t)
        }
    })

    return deferred
}

/**
 * Converts this [Mono] into a [Deferred] using the given [scope].
 *
 * This is a coroutine-based bridge that awaits the [Mono] via [awaitSingle].
 * The returned [Deferred] is cancelled if the enclosing coroutine is cancelled.
 *
 * If the [Mono] completes without emitting any value, [awaitSingle] throws
 * [NoSuchElementException].
 */
fun <T : Any> Mono<T>.asDeferred(scope: CoroutineScope): Deferred<T> =
    scope.async {
        this@asDeferred.awaitSingle()
    }


/**
 * Enables Kotlin destructuring for Reactor's [Tuple2].
 *
 * Example:
 * ```
 * val (a, b) = tuple2
 * ```
 */
operator fun <T1 : Any, T2 : Any> Tuple2<T1, T2>.component1(): T1 = this.t1

/**
 * Enables Kotlin destructuring for Reactor's [Tuple2].
 *
 * Example:
 * ```
 * val (a, b) = tuple2
 * ```
 */
operator fun <T1 : Any, T2 : Any> Tuple2<T1, T2>.component2(): T2 = this.t2