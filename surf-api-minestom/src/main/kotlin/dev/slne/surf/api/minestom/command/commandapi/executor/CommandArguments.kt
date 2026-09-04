package dev.slne.surf.api.minestom.command.commandapi.executor

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.jetbrains.annotations.ApiStatus
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

data class ParsedArgument(
    val name: String,
    val value: Any?,
    val raw: String?,
    val present: Boolean,
)

class CommandArguments private constructor(
    private val ordered: List<ParsedArgument>,
) {
    private val named = Object2ObjectOpenHashMap<String, ParsedArgument>(ordered.size).apply {
        ordered.forEach { argument ->
            put(argument.name, argument)
        }
    }

    inline fun <reified T> get(name: String): T = typed(name, T::class, nullable = false) as T

    inline fun <reified T> getOptional(name: String): T? =
        typed(name, T::class, nullable = true) as T?

    fun getRaw(name: String): String? = named[name]?.raw

    fun getRaw(index: Int): String? = ordered.getOrNull(index)?.raw

    fun rawArguments(): List<String> {
        val rawArguments = ObjectArrayList<String>(ordered.size)

        ordered.forEach { argument ->
            if (argument.present) {
                argument.raw?.let(rawArguments::add)
            }
        }

        return rawArguments
    }

    operator fun get(index: Int): Any? = ordered[index].value

    operator fun contains(name: String): Boolean = named[name]?.present == true

    inline operator fun <reified T> getValue(thisRef: Any?, property: KProperty<*>): T =
        typed(property.name, T::class, nullable = null is T) as T

    @PublishedApi
    internal fun typed(name: String, expected: KClass<*>, nullable: Boolean): Any? {
        val argument = named[name]
            ?: throw IllegalArgumentException("No argument named '$name' exists")
        if (!argument.present && argument.value == null) {
            if (nullable) return null
            throw IllegalArgumentException("Required argument '$name' is absent")
        }
        val value = argument.value
        if (value == null) {
            if (nullable) return null
            throw IllegalArgumentException("Argument '$name' is null but ${expected.qualifiedName} was required")
        }
        require(expected.isInstance(value)) {
            "Argument '$name' expected ${expected.qualifiedName}, got ${value::class.qualifiedName}"
        }
        return value
    }

    companion object {
        @ApiStatus.Internal
        fun of(arguments: List<ParsedArgument>): CommandArguments {
            val copy = ObjectArrayList(arguments)
            val names = ObjectOpenHashSet<String>(copy.size)

            copy.forEach { argument ->
                require(names.add(argument.name)) {
                    "Parsed argument names must be unique"
                }
            }

            return CommandArguments(copy)
        }

        fun empty(): CommandArguments = CommandArguments(emptyList())
    }
}
