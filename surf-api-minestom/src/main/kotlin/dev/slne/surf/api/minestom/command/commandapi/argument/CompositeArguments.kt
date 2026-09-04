/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command.commandapi.argument

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.concurrent.CompletableFuture
import dev.slne.surf.api.minestom.command.commandapi.CommandAPI
import dev.slne.surf.api.minestom.command.commandapi.exception.ComponentMessage
import dev.slne.surf.api.minestom.command.commandapi.exception.WrapperCommandSyntaxException
import net.minestom.server.command.CommandSender
import dev.slne.surf.api.minestom.command.commandapi.exception.CommandSyntaxException as ApiCommandSyntaxException

data class CustomArgumentInfo<B>(
    val sender: CommandSender,
    val currentInput: String,
    val baseValue: B,
)

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
 * Parses a value with [baseType], then hands it to [parser] wrapped in a [CustomArgumentInfo]
 * carrying the sender, the exact text [baseType] consumed and the parsed base value. [parser]
 * never sees the underlying [StringReader]; it composes on top of the already-parsed base value.
 *
 * A command source is required to build that info even when [parser] does not read it, so the
 * source-less overload is rejected rather than fabricating one.
 *
 * A [CommandSyntaxException] thrown by [parser] passes through unchanged. This project's own
 * [ApiCommandSyntaxException] - directly, or unwrapped from a [WrapperCommandSyntaxException] - is
 * translated into one instead, with the reader's cursor reset to where this value started.
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

    /** A custom argument reads exactly what its base reads, so it completes the same values. */
    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> = baseType.listSuggestions(context, builder)
}

/**
 * Rewrites this project's syntax failure as the Brigadier one the dispatcher expects, keeping the
 * styled component so the sender sees the message the argument wrote rather than its plain form.
 */
private fun ApiCommandSyntaxException.toBrigadier(reader: StringReader): CommandSyntaxException {
    val message = component
        ?.let(::ComponentMessage)
        ?: LiteralMessage(this.message ?: "Invalid argument")

    return SimpleCommandExceptionType(message).createWithContext(reader)
}

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
 * Reads elements with [elementType], separated by [delimiter] (whitespace around the delimiter,
 * and leading or trailing whitespace around the whole list, is skipped). Rejects an empty list
 * unless [allowEmpty] permits it.
 *
 * Also rejects an individual element that consumed no input at all - a leading, trailing, or
 * doubled-up delimiter - unless [allowEmpty] permits it. This is checked here rather than left to
 * [elementType], since some element types (a bare word, for instance) would otherwise accept an
 * empty token silently instead of rejecting it.
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
     * Completes the element being typed, not the list: the offered entries replace the text after
     * the last delimiter, so accepting one leaves the elements before it alone.
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

fun <T, R> Argument<T>.map(mapper: (T) -> R): Argument<R> =
    CustomArgument(this, parser = { info -> mapper(info.baseValue) })

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
