package dev.slne.surf.api.core.messages.adventure

import net.kyori.adventure.sound.Sound

/**
 * A DSL marker for the Sound DSL to prevent scope conflicts in nested DSL blocks.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class SoundDsl

/**
 * Creates a [Sound] using the DSL-style builder.
 *
 * @param block The configuration block for building the sound.
 * @return A configured [Sound] instance.
 *
 * **Example Usage:**
 * ```kotlin
 * val mySound = sound {
 *     source(Sound.Source.PLAYER)
 *     type(key("minecraft", "entity.player.hurt"))
 *     volume(1.0f)
 *     pitch(1.2f)
 * }
 * ```
 */
inline fun sound(block: @SoundDsl Sound.Builder.() -> Unit): Sound {
    return Sound.sound().apply(block).build()
}

/**
 * Creates a [Sound] using the DSL-style builder. This is an alias for [sound].
 *
 * @param block The configuration block for building the sound.
 * @return A configured [Sound] instance.
 */
inline fun Sound(block: @SoundDsl Sound.Builder.() -> Unit) = sound(block)

typealias AdventureSound = Sound