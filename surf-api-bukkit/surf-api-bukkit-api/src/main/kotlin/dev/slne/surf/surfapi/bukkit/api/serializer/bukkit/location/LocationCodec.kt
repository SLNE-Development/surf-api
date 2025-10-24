package dev.slne.surf.surfapi.bukkit.api.serializer.bukkit.location

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.slne.surf.surfapi.core.api.serializer.java.uuid.JavaUUIDCodec
import org.bukkit.Bukkit
import org.bukkit.Location

object LocationCodec {
    val CODEC: Codec<Location> = RecordCodecBuilder.create { instance ->
        instance.group(
            JavaUUIDCodec.CODEC.optionalFieldOf("worldUuid", null).forGetter { it.world?.uid },
            Codec.DOUBLE.fieldOf("x").forGetter(Location::x),
            Codec.DOUBLE.fieldOf("y").forGetter(Location::y),
            Codec.DOUBLE.fieldOf("z").forGetter(Location::z),
            Codec.FLOAT.fieldOf("yaw").forGetter(Location::getYaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(Location::getPitch),
        ).apply(instance) { worldUuid, x, y, z, yaw, pitch ->
            val world = worldUuid?.let { Bukkit.getWorld(it) }

            Location(world, x, y, z, yaw, pitch)
        }
    }
}