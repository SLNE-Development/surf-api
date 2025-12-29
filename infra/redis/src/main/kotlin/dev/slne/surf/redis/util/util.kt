package dev.slne.surf.redis.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.reactor.awaitSingle
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactor.core.publisher.Mono

internal fun <T : Any> Mono<T>.asDeferred(): Deferred<T> {
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

internal fun <T : Any> Mono<T>.asDeferred(scope: CoroutineScope): Deferred<T> =
    scope.async {
        this@asDeferred.awaitSingle()
    }