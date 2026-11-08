package dev.slne.surf.api.core.config.serializer.collection.map

/**
 * Forces [MapSerializer] to use Configurate's default map serializer behavior.
 *
 * Without this annotation, [MapSerializer] logs and skips individual invalid map entries.
 * With this annotation, serialization and deserialization errors are thrown normally.
 *
 * Usage:
 * ```kotlin
 * @ConfigSerializable
 * data class Config(
 *     val strictMap: @ThrowExceptions Map<String, Int> = emptyMap()
 * )
 * ```
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class ThrowExceptions