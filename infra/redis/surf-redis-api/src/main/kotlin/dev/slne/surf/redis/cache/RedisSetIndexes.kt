package dev.slne.surf.redis.cache

import dev.slne.surf.redis.util.InternalRedisAPI
import it.unimi.dsi.fastutil.objects.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class RedisSetIndex<T : Any, V : Any> internal constructor(
    val name: String,
    private val valuesOf: (T) -> Iterable<V>,
    private val valueToString: (V) -> String,
    private val normalize: (String) -> String
) {
    @InternalRedisAPI
    fun extractStringsSequence(element: T): Sequence<String> = valuesOf(element).asSequence()
        .map(valueToString)
        .map(normalize)
        .filter { it.isNotEmpty() }

    @InternalRedisAPI
    fun extractStrings(element: T): Set<String> = extractStringsSequence(element).toSet()

    @InternalRedisAPI
    fun valueString(value: V): String =
        normalize(valueToString(value)).also {
            require(it.isNotEmpty()) { "Index '$name' produced blank key for value '$value'" }
        }

    override fun toString(): String = "RedisSetIndex(name='$name')"
}

abstract class RedisSetIndexes<T : Any> {
    private val _indices = Object2ObjectLinkedOpenHashMap<String, RedisSetIndex<T, *>>()

    @Volatile
    private var indexByName: Object2ObjectMap<String, RedisSetIndex<T, *>> = Object2ObjectMaps.emptyMap()

    @Volatile
    var all: List<RedisSetIndex<T, *>> = emptyList()
        private set

    @Volatile
    var names: Set<String> = emptySet()
        private set

    @InternalRedisAPI
    fun containsSameInstance(index: RedisSetIndex<T, *>): Boolean {
        return indexByName[index.name] === index
    }

    protected fun <V : Any> index(
        name: String? = null,
        valueToString: (V) -> String = { it.toString() },
        normalize: (String) -> String = { it.trim() },
        valuesOf: (T) -> Iterable<V>
    ): IndexDelegate<V> = IndexDelegate(name, valueToString, normalize, valuesOf)

    protected fun <V : Any> indexOne(
        name: String? = null,
        valueToString: (V) -> String = { it.toString() },
        normalize: (String) -> String = { it.trim() },
        valueOf: (T) -> V?
    ): IndexDelegate<V> = IndexDelegate(name, valueToString, normalize) { t ->
        val v = valueOf(t) ?: return@IndexDelegate emptyList()
        listOf(v)
    }

    protected inner class IndexDelegate<V : Any>(
        private val explicitName: String?,
        private val valueToString: (V) -> String,
        private val normalize: (String) -> String,
        private val valuesOf: (T) -> Iterable<V>
    ) {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, RedisSetIndex<T, V>> {
            val resolvedName = explicitName ?: prop.name
            require(resolvedName.isNotBlank()) { "Index name must not be blank" }
            check(!_indices.containsKey(resolvedName)) {
                "Duplicate index '$resolvedName' in ${this@RedisSetIndexes::class.simpleName}"
            }

            val idx = RedisSetIndex(
                name = resolvedName,
                valuesOf = valuesOf,
                valueToString = valueToString,
                normalize = normalize
            )

            _indices[resolvedName] = idx
            indexByName = Object2ObjectLinkedOpenHashMap(_indices)
            all = ObjectImmutableList(_indices.values)
            names = ObjectLinkedOpenHashSet(_indices.keys)

            return ReadOnlyProperty { _, _ -> idx }
        }
    }

    companion object {
        fun <T : Any> empty(): RedisSetIndexes<T> = object : RedisSetIndexes<T>() {}
    }
}