package dev.slne.surf.surfapi.core.api.messages.adventure

import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import dev.slne.surf.surfapi.core.api.util.mutableObjectSetOf
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component

/**
 * A DSL marker for the BossBar DSL to prevent scope conflicts in nested DSL blocks.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class BossBarDsl

/**
 * Creates a [BossBar] using the DSL-style builder.
 *
 * @param block The configuration block for building the boss bar.
 * @return A configured [BossBar] instance.
 *
 * **Example Usage:**
 * ```kotlin
 * val bossBar = bossBar {
 *     name { appendText("Final Boss", ERROR) }
 *     progress = 0.75f
 *     color = BossBar.Color.RED
 *     overlay = BossBar.Overlay.NOTCHED_6
 *     flags(BossBar.Flag.CREATE_WORLD_FOG, BossBar.Flag.DARKEN_SCREEN)
 * }
 * ```
 */
inline fun bossBar(block: @BossBarDsl BossBarBuilder.() -> Unit): BossBar {
    return BossBarBuilder().apply(block).build()
}

/**
 * Creates a [BossBar] using the DSL-style builder. This is an alias for [bossBar].
 *
 * @param block The configuration block for building the boss bar.
 * @return A configured [BossBar] instance.
 */
inline fun BossBar(block: @BossBarDsl BossBarBuilder.() -> Unit) = bossBar(block)

/**
 * A DSL builder for creating a [BossBar].
 */
@BossBarDsl
class BossBarBuilder {

    /**
     * The name (title) of the boss bar.
     */
    var name: Component = Component.empty()

    /**
     * The progress of the boss bar, ranging from [BossBar.MIN_PROGRESS] (0.0) to [BossBar.MAX_PROGRESS] (1.0).
     *
     * @throws IllegalArgumentException if the progress is set outside the valid range.
     *
     * **Example Usage:**
     * ```kotlin
     * bossBar {
     *     progress = 0.5f  // 50% progress
     * }
     * ```
     */
    var progress = BossBar.MIN_PROGRESS
        set(value) {
            require(value in BossBar.MIN_PROGRESS..BossBar.MAX_PROGRESS) { "Progress must be between 0 and 1" }
            field = value
        }

    /**
     * The color of the boss bar. Defaults to [BossBar.Color.WHITE].
     *
     * **Example Usage:**
     * ```kotlin
     * bossBar {
     *     color = BossBar.Color.BLUE
     * }
     * ```
     */
    var color = BossBar.Color.WHITE

    /**
     * The overlay style of the boss bar, which defines how the progress is displayed.
     * Defaults to [BossBar.Overlay.PROGRESS].
     *
     * **Example Usage:**
     * ```kotlin
     * bossBar {
     *     overlay = BossBar.Overlay.NOTCHED_10
     * }
     * ```
     */
    var overlay = BossBar.Overlay.PROGRESS

    /**
     * A set of flags that modify the boss bar's behavior.
     * Defaults to an empty set.
     *
     * **Example Usage:**
     * ```kotlin
     * bossBar {
     *     flags(BossBar.Flag.CREATE_WORLD_FOG, BossBar.Flag.DARKEN_SCREEN)
     * }
     * ```
     */
    var flags = mutableObjectSetOf<BossBar.Flag>(3)

    /**
     * Sets the name (title) of the boss bar using a component builder.
     *
     * @param block The configuration block for creating the name component.
     *
     * **Example Usage:**
     * ```kotlin
     * bossBar {
     *     name { appendText("Dragon Attack!", ERROR) }
     * }
     * ```
     */
    inline fun name(block: @BossBarDsl SurfComponentBuilder.() -> Unit) {
        name = buildText(block)
    }

    /**
     * Sets multiple flags for the boss bar.
     *
     * @param flags The flags to apply to the boss bar.
     *
     * **Example Usage:**
     * ```kotlin
     * bossBar {
     *     flags(BossBar.Flag.CREATE_WORLD_FOG, BossBar.Flag.DARKEN_SCREEN)
     * }
     * ```
     */
    fun flags(vararg flags: BossBar.Flag) {
        this.flags = mutableObjectSetOf(*flags)
    }

    /**
     * Builds and returns the configured [BossBar] instance.
     *
     * @return The constructed [BossBar] object.
     */
    @PublishedApi
    internal fun build(): BossBar {
        return BossBar.bossBar(name, progress, color, overlay, flags)
    }
}