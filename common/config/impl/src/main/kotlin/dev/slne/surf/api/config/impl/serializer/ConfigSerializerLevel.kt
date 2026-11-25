package dev.slne.surf.api.config.impl.serializer

import io.leangen.geantyref.GenericTypeReflector
import io.leangen.geantyref.TypeToken
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.serialize.TypeSerializerCollection

/**
 * The serializers of one priority level of a config file, with at most one serializer per type.
 *
 * [registerTo] registers a serializer for a subtype before one for its supertype, so the most
 * specific serializer of the level wins regardless of the order the beans were found in.
 *
 * @param description names the level in failure messages
 * @throws IllegalArgumentException if two serializers serialize the same type
 */
internal class ConfigSerializerLevel(
    description: String,
    registrations: List<ConfigSerializerRegistration>,
) {

    private val ordered: List<ConfigSerializerRegistration>

    init {
        val byType = registrations.groupBy { registration -> registration.type }
        for ((type, duplicates) in byType) {
            require(duplicates.size == 1) {
                "Config serializers ${duplicates.joinToString(" and ") { it.source }} " +
                    "serialize the same type ${type.typeName} in $description"
            }
        }

        ordered = registrations.sortedByDescending { registration ->
            registrations.count { other ->
                other !== registration && GenericTypeReflector.isSuperType(
                    other.type,
                    registration.type,
                )
            }
        }
    }

    /**
     * Registers the serializers of this level with [builder], subtypes first.
     */
    fun registerTo(builder: TypeSerializerCollection.Builder) {
        for (registration in ordered) {
            @Suppress("UNCHECKED_CAST")
            builder.register(
                TypeToken.get(registration.type) as TypeToken<Any>,
                registration.serializer as TypeSerializer<Any>,
            )
        }
    }

    companion object {
        /**
         * A level without serializers.
         */
        val EMPTY = ConfigSerializerLevel("no level", emptyList())
    }
}
