package dev.slne.surf.surfapi.core.api.serializer

import dev.slne.surf.surfapi.core.api.serializer.adventure.book.AdventureBookSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.bossbar.AdventureBossBarSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.component.AdventureComponentSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.component.decoration.AdventureTextDecorationSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.component.shadowcolor.AdventureShadowColorSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.component.textcolor.AdventureTextColorSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.key.AdventureKeySerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.resourcepack.info.AdventureResourcePackInfoSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.sound.AdventureSoundSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.sound.stop.AdventureSoundStopSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.title.AdventureTitleSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.title.AdventureTitleSerializer.AdventureTitleTimes
import dev.slne.surf.surfapi.core.api.serializer.java.uri.JavaURISerializer
import dev.slne.surf.surfapi.core.api.serializer.java.uuid.JavaUUIDSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

object SurfSerializerModule {
    val adventure = SerializersModule {
        contextual(AdventureComponentSerializer)
        contextual(AdventureTextDecorationSerializer)
        contextual(AdventureShadowColorSerializer)
        contextual(AdventureTextColorSerializer)
        contextual(AdventureBookSerializer)
        contextual(AdventureBossBarSerializer)
        contextual(AdventureResourcePackInfoSerializer)
        contextual(AdventureKeySerializer)
        contextual(AdventureSoundSerializer)
        contextual(AdventureSoundStopSerializer)
        contextual(AdventureTitleSerializer)
        contextual(AdventureTitleTimes)
    }

    val java = SerializersModule {
        contextual(JavaURISerializer)
        contextual(JavaUUIDSerializer)
    }

    val all = SerializersModule {
        include(adventure)
        include(java)
    }
}