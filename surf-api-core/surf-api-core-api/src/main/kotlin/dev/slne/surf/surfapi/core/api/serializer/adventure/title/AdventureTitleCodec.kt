package dev.slne.surf.surfapi.core.api.serializer.adventure.title

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.slne.surf.surfapi.core.api.serializer.adventure.component.AdventureComponentCodec
import dev.slne.surf.surfapi.core.api.serializer.java.JavaDurationCodec
import net.kyori.adventure.title.Title

object AdventureTitleCodec {
    val TIMES_CODEC: Codec<Title.Times> = RecordCodecBuilder.create { instance ->
        instance.group(
            JavaDurationCodec.CODEC.fieldOf("fade_in").forGetter(Title.Times::fadeIn),
            JavaDurationCodec.CODEC.fieldOf("stay").forGetter(Title.Times::stay),
            JavaDurationCodec.CODEC.fieldOf("fade_out").forGetter(Title.Times::fadeOut)
        ).apply(instance, Title.Times::times)
    }

    val CODEC: Codec<Title> = RecordCodecBuilder.create { instance ->
        instance.group(
            AdventureComponentCodec.CODEC.fieldOf("title").forGetter(Title::title),
            AdventureComponentCodec.CODEC.fieldOf("subtitle").forGetter(Title::subtitle),
            TIMES_CODEC.optionalFieldOf("times", null).forGetter(Title::times)
        ).apply(instance, Title::title)
    }
}