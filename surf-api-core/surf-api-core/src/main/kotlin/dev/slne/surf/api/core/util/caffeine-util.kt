package dev.slne.surf.api.core.util

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalListener
import java.lang.AutoCloseable

private val log = logger()

private fun <K : Any, V : Any> noOpRemovalListener(): RemovalListener<K, V> = RemovalListener { _, _, _ -> }

/**
 * Configures a Caffeine cache to automatically close entries that are removed from the cache
 * and invokes a specified `RemovalListener` before the auto-close operation is executed.
 *
 * This method ensures that if the key or value implements `AutoCloseable`, their `close` method
 * is called when they are removed from the cache due to any removal cause (e.g., eviction, manual removal).
 * If an exception occurs during the `close` or in the provided `beforeClose` listener, it is caught
 * and logged as a warning.
 *
 * @param beforeClose an optional `RemovalListener` to be invoked before the automatic
 * closing of the removed key and value occurs. A no-op listener is used if this parameter is not provided.
 * @return a modified instance of the `Caffeine` cache configured with the automatic close-on-removal behavior.
 */
fun <K : Any, V : Any> Caffeine<K, V>.withAutoCloseOnRemoval(
    beforeClose: RemovalListener<K, V> = noOpRemovalListener()
): Caffeine<K, V> = removalListener { key, value, cause ->
    runCatching {
        beforeClose.onRemoval(key, value, cause)
    }.onFailure { throwable ->
        log.atWarning()
            .withCause(throwable)
            .log(
                "RemovalListener beforeClose failed for key=%s, value=%s, cause=%s",
                key,
                value,
                cause,
            )
    }

    (key as? AutoCloseable)?.closeCatching("key", key, value, cause)
    (value as? AutoCloseable)?.closeCatching("value", key, value, cause)
}

private fun AutoCloseable.closeCatching(label: String, key: Any?, value: Any?, cause: Any?) {
    runCatching { close() }
        .onFailure { throwable ->
            log.atWarning()
                .withCause(throwable)
                .log("Failed to close %s on removal. key=%s, value=%s, cause=%s", label, key, value, cause)
        }
}
