package dev.slne.surf.api.core.messages.adventure

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.util.Ticks
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import java.time.Duration as JavaDuration

/**
 * A DSL marker for the Title DSL to prevent scope conflicts in nested DSL blocks.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class TitleDsl

/**
 * A DSL marker for the Title Times DSL to prevent scope conflicts in nested DSL blocks.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class TitleTimeDsl

/**
 * Creates a [Title] using the DSL-style builder.
 *
 * @param block The configuration block for building the title.
 * @return A configured [Title] instance.
 *
 * **Example Usage:**
 * ```kotlin
 * val myTitle = title {
 *     title { appendText("Welcome!", PRIMARY) }
 *     subtitle { appendText("Enjoy your stay.", SECONDARY) }
 *     times {
 *         fadeIn(20)  // 1 second fade-in
 *         stay(100)   // 5 seconds display
 *         fadeOut(40) // 2 seconds fade-out
 *     }
 * }
 * ```
 */
inline fun title(block: @TitleDsl TitleBuilder.() -> Unit): Title {
    return TitleBuilder().apply(block).build()
}

/**
 * Creates a [Title] using the DSL-style builder. This is an alias for [title].
 *
 * @param block The configuration block for building the title.
 * @return A configured [Title] instance.
 * @see title
 */
inline fun Title(block: @TitleDsl TitleBuilder.() -> Unit) = title(block)

/**
 * Creates a [Title.Times] instance using the DSL-style builder.
 *
 * @param block The configuration block for setting title timing.
 * @return A configured [Title.Times] instance.
 *
 * **Example Usage:**
 * ```kotlin
 * val times = titleTimes {
 *     fadeIn(20)
 *     stay(80)
 *     fadeOut(30)
 * }
 * ```
 */
inline fun titleTimes(block: @TitleTimeDsl TitleTimesBuilder.() -> Unit): Title.Times {
    return TitleTimesBuilder().apply(block).build()
}

/**
 * A DSL builder for creating a [Title].
 */
@TitleDsl
class TitleBuilder {
    /**
     * The main title text of the [Title].
     */
    var title: Component = Component.empty()

    /**
     * The subtitle text of the [Title].
     */
    var subtitle: Component = Component.empty()

    /**
     * The timing settings for the [Title]. Defaults to [Title.DEFAULT_TIMES].
     */
    internal var times: Title.Times = Title.DEFAULT_TIMES


    /**
     * Sets the main title text using a component builder.
     *
     * @param block The configuration block for creating the title component.
     *
     * **Example Usage:**
     * ```kotlin
     * title {
     *     title { appendText("Game Over", ERROR) }
     * }
     * ```
     */
    inline fun title(block: @TitleDsl SurfComponentBuilder.() -> Unit) {
        title = buildText(block)
    }

    /**
     * Sets the subtitle text using a component builder.
     *
     * @param block The configuration block for creating the subtitle component.
     *
     * **Example Usage:**
     * ```kotlin
     * title {
     *     subtitle { appendText("You have lost the game.", SECONDARY) }
     * }
     * ```
     */
    inline fun subtitle(block: @TitleDsl SurfComponentBuilder.() -> Unit) {
        subtitle = buildText(block)
    }

    /**
     * Sets the timing for the title display.
     *
     * @param block The configuration block for setting fade-in, stay, and fade-out times.
     *
     * **Example Usage:**
     * ```kotlin
     * title {
     *     times {
     *         fadeIn(20)
     *         stay(100)
     *         fadeOut(40)
     *     }
     * }
     * ```
     */
    fun times(block: @TitleDsl TitleTimesBuilder.() -> Unit) {
        times = TitleTimesBuilder().apply(block).build()
    }

    /**
     * Builds and returns the configured [Title] instance.
     *
     * @return The constructed [Title] object.
     */
    @PublishedApi
    internal fun build(): Title {
        return Title.title(title, subtitle, times)
    }
}

/**
 * A DSL builder for setting the fade-in, stay, and fade-out times of a [Title].
 */
@TitleTimeDsl
class TitleTimesBuilder {
    /**
     * The fade-in duration before the title fully appears.
     */
    internal var fadeIn: JavaDuration = Title.DEFAULT_TIMES.fadeIn()

    /**
     * The duration for which the title remains visible.
     */
    internal var stay: JavaDuration = Title.DEFAULT_TIMES.stay()

    /**
     * The fade-out duration before the title disappears.
     */
    internal var fadeOut: JavaDuration = Title.DEFAULT_TIMES.fadeOut()


    /**
     * Sets the fade-in duration in ticks.
     *
     * @param ticks The duration in ticks.
     *
     * **Example Usage:**
     * ```kotlin
     * times {
     *     fadeIn(30) // 1.5 seconds
     * }
     * ```
     */
    fun fadeIn(ticks: Long) {
        fadeIn = Ticks.duration(ticks)
    }

    /**
     * Sets the fade-in duration using a [JavaDuration].
     *
     * @param duration The duration.
     */
    fun fadeIn(duration: JavaDuration) {
        fadeIn = duration
    }

    /**
     * Sets the fade-in duration using a [Duration].
     *
     * @param duration The duration.
     */
    fun fadeIn(duration: Duration) {
        fadeIn = duration.toJavaDuration()
    }

    /**
     * Sets the stay duration in ticks.
     *
     * @param ticks The duration in ticks.
     *
     * **Example Usage:**
     * ```kotlin
     * times {
     *     stay(80) // 4 seconds
     * }
     * ```
     */
    fun stay(ticks: Long) {
        stay = Ticks.duration(ticks)
    }

    /**
     * Sets the stay duration using a [JavaDuration].
     *
     * @param duration The duration.
     */
    fun stay(duration: JavaDuration) {
        stay = duration
    }

    /**
     * Sets the stay duration using a [Duration].
     *
     * @param duration The duration.
     */
    fun stay(duration: Duration) {
        stay = duration.toJavaDuration()
    }

    /**
     * Sets the fade-out duration in ticks.
     *
     * @param ticks The duration in ticks.
     *
     * **Example Usage:**
     * ```kotlin
     * times {
     *     fadeOut(40) // 2 seconds
     * }
     * ```
     */
    fun fadeOut(ticks: Long) {
        fadeOut = Ticks.duration(ticks)
    }

    /**
     * Sets the fade-out duration using a [JavaDuration].
     *
     * @param duration The duration.
     */
    fun fadeOut(duration: JavaDuration) {
        fadeOut = duration
    }

    /**
     * Sets the fade-out duration using a [Duration].
     *
     * @param duration The duration.
     */
    fun fadeOut(duration: Duration) {
        fadeOut = duration.toJavaDuration()
    }

    /**
     * Builds and returns the configured [Title.Times] instance.
     *
     * @return The constructed [Title.Times] object.
     */
    @PublishedApi
    internal fun build(): Title.Times {
        return Title.Times.times(fadeIn, stay, fadeOut)
    }
}