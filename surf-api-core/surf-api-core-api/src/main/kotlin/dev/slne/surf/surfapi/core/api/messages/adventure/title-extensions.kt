package dev.slne.surf.surfapi.core.api.messages.adventure

import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.util.Ticks
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import java.time.Duration as JavaDuration

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class TitleDsl

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class TitleTimeDsl

inline fun title(block: @TitleDsl TitleBuilder.() -> Unit): Title {
    return TitleBuilder().apply(block).build()
}

inline fun Title(block: @TitleDsl TitleBuilder.() -> Unit) = title(block)

inline fun titleTimes(block: @TitleTimeDsl TitleTimesBuilder.() -> Unit): Title.Times {
    return TitleTimesBuilder().apply(block).build()
}

@TitleDsl
class TitleBuilder {
    var title: Component = Component.empty()
    var subtitle: Component = Component.empty()
    internal var times: Title.Times = Title.DEFAULT_TIMES


    inline fun title(block: @TitleDsl SurfComponentBuilder.() -> Unit) {
        title = buildText(block)
    }

    inline fun subtitle(block: @TitleDsl SurfComponentBuilder.() -> Unit) {
        subtitle = buildText(block)
    }

    fun times(block: @TitleDsl TitleTimesBuilder.() -> Unit) {
        times = TitleTimesBuilder().apply(block).build()
    }

    @PublishedApi
    internal fun build(): Title {
        return Title.title(title, subtitle, times)
    }
}


@TitleTimeDsl
class TitleTimesBuilder {
    internal var fadeIn: JavaDuration = Ticks.duration(10)
    internal var stay: JavaDuration = Ticks.duration(70)
    internal var fadeOut: JavaDuration = Ticks.duration(20)


    fun fadeIn(ticks: Long) {
        fadeIn = Ticks.duration(ticks)
    }

    fun fadeIn(duration: JavaDuration) {
        fadeIn = duration
    }

    fun fadeIn(duration: Duration) {
        fadeIn = duration.toJavaDuration()
    }

    fun stay(ticks: Long) {
        stay = Ticks.duration(ticks)
    }

    fun stay(duration: JavaDuration) {
        stay = duration
    }

    fun stay(duration: Duration) {
        stay = duration.toJavaDuration()
    }

    fun fadeOut(ticks: Long) {
        fadeOut = Ticks.duration(ticks)
    }

    fun fadeOut(duration: JavaDuration) {
        fadeOut = duration
    }

    fun fadeOut(duration: Duration) {
        fadeOut = duration.toJavaDuration()
    }

    @PublishedApi
    internal fun build(): Title.Times {
        return Title.Times.times(fadeIn, stay, fadeOut)
    }
}