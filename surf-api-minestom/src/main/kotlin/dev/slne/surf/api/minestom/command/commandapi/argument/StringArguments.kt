/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command.commandapi.argument

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import dev.slne.surf.api.minestom.command.commandapi.argument.parser.FixedSetParser
import it.unimi.dsi.fastutil.objects.ObjectList

class StringArgument(nodeName: String) : Argument<String>(nodeName) {
    override val kind = ArgumentKind.Word
    override val rawType: ArgumentType<String> = StringArgumentType.word()
    override fun stringify(value: String): String = value
}

class TextArgument(nodeName: String) : Argument<String>(nodeName) {
    override val kind = ArgumentKind.Text
    override val rawType: ArgumentType<String> = StringArgumentType.string()
    override fun stringify(value: String): String = value.asQuotablePhrase()
}

class GreedyStringArgument(nodeName: String) : Argument<String>(nodeName) {
    override val kind = ArgumentKind.GreedyString
    override val rawType: ArgumentType<String> = StringArgumentType.greedyString()
    override val greedy = true
    override fun stringify(value: String): String = value
}

class LiteralArgument(
    nodeName: String,
    val literal: String = nodeName,
) : Argument<String>(nodeName) {
    override val kind = ArgumentKind.Literal(literal)
    override val rawType: ArgumentType<String> = UnsupportedArgumentType(nodeName)

    init {
        require(literal.isNotBlank()) { "Literal value must not be blank" }
        require(literal.none(Char::isWhitespace)) { "Literal value must be a single token" }
    }

    override fun stringify(value: String): String = value
}

class MultiLiteralArgument(nodeName: String, vararg literals: String) : Argument<String>(nodeName) {
    private val literals: List<String> = ObjectList.of(*literals)
    override val kind = ArgumentKind.MultiLiteral(this.literals)
    override val rawType: ArgumentType<String> = FixedSetParser(this.literals.associateWith { it })

    init {
        require(this.literals.isNotEmpty()) { "Multi-literal argument must contain at least one literal" }
        require(this.literals.all(String::isNotBlank)) { "Literal values must not be blank" }
        require(this.literals.none { literal -> literal.any(Char::isWhitespace) }) {
            "Multi-literal values must be single tokens"
        }
        require(this.literals.distinct().size == this.literals.size) { "Literal values must be unique" }
    }

    override fun stringify(value: String): String = value
}

/**
 * Reads the remainder of the input as a single string, the same way [GreedyStringArgument] does.
 */
class CommandArgument(nodeName: String) : Argument<String>(nodeName) {
    override val kind = ArgumentKind.Command
    override val rawType: ArgumentType<String> = StringArgumentType.greedyString()
    override val greedy = true

    override fun stringify(value: String): String = value
}

private fun String.asQuotablePhrase(): String {
    if (none { character ->
            character.isWhitespace() ||
                    Character.isISOControl(character) ||
                    character == '\'' ||
                    character == '"'
        }
    ) {
        return this
    }

    return buildString(length + 2) {
        append('"')
        this@asQuotablePhrase.forEach { character ->
            append(
                when (character) {
                    '\\' -> "\\\\"
                    '"' -> "\\\""
                    '\b' -> "\\b"
                    '\t' -> "\\t"
                    '\n' -> "\\n"
                    '\u000C' -> "\\f"
                    '\r' -> "\\r"
                    else -> if (Character.isISOControl(character)) {
                        "\\u${character.code.toString(16).padStart(4, '0')}"
                    } else {
                        character
                    }
                },
            )
        }
        append('"')
    }
}
