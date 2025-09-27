package dev.slne.surf.surfapi.core.api.serializer.adventure.resourcepack.info

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.slne.surf.surfapi.core.api.serializer.java.uri.JavaURICodec
import dev.slne.surf.surfapi.core.api.serializer.java.uuid.JavaUUIDCodec
import net.kyori.adventure.resource.ResourcePackInfo

object AdventureResourcePackInfoCodec {
    val CODEC: Codec<ResourcePackInfo> = RecordCodecBuilder.create { instance ->
        instance.group(
            JavaUUIDCodec.CODEC.fieldOf("id").forGetter(ResourcePackInfo::id),
            JavaURICodec.CODEC.fieldOf("uri").forGetter(ResourcePackInfo::uri),
            Codec.STRING.fieldOf("hash").forGetter(ResourcePackInfo::hash),
        ).apply(instance) { id, uri, hash ->
            ResourcePackInfo.resourcePackInfo(id, uri, hash)
        }
    }
}