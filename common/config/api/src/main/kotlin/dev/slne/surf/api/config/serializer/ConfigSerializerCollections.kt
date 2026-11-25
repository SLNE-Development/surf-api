package dev.slne.surf.api.config.serializer

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.surfBean
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.serialize.TypeSerializerCollection

/**
 * Builds the serializers of files opened through `ConfigHandles.open`.
 *
 * Implemented by a bean of surf-api's root context.
 */
@InternalSurfApi
interface ConfigSerializerCollections {

    /**
     * Returns the serializers of one file: [serializers] first, then surf-api's root defaults.
     *
     * @param serializers the file's own serializers, each for the type it declares
     * @throws IllegalArgumentException if the type of a serializer cannot be resolved, or two
     * serializers of one level serialize the same type
     */
    fun withRootDefaults(serializers: List<TypeSerializer<*>>): TypeSerializerCollection

    @InternalSurfApi
    companion object {
        /**
         * The [ConfigSerializerCollections] bean of surf-api's root context.
         */
        @InternalSurfApi
        val instance by surfBean<ConfigSerializerCollections>()
    }
}
