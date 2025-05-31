package dev.slne.surf.surfapi.core.api.messages.adventure

import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.Sound.Emitter

inline fun Audience.sendText(block: SurfComponentBuilder.() -> Unit) {
    sendMessage(SurfComponentBuilder(block))
}

inline fun Audience.openBook(block: @BookDsl Book.Builder.() -> Unit) {
    openBook(Book(block))
}

inline fun Audience.showBossBar(block: @BossBarDsl BossBarBuilder.() -> Unit) {
    showBossBar(BossBar(block))
}

inline fun Audience.playSound(block: @SoundDsl Sound.Builder.() -> Unit) {
    playSound(Sound(block))
}

inline fun Audience.playSound(useSelfEmitter: Boolean, block: @SoundDsl Sound.Builder.() -> Unit) {
    if (useSelfEmitter) {
        playSound(Sound(block), Emitter.self())
        return
    }

    playSound(Sound(block))
}

inline fun Audience.showTitle(block: @TitleDsl TitleBuilder.() -> Unit) {
    showTitle(Title(block))
}