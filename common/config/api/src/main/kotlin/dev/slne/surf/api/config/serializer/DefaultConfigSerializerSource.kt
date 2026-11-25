package dev.slne.surf.api.config.serializer

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import org.spongepowered.configurate.serialize.TypeSerializer

/**
 * Supplies root default serializers that cannot each be a [SurfConfigSerializer] bean, such as a
 * family of serializers built from runtime data.
 *
 * Only beans of surf-api's root context are read. Each serializer joins the root level like an
 * `applyToAll` [SurfConfigSerializer] and applies to the type that its `TypeSerializer` supertype
 * or `ScalarSerializer.type()` declares.
 */
@InternalSurfApi
fun interface DefaultConfigSerializerSource {

    /**
     * Returns the serializers to add; called again for every file that is opened.
     */
    fun serializers(): List<TypeSerializer<*>>
}
