package dev.slne.surf.surfapi.core.api.serializer.adventure.sound.stop

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.slne.surf.surfapi.core.api.serializer.adventure.key.AdventureKeyCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.sound.AdventureSoundCodec
import net.kyori.adventure.sound.SoundStop

object AdventureSoundStopCodec {

    val CODEC: Codec<SoundStop> = RecordCodecBuilder.create { instance ->
        instance.group(
            AdventureSoundCodec.SOURCE_CODEC.optionalFieldOf("source", null)
                .forGetter(SoundStop::source),
            AdventureKeyCodec.CODEC.optionalFieldOf("sound", null)
                .forGetter(SoundStop::sound),
        ).apply(instance) { source, sound ->
            if (source == null && sound == null) {
                SoundStop.all()
            } else if (source != null && sound != null) {
                SoundStop.namedOnSource(sound, source)
            } else if (source != null) {
                SoundStop.source(source)
            } else {
                SoundStop.named(sound)
            }
        }
    }
}