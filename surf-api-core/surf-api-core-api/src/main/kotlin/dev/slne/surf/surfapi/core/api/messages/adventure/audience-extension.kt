@file:Suppress("NOTHING_TO_INLINE")

package dev.slne.surf.surfapi.core.api.messages.adventure

import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.Sound.Emitter
import net.kyori.adventure.util.TriState

inline fun Audience.sendText(block: SurfComponentBuilder.() -> Unit) {
    sendMessage(SurfComponentBuilder(block))
}

inline fun Audience.sendActionBar(block: SurfComponentBuilder.() -> Unit) {
    sendActionBar(SurfComponentBuilder(block))
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

inline fun Audience.uuidOrNull() = getPointer(Identity.UUID)
inline fun Audience.uuid() = uuidOrNull() ?: error("Audience does not have a UUID pointer")
inline fun Audience.nameOrNull() = getPointer(Identity.NAME)
inline fun Audience.name() = nameOrNull() ?: error("Audience does not have a name pointer")
inline fun Audience.displayNameOrNull() = getPointer(Identity.DISPLAY_NAME)
inline fun Audience.displayName() =
    displayNameOrNull() ?: error("Audience does not have a display name pointer")

inline fun Audience.testPermission(permission: String) =
    getPointer(PermissionChecker.POINTER)?.value(permission) ?: TriState.NOT_SET

inline fun Audience.hasPermission(permission: String) =
    getPointer(PermissionChecker.POINTER)?.test(permission) ?: false