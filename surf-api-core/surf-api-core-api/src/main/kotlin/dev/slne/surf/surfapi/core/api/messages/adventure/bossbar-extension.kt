package dev.slne.surf.surfapi.core.api.messages.adventure

import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import dev.slne.surf.surfapi.core.api.util.mutableObjectSetOf
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class BossBarDsl

inline fun bossBar(block: @BossBarDsl BossBarBuilder.() -> Unit): BossBar {
    return BossBarBuilder().apply(block).build()
}

inline fun BossBar(block: @BossBarDsl BossBarBuilder.() -> Unit) = bossBar(block)

@BossBarDsl
class BossBarBuilder {
    var name: Component = Component.empty()

    var progress = BossBar.MIN_PROGRESS
        set(value) {
            require(value in BossBar.MIN_PROGRESS..BossBar.MAX_PROGRESS) { "Progress must be between 0 and 1" }
            field = value
        }
    var color = BossBar.Color.WHITE
    var overlay = BossBar.Overlay.PROGRESS

    var flags = mutableObjectSetOf<BossBar.Flag>(3)

    inline fun name(block: @BossBarDsl SurfComponentBuilder.() -> Unit) {
        name = buildText(block)
    }

    fun flags(vararg flags: BossBar.Flag) {
        this.flags = mutableObjectSetOf(*flags)
    }
    @PublishedApi
    internal fun build(): BossBar {
        return BossBar.bossBar(name, progress, color, overlay, flags)
    }
}
