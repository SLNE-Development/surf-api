package dev.slne.surf.surfapi.core.api.messages.adventure

import net.kyori.adventure.sound.Sound


@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class SoundDsl

inline fun sound(block: @SoundDsl Sound.Builder.() -> Unit): Sound {
    return Sound.sound().apply(block).build()
}

inline fun Sound(block: @SoundDsl Sound.Builder.() -> Unit) = sound(block)