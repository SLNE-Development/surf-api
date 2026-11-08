package dev.slne.surf.api.core.config.serializer.collection.map

/**
 * Writes normalized map keys back to the configuration after deserialization.
 *
 * This is useful for key types whose serializer accepts multiple input formats but serializes
 * back into one canonical format.
 *
 * Usage:
 * ```kotlin
 * @ConfigSerializable
 * data class Config(
 *     val values: Map<@WriteKeyBack Key, Int> = emptyMap()
 * )
 * ```
 *
 * If the key serializer reads `minecraft:STONE` but serializes it as `minecraft:stone`,
 * the old key is removed and the normalized key is written back.
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class WriteKeyBack