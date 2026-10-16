/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command.argument

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.exception.ComponentMessage
import dev.slne.surf.api.minestom.command.exception.WrapperCommandSyntaxException
import net.minestom.server.command.CommandSender
import java.util.concurrent.CompletableFuture
import dev.slne.surf.api.minestom.command.exception.CommandSyntaxException as ApiCommandSyntaxException

/**
 * Input of a [CustomArgument] parser.
 *
 * @property sender the sender running the command
 * @property currentInput the text consumed by the base argument
 * @property baseValue the value the base argument parsed from [currentInput]
 */
data class CustomArgumentInfo<B>(
    val sender: CommandSender,
    val currentInput: String,
    val baseValue: B,
)

/**
 * Argument that parses its input with [base] and converts the result to [T] with a parser.
 *
 * The parser reports invalid input by throwing a [CommandSyntaxException], an
 * [ApiCommandSyntaxException] (for example through [CommandAPI.failWithMessage]) or a
 * [WrapperCommandSyntaxException]; the failure is shown at the start of the argument. Parsing needs
 * a command source, so the argument cannot be parsed without one.
 *
 * The optional flag, default value, permissions and requirements of [base] carry over; a default
 * value is passed through the parser with an empty [CustomArgumentInfo.currentInput]. Suggestions
 * come from the raw type of [base]; suggestion providers set on [base] are not inherited.
 * Values are stringified with `toString()`.
 *
 * @param base argument that reads the raw value
 * @param parser converts the base value to [T]
 */
open class CustomArgument<T, B> internal constructor(
    private val base: Argument<B>,
    private val formatter: (T) -> String,
    private val parser: (CustomArgumentInfo<B>) -> T,
) : Argument<T>(base.nodeName) {
    constructor(
        base: Argument<B>,
        parser: (CustomArgumentInfo<B>) -> T,
    ) : this(base, { value -> value.toString() }, parser)

    init {
        val baseDefinition = base.toDefinition()
        val defaultValue = baseDefinition.defaultValue
        when {
            defaultValue != null -> setOptional { sender ->
                parser(CustomArgumentInfo(sender, "", defaultValue(sender)))
            }

            baseDefinition.optional -> setOptional(optional = true)
        }
        baseDefinition.permissions.forEach(::withPermission)
        baseDefinition.requirements.forEach(::withRequirement)
    }

    override val kind: ArgumentKind<T>
        get() = ArgumentKind.Custom(base.toDefinition(), parser)

    override val rawType: ArgumentType<T> = CustomArgumentType(base.toDefinition().rawType, parser)

    override val greedy: Boolean
        get() = base.toDefinition().greedy

    override val listDelimiter: Char?
        get() = base.toDefinition().listDelimiter

    override fun stringify(value: T): String = formatter(value)
}

private val CUSTOM_ARGUMENT_NEEDS_SENDER = SimpleCommandExceptionType(
    LiteralMessage("A custom argument requires a command source to resolve"),
)

/**
 * Parses a value with [baseType] and converts it with [parser].
 *
 * The overload without a source always fails, because [CustomArgumentInfo] requires a
 * [CommandSender].
 *
 * A Brigadier [CommandSyntaxException] from [parser] propagates unchanged. An
 * [ApiCommandSyntaxException], also when wrapped in a [WrapperCommandSyntaxException], is converted
 * into a Brigadier exception positioned at the start of the value, keeping its component message.
 */
private class CustomArgumentType<T, B>(
    private val baseType: ArgumentType<B>,
    private val parser: (CustomArgumentInfo<B>) -> T,
) : ArgumentType<T> {
    override fun parse(reader: StringReader): T = throw CUSTOM_ARGUMENT_NEEDS_SENDER.createWithContext(reader)

    override fun <S> parse(reader: StringReader, source: S): T {
        val start = reader.cursor
        val sender = source as? CommandSender ?: throw CUSTOM_ARGUMENT_NEEDS_SENDER.createWithContext(reader)

        val baseValue = baseType.parse(reader, source)
        val consumed = reader.string.substring(start, reader.cursor)
        val info = CustomArgumentInfo(sender, consumed, baseValue)

        return try {
            parser(info)
        } catch (failure: CommandSyntaxException) {
            throw failure
        } catch (failure: WrapperCommandSyntaxException) {
            reader.cursor = start
            throw failure.exception.toBrigadier(reader)
        } catch (failure: ApiCommandSyntaxException) {
            reader.cursor = start
            throw failure.toBrigadier(reader)
        }
    }

    /**
     * Delegates to [baseType].
     */
    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> = baseType.listSuggestions(context, builder)
}

/**
 * Converts this exception to a Brigadier exception at the cursor of [reader], keeping the
 * component message when there is one.
 */
private fun ApiCommandSyntaxException.toBrigadier(reader: StringReader): CommandSyntaxException {
    val message = component
        ?.let(::ComponentMessage)
        ?: LiteralMessage(this.message ?: "Invalid argument")

    return SimpleCommandExceptionType(message).createWithContext(reader)
}

/**
 * Delimiter-separated list of values parsed by [element].
 *
 * Whitespace around the delimiter and around the whole list is skipped, so a list can span several
 * space-separated tokens. The argument is greedy and must be the last argument of its path.
 * Values are stringified with the formatter of the element and joined with the delimiter.
 *
 * Without [allowEmpty], an empty list and an empty element, such as a leading, trailing or doubled
 * delimiter, fail with `List must not be empty`. With it, an empty input parses to an empty list.
 *
 * Permissions and requirements of [element] carry over.
 *
 * @param delimiter separator between elements; must be a visible non-whitespace character
 * @throws IllegalArgumentException when [delimiter] is whitespace or the NUL character, or
 * [element] is greedy
 */
class ListArgument<T>(
    nodeName: String,
    private val element: Argument<T>,
    val delimiter: Char = ',',
    val allowEmpty: Boolean = false,
) : Argument<List<T>>(nodeName) {
    init {
        val elementDefinition = element.toDefinition()
        require(!delimiter.isWhitespace() && delimiter != '\u0000') {
            "List delimiter must be a visible non-whitespace character"
        }
        require(!elementDefinition.greedy) { "List elements cannot consume remaining input" }
        elementDefinition.permissions.forEach(::withPermission)
        elementDefinition.requirements.forEach(::withRequirement)
    }

    override val kind: ArgumentKind<List<T>>
        get() = ArgumentKind.List(element.toDefinition(), delimiter, allowEmpty)

    override val rawType: ArgumentType<List<T>> =
        ListArgumentType(element.toDefinition().rawType, delimiter, allowEmpty)

    override val greedy = true

    override val listDelimiter: Char = delimiter

    override fun stringify(value: List<T>): String {
        val elementDefinition = element.toDefinition()
        return value.joinToString(delimiter.toString(), transform = elementDefinition.stringify)
    }
}

private val LIST_ARGUMENT_EMPTY = SimpleCommandExceptionType(LiteralMessage("List must not be empty"))

/**
 * Parses elements with [elementType], separated by [delimiter] and optional spaces.
 *
 * Unless [allowEmpty] is set, an empty list and any element that consumed no input fail with
 * `List must not be empty`. The empty-element check is made here because some element types, such
 * as a bare word, accept an empty token.
 */
private class ListArgumentType<T>(
    private val elementType: ArgumentType<T>,
    private val delimiter: Char,
    private val allowEmpty: Boolean,
) : ArgumentType<List<T>> {
    override fun parse(reader: StringReader): List<T> = parseList(reader) { r -> elementType.parse(r) }

    override fun <S> parse(reader: StringReader, source: S): List<T> =
        parseList(reader) { r -> elementType.parse(r, source) }

    /**
     * Suggests entries for the element after the last delimiter and keeps the earlier elements.
     */
    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val typed = builder.remaining
        val elementStart = typed.lastIndexOf(delimiter) + 1
        val offset = builder.start + elementStart + typed.drop(elementStart).takeWhile(Char::isWhitespace).length

        return elementType.listSuggestions(context, builder.createOffset(offset))
    }

    private inline fun parseList(reader: StringReader, parseElement: (StringReader) -> T): List<T> {
        val start = reader.cursor
        skipWhitespace(reader)
        if (!reader.canRead()) {
            if (allowEmpty) return emptyList()
            reader.cursor = start
            throw LIST_ARGUMENT_EMPTY.createWithContext(reader)
        }

        val values = ArrayList<T>()
        while (true) {
            val elementStart = reader.cursor
            val value = parseElement(reader)
            if (!allowEmpty && reader.cursor == elementStart) {
                reader.cursor = elementStart
                throw LIST_ARGUMENT_EMPTY.createWithContext(reader)
            }
            values += value

            skipWhitespace(reader)
            if (reader.canRead() && reader.peek() == delimiter) {
                reader.skip()
                skipWhitespace(reader)
            } else {
                break
            }
        }
        return values
    }

    private fun skipWhitespace(reader: StringReader) {
        while (reader.canRead() && reader.peek() == ' ') {
            reader.skip()
        }
    }
}

/**
 * Returns an argument that parses like this one and converts the value with [mapper].
 *
 * The result is stringified with `toString()`, which matters for safe suggestions.
 */
fun <T, R> Argument<T>.map(mapper: (T) -> R): Argument<R> =
    CustomArgument(this, parser = { info -> mapper(info.baseValue) })

/**
 * Returns an argument that parses like this one but rejects values for which [predicate] is
 * `false`.
 *
 * A rejected value fails the command with `Invalid value for <nodeName>`.
 */
fun <T> Argument<T>.filter(predicate: (T) -> Boolean): Argument<T> {
    val definition = toDefinition()
    return CustomArgument(
        base = this,
        parser = { info ->
            if (!predicate(info.baseValue)) {
                CommandAPI.failWithString("Invalid value for $nodeName")
            }
            info.baseValue
        },
        formatter = definition.stringify,
    )
}
