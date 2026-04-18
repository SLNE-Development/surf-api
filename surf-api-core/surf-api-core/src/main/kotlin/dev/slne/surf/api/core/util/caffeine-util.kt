package dev.slne.surf.api.core.util

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalListener
import java.lang.AutoCloseable

private val log = logger()

private fun <K : Any, V : Any> noOpRemovalListener(): RemovalListener<K, V> = RemovalListener { _, _, _ -> }

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
