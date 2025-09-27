package dev.slne.surf.surfapi.core.api.serializer

import dev.slne.surf.surfapi.core.api.serializer.adventure.book.AdventureBookCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.bossbar.AdventureBossBarCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.component.AdventureComponentCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.component.decoration.AdventureTextDecorationCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.component.shadowcolor.AdventureShadowColorCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.component.textcolor.AdventureTextColorCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.key.AdventureKeyCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.resourcepack.info.AdventureResourcePackInfoCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.sound.AdventureSoundCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.sound.stop.AdventureSoundStopCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.title.AdventureTitleCodec
import dev.slne.surf.surfapi.core.api.serializer.java.uri.JavaURICodec
import dev.slne.surf.surfapi.core.api.serializer.java.uuid.JavaUUIDCodec

object SurfCodecs {
    // region Adventure
    val COMPONENT = AdventureComponentCodec.CODEC
    val SHADOW_COLOR = AdventureShadowColorCodec.CODEC
    val TEXT_COLOR = AdventureTextColorCodec.CODEC
    val TEXT_DECORATION = AdventureTextDecorationCodec.CODEC
    val BOOK = AdventureBookCodec.CODEC
    val BOSS_BAR = AdventureBossBarCodec.CODEC
    val RESOURCE_PACK_INFO = AdventureResourcePackInfoCodec.CODEC
    val KEY = AdventureKeyCodec.CODEC
    val SOUND = AdventureSoundCodec.CODEC
    val SOUND_STOP = AdventureSoundStopCodec.CODEC
    val TITLE = AdventureTitleCodec.CODEC
    // endregion

    // region Java
    val URI = JavaURICodec.CODEC
    val UUID = JavaUUIDCodec.CODEC
    // endregion
}