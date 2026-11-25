package dev.slne.surf.api.minestom.command.executor

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

data class ParsedArgument(
    val name: String,
    val value: Any?,
    val raw: String?,
    val isPresent: Boolean,
)

class CommandArguments private constructor(
    private val ordered: List<ParsedArgument>,
) {
    private val named = ordered.associateBy { argument -> argument.name }

    inline fun <reified T> get(name: String): T = typed(name, T::class, nullable = null is T) as T

    inline fun <reified T> getOptional(name: String): T? {
        return typed(
            name,
            T::class,
            nullable = true,
        ) as T?
    }

    fun getRaw(name: String): String? = named[name]?.raw

    fun getRaw(index: Int): String? = ordered.getOrNull(index)?.raw

    fun rawArguments(): List<String> {
        return ordered.mapNotNull { argument ->
            argument.raw.takeIf { argument.isPresent }
        }
    }

    operator fun get(index: Int): Any? = ordered[index].value

    operator fun contains(name: String): Boolean = named[name]?.isPresent == true

    inline operator fun <reified T> getValue(thisRef: Any?, property: KProperty<*>): T {
        return typed(property.name, T::class, nullable = null is T) as T
    }

    @PublishedApi
    internal fun typed(name: String, expected: KClass<*>, nullable: Boolean): Any? {
        val argument = named[name]
            ?: throw IllegalArgumentException("No argument named '$name' exists")
        if (!argument.isPresent && argument.value == null) {
            if (nullable) return null

            throw IllegalArgumentException("Required argument '$name' is absent")
        }

        val value = argument.value

        if (value == null) {
            if (nullable) return null

            throw IllegalArgumentException(
                "Argument '$name' is null but ${expected.qualifiedName} was required",
            )
        }

        require(expected.isInstance(value)) {
            "Argument '$name' expected ${expected.qualifiedName}, got ${value::class.qualifiedName}"
        }

        return value
    }

    companion object {
        @InternalSurfApi
        fun of(arguments: List<ParsedArgument>): CommandArguments {
            val copy = arguments.toList()
            val names = mutableSetOf<String>()

            for (argument in copy) {
                require(names.add(argument.name)) {
                    "Parsed argument names must be unique"
                }
            }

            return CommandArguments(copy)
        }

        fun empty(): CommandArguments = CommandArguments(emptyList())
    }
}
