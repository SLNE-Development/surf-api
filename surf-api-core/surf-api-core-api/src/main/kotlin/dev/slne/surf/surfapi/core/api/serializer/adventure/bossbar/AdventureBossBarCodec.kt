package dev.slne.surf.surfapi.core.api.serializer.adventure.bossbar

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.slne.surf.surfapi.core.api.serializer.adventure.component.AdventureComponentCodec
import net.kyori.adventure.bossbar.BossBar

object AdventureBossBarCodec {
    val COLOR_CODEC: Codec<BossBar.Color> = Codec.STRING.xmap(
        { name -> BossBar.Color.valueOf(name.uppercase()) },
        { color -> color.name.lowercase() }
    )

    val OVERLAY_CODEC: Codec<BossBar.Overlay> = Codec.STRING.xmap(
        { name -> BossBar.Overlay.valueOf(name.uppercase()) },
        { overlay -> overlay.name.lowercase() }
    )

    val FLAG_CODEC: Codec<BossBar.Flag> = Codec.STRING.xmap(
        { name -> BossBar.Flag.valueOf(name.uppercase()) },
        { flag -> flag.name.lowercase() }
    )

    val CODEC: Codec<BossBar> = RecordCodecBuilder.create { instance ->
        instance.group(
            AdventureComponentCodec.CODEC.fieldOf("name").forGetter(BossBar::name),
            Codec.FLOAT.fieldOf("progress").forGetter(BossBar::progress),
            COLOR_CODEC.fieldOf("color").forGetter(BossBar::color),
            OVERLAY_CODEC.fieldOf("overlay").forGetter(BossBar::overlay),
            FLAG_CODEC.listOf().optionalFieldOf("flags", emptyList())
                .forGetter { it.flags().toList() }
        ).apply(instance) { name, progress, color, overlay, flags ->
            BossBar.bossBar(name, progress, color, overlay, flags.toSet())
        }
    }
}