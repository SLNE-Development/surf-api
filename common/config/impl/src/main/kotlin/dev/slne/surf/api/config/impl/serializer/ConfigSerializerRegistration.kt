package dev.slne.surf.api.config.impl.serializer

import io.leangen.geantyref.GenericTypeReflector
import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.TypeSerializer
import org.springframework.util.ClassUtils
import java.lang.reflect.*

/**
 * A serializer together with the type it serializes.
 *
 * @property type the serialized type; subtypes match as well
 * @property source names the serializer in failure messages
 */
internal class ConfigSerializerRegistration(
    val type: Type,
    val serializer: TypeSerializer<*>,
    val source: String,
) {

    companion object {

        /**
         * Resolves the type of [serializer]: [declaredType] if given, otherwise the type of a
         * [ScalarSerializer], otherwise the type argument of its [TypeSerializer] supertype.
         *
         * The type argument is resolved with geantyref, the library Configurate matches types
         * with, so generic arguments such as `List<Price>` are kept.
         *
         * @param declaredType the type to serialize, or `null` to resolve it from [serializer]
         * @throws IllegalArgumentException if the type cannot be resolved without type variables
         */
        fun of(
            serializer: TypeSerializer<*>,
            declaredType: Class<*>? = null,
        ): ConfigSerializerRegistration {
            val serializerClass = ClassUtils.getUserClass(serializer)
            val type = declaredType ?: resolveType(serializer, serializerClass)

            return ConfigSerializerRegistration(type, serializer, serializerClass.name)
        }

        private fun resolveType(serializer: TypeSerializer<*>, serializerClass: Class<*>): Type {
            if (serializer is ScalarSerializer<*>) return serializer.type().type

            val type = GenericTypeReflector.getTypeParameter(
                serializerClass,
                TypeSerializer::class.java.typeParameters.single(),
            )
            require(type != null && !type.hasTypeVariables()) {
                "Cannot resolve the type that config serializer ${serializerClass.name} " +
                    "serializes from its TypeSerializer supertype; declare it with " +
                    "@SurfConfigSerializer(type = ...)"
            }

            return type
        }

        private fun Type.hasTypeVariables(): Boolean {
            return when (this) {
                is TypeVariable<*> -> true
                is ParameterizedType -> actualTypeArguments.any { it.hasTypeVariables() }
                is GenericArrayType -> genericComponentType.hasTypeVariables()
                is WildcardType -> (upperBounds + lowerBounds).any { it.hasTypeVariables() }
                else -> false
            }
        }
    }
}
