package dev.slne.surf.surfapi.core.api.serializer.adventure.sound

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.slne.surf.surfapi.core.api.serializer.adventure.key.AdventureKeyCodec
import net.kyori.adventure.sound.Sound
import java.util.*

object AdventureSoundCodec {
    val SOURCE_CODEC: Codec<Sound.Source> = Codec.STRING.comapFlatMap({ string ->
        try {
            DataResult.success(Sound.Source.valueOf(string.uppercase()))
        } catch (e: IllegalArgumentException) {
            DataResult.error { e.message ?: "Invalid sound source: $string" }
        }
    }, Sound.Source::name)

    val CODEC: Codec<Sound> = RecordCodecBuilder.create { instance ->
        instance.group(
            AdventureKeyCodec.CODEC.fieldOf("name").forGetter(Sound::name),
            SOURCE_CODEC.fieldOf("source").forGetter(Sound::source),
            Codec.FLOAT.fieldOf("volume").forGetter(Sound::volume),
            Codec.FLOAT.fieldOf("pitch").forGetter(Sound::pitch),
            Codec.LONG.optionalFieldOf("seed").forGetter { sound ->
                Optional.of(sound.seed())
                    .filter(OptionalLong::isPresent)
                    .map(OptionalLong::getAsLong)
            },
        ).apply(instance) { name, source, volume, pitch, seed ->
            Sound.sound()
                .type(name)
                .source(source)
                .volume(volume)
                .pitch(pitch)
                .seed(seed.map(OptionalLong::of).orElseGet(OptionalLong::empty))
                .build()
        }
    }
}