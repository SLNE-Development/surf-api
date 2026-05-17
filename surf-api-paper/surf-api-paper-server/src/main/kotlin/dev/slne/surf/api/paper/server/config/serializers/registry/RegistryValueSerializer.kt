package dev.slne.surf.api.paper.server.config.serializers.registry

import io.leangen.geantyref.TypeToken
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import org.spongepowered.configurate.serialize.SerializationException

class RegistryValueSerializer<T : Keyed> : RegistryEntrySerializer<T, T> {
    constructor(type: TypeToken<T>, registryKey: RegistryKey<T>, omitMinecraftNamespace: Boolean) : super(
        type,
        registryKey,
        omitMinecraftNamespace
    )

    constructor(type: Class<T>, registryKey: RegistryKey<T>, omitMinecraftNamespace: Boolean) : super(
        type,
        registryKey,
        omitMinecraftNamespace
    )

    override fun convertFromResourceKey(key: Key): T {
        val value = registry().get(key)
            ?: throw SerializationException("Missing value in $registryKey with string key: ${key.asString()}")

        return value
    }

    override fun convertToResourceKey(value: T): Key {
        return registry().getKeyOrThrow(value)
    }
}