package dev.slne.surf.api.core.config.serializer.collection.map

import io.leangen.geantyref.GenericTypeReflector
import io.leangen.geantyref.TypeFactory
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.AnnotatedParameterizedType
import java.lang.reflect.AnnotatedType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Serializer bridge for fastutil map implementations.
 *
 * Configurate does not need to know how to construct each fastutil map directly. Instead,
 * this serializer maps the fastutil type to a regular [Map] type with boxed primitive
 * key and value types, lets Configurate deserialize that regular map, and then creates
 * the requested fastutil implementation via [factory].
 *
 * Usage:
 * ```kotlin
 * builder.register(
 *     object : TypeToken<Object2IntMap<Any>>() {},
 *     FastutilMapSerializer.SomethingToPrimitive<Object2IntMap<Any>>(
 *         { Object2IntOpenHashMap(it as Map<out Any, Int>) },
 *         Integer.TYPE
 *     )
 * )
 * ```
 *
 * @param M The concrete fastutil map type.
 * @param factory Creates the concrete fastutil map from the deserialized regular map.
 */
abstract class FastutilMapSerializer<M : Map<*, *>>(
    private val factory: (Map<Any, Any>) -> M
) : TypeSerializer.Annotated<M> {

    /**
     * Deserializes the node as a regular boxed [Map] and converts it into the target
     * fastutil map implementation.
     */
    override fun deserialize(type: AnnotatedType, node: ConfigurationNode): M {
        val mapType = createAnnotatedMapType(type as AnnotatedParameterizedType)
        val map = node.get(mapType) as? Map<Any, Any> ?: emptyMap()

        return factory(map)
    }

    /**
     * Serializes the fastutil map through a regular boxed [Map] type.
     *
     * Empty maps are written as `null`.
     */
    override fun serialize(type: AnnotatedType, obj: M?, node: ConfigurationNode) {
        if (obj.isNullOrEmpty()) {
            node.raw(null)
            return
        }

        node.set(createAnnotatedMapType(type as AnnotatedParameterizedType), obj)
    }


    private fun createAnnotatedMapType(type: AnnotatedParameterizedType): AnnotatedType {
        val baseType = createBaseMapType(type.type as ParameterizedType)
        return GenericTypeReflector.annotate(baseType, type.annotations)
    }

    /**
     * Creates the regular boxed [Map] type that Configurate should use internally.
     */
    protected abstract fun createBaseMapType(type: ParameterizedType): Type

    /**
     * Serializer variant for maps with a regular object-like key and a primitive value.
     *
     * Example fastutil types:
     * - `Object2IntMap<K>`
     * - `Reference2LongMap<K>`
     */
    class SomethingToPrimitive<M : Map<*, *>>(
        factory: (Map<Any, Any>) -> M,
        private val primitiveType: Type
    ) : FastutilMapSerializer<M>(factory) {

        /**
         * Creates a `Map<K, BoxedPrimitive>` type.
         */
        override fun createBaseMapType(type: ParameterizedType): Type {
            return TypeFactory.parameterizedClass(
                Map::class.java,
                type.actualTypeArguments[0],
                GenericTypeReflector.box(primitiveType)
            )
        }
    }

    /**
     * Serializer variant for maps with a primitive key and a regular object-like value.
     *
     * Example fastutil types:
     * - `Int2ObjectMap<V>`
     * - `Long2ReferenceMap<V>`
     */
    class PrimitiveToSomething<M : Map<*, *>>(
        factory: (Map<Any, Any>) -> M,
        private val primitiveType: Type
    ) : FastutilMapSerializer<M>(factory) {

        /**
         * Creates a `Map<BoxedPrimitive, V>` type.
         */
        override fun createBaseMapType(type: ParameterizedType): Type {
            return TypeFactory.parameterizedClass(
                Map::class.java,
                GenericTypeReflector.box(primitiveType),
                type.actualTypeArguments[0]
            )
        }
    }

    /**
     * Serializer variant for maps where both key and value are represented by type arguments.
     *
     * Example fastutil types:
     * - `Object2ObjectMap<K, V>`
     * - `Reference2ObjectMap<K, V>`
     */
    class SomethingToSomething<M : Map<*, *>>(
        factory: (Map<Any, Any>) -> M,
    ) : FastutilMapSerializer<M>(factory) {

        /**
         * Creates a `Map<K, V>` type.
         */
        override fun createBaseMapType(type: ParameterizedType): Type {
            return TypeFactory.parameterizedClass(
                Map::class.java,
                type.actualTypeArguments[0],
                type.actualTypeArguments[1]
            )
        }
    }
}