package dev.slne.surf.api.paper.server.config.serializers.registry

import io.leangen.geantyref.TypeToken
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.InvalidKeyException
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import org.bukkit.Registry
import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.AnnotatedType
import java.util.function.Predicate

abstract class RegistryEntrySerializer<T, R : Keyed> : ScalarSerializer.Annotated<T> {

    private val registryKey: RegistryKey<R>
    private val omitMinecraftNamespace: Boolean

    protected constructor(
        type: TypeToken<T>,
        registryKey: RegistryKey<R>,
        omitMinecraftNamespace: Boolean
    ) : super(type) {
        this.registryKey = registryKey
        this.omitMinecraftNamespace = omitMinecraftNamespace
    }

    protected constructor(
        type: Class<T>,
        registryKey: RegistryKey<R>,
        omitMinecraftNamespace: Boolean
    ) : super(type) {
        this.registryKey = registryKey
        this.omitMinecraftNamespace = omitMinecraftNamespace
    }

    protected fun registry(): Registry<R> {
        return RegistryAccess.registryAccess().getRegistry(registryKey)
    }

    protected abstract fun convertFromResourceKey(key: Key): T

    override fun deserialize(type: AnnotatedType?, obj: Any): T? {
        return convertFromResourceKey(deserializeKey(obj))
    }

    protected abstract fun convertToResourceKey(value: T): Key

    override fun serialize(type: AnnotatedType, item: T, typeSupported: Predicate<Class<*>>): Any {
        val key = this.convertToResourceKey(item)
        return if (this.omitMinecraftNamespace && key.key().namespace() == Key.MINECRAFT_NAMESPACE) {
            key.key().value()
        } else {
            key.asString()
        }
    }

    private fun deserializeKey(input: Any): Key {
        return try {
            Key.key(input.toString())
        } catch (e: InvalidKeyException) {
            throw SerializationException(Key::class.java, "Could not create a key from input", e)
        }
    }
}