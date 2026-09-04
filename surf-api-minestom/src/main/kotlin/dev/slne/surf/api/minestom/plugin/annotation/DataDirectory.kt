package dev.slne.surf.api.minestom.plugin.annotation

import com.google.inject.BindingAnnotation

/**
 * The plugin's own data directory, `plugins/<plugin-id>`.
 *
 * The server creates the directory while it wires the plugin, so it already exists by the time
 * anything is injected.
 *
 * ```
 * class MyStorage @Inject constructor(
 *     @DataDirectory private val dataDirectory: Path,
 * ) {
 *     private val file = dataDirectory.resolve("storage.yml")
 * }
 * ```
 *
 * @see dev.slne.surf.api.minestom.plugin.MinestomPlugin
 */
@BindingAnnotation
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
)
annotation class DataDirectory
