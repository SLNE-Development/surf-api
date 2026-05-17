package dev.slne.surf.api.core.config.serializer.collection.map

import io.leangen.geantyref.TypeToken
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.spongepowered.configurate.BasicConfigurationNode
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import java.lang.reflect.AnnotatedParameterizedType
import java.lang.reflect.AnnotatedType
import java.lang.reflect.Type

/**
 * Fault-tolerant Configurate serializer for maps.
 *
 * Invalid individual entries are logged and skipped instead of failing the entire map.
 * If [ThrowExceptions] is present on the map type, this serializer delegates to Configurate's
 * default map serializer and preserves the default exception behavior.
 *
 * When [clearInvalids] is enabled, keys that are present in the existing node but not written
 * by the current serialized map are removed during serialization.
 *
 * @param clearInvalids Whether stale keys should be removed from the configuration node
 * during serialization.
 */
internal class MapSerializer(
    private val clearInvalids: Boolean
) : TypeSerializer.Annotated<Map<*, *>> {
    companion object {
        private val logger = ComponentLogger.logger()

        /**
         * Type token used to register this serializer for all map types.
         */
        val TYPE = object : TypeToken<Map<*, *>>() {}
    }
    private val fallback = requireNotNull(TypeSerializerCollection.defaults().get(TYPE)) {
        "Could not find default Map<?, ?> serializer"
    }

    /**
     * Deserializes a map while skipping entries whose key or value cannot be deserialized.
     *
     * If [ThrowExceptions] is present on [type], the default Configurate map serializer is used.
     */
    override fun deserialize(type: AnnotatedType, node: ConfigurationNode): Map<*, *> {
        if (type.isAnnotationPresent(ThrowExceptions::class.java)) {
            return fallback.deserialize(type, node)
        }

        val map = linkedMapOf<Any, Any>()
        val rawType = type.type

        if (!node.isMap) {
            return map
        }

        if (type !is AnnotatedParameterizedType) {
            throw SerializationException(rawType, "Raw types are not supported for collections")
        }

        val args = type.annotatedActualTypeArguments
        if (args.size != 2) {
            throw SerializationException(rawType, "Map expected two type arguments!")
        }

        val keyType = args[0]
        val valueType = args[1]

        val keySerializer = node.options().serializers().get(keyType)
            ?: throw SerializationException(rawType, "No type serializer available for key type $keyType")

        val valueSerializer = node.options().serializers().get(valueType)
            ?: throw SerializationException(rawType, "No type serializer available for value type $valueType")

        val writeKeyBack = keyType.isAnnotationPresent(WriteKeyBack::class.java)
        val keyNode = BasicConfigurationNode.root(node.options())
        val keysToClear = mutableSetOf<Any>()

        for ((rawKey, valueNode) in node.childrenMap()) {
            val deserializedKey = deserializePart(
                keyType.type,
                keySerializer,
                "key",
                keyNode.set(rawKey),
                node.path()
            )

            val deserializedValue = deserializePart(
                valueType.type,
                valueSerializer,
                "value",
                valueNode,
                valueNode.path()
            )

            if (deserializedKey == null || deserializedValue == null) {
                continue
            }

            if (writeKeyBack) {
                val shouldKeep = serializePart(
                    keyType.type,
                    keySerializer,
                    deserializedKey,
                    "key",
                    keyNode,
                    node.path()
                )

                val writtenKey = requireNotNull(keyNode.raw()) { "Key must not be null!" }

                if (shouldKeep && rawKey != writtenKey) {
                    keysToClear += rawKey
                }
            }

            map[deserializedKey] = deserializedValue
        }


        if (writeKeyBack) {
            for (keyToClear in keysToClear) {
                node.node(keyToClear).raw(null)
            }
        }

        return map
    }

    /**
     * Serializes a map into [node].
     *
     * If [clearInvalids] is enabled, existing child nodes that were not visited while writing
     * the current map are removed.
     */
    override fun serialize(type: AnnotatedType, obj: Map<*, *>?, node: ConfigurationNode) {
        if (type.isAnnotationPresent(ThrowExceptions::class.java)) {
            fallback.serialize(type, obj, node)
            return
        }

        val rawType = type.type

        if (type !is AnnotatedParameterizedType) {
            throw SerializationException(rawType, "Raw types are not supported for collections")
        }

        val args = type.annotatedActualTypeArguments
        if (args.size != 2) {
            throw SerializationException(rawType, "Map expected two type arguments!")
        }

        val keyType = args[0]
        val valueType = args[1]

        val keySerializer = node.options().serializers().get(keyType)
            ?: throw SerializationException(rawType, "No type serializer available for key type $keyType")

        val valueSerializer = node.options().serializers().get(valueType)
            ?: throw SerializationException(rawType, "No type serializer available for value type $valueType")

        if (obj.isNullOrEmpty()) {
            node.set(emptyMap<Any, Any>())
            return
        }

        val unvisitedKeys = if (node.empty()) {
            node.raw(emptyMap<Any, Any>())
            mutableSetOf()
        } else {
            node.childrenMap().keys.toMutableSet()
        }

        val keyNode = BasicConfigurationNode.root(node.options())

        for ((key, value) in obj) {
            if (!serializePart(keyType.type, keySerializer, key, "key", keyNode, node.path())) {
                continue
            }

            val keyObj = requireNotNull(keyNode.raw()) { "Key must not be null!" }
            val child = node.node(keyObj)

            serializePart(valueType.type, valueSerializer, value, "value", child, child.path())

            unvisitedKeys -= keyObj
        }

        if (clearInvalids) {
            for (unusedChild in unvisitedKeys) {
                node.removeChild(unusedChild)
            }
        }
    }

    /**
     * Returns an empty linked map for missing map values.
     */
    override fun emptyValue(type: AnnotatedType, options: ConfigurationOptions): Map<*, *> {
        if (type.isAnnotationPresent(ThrowExceptions::class.java)) {
            return fallback.emptyValue(type, options) ?: linkedMapOf<Any, Any>()
        }

        return linkedMapOf<Any, Any>()
    }

    private fun deserializePart(
        type: Type,
        serializer: TypeSerializer<*>,
        mapPart: String,
        node: ConfigurationNode,
        path: Any
    ): Any? {
        return try {
            serializer.deserialize(type, node)
        } catch (e: SerializationException) {
            e.initPath(node::path)
            logger.error(
                "Could not deserialize {} {} into {} at {}: {}",
                mapPart,
                node.raw(),
                type,
                path,
                e.rawMessage()
            )
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun serializePart(
        type: Type,
        serializer: TypeSerializer<*>,
        obj: Any?,
        mapPart: String,
        node: ConfigurationNode,
        path: Any
    ): Boolean {
        return try {
            (serializer as TypeSerializer<Any?>).serialize(type, obj, node)
            true
        } catch (e: SerializationException) {
            e.initPath(node::path)
            logger.error(
                "Could not serialize {} {} from {} at {}: {}",
                mapPart,
                obj,
                type,
                path,
                e.rawMessage()
            )
            false
        }
    }
}