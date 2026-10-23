package dev.slne.surf.nats.gradle.model

import com.squareup.wire.schema.Location
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Reads `[default = "..."]` literals from the source text; wire's unescaped value loses non-ASCII byte values.
 *
 * The literal is located through wire's [Location], which counts lines like wire-schema does, and is only used when
 * decoding it with wire-schema's escape rules yields wire's parsed default. Source texts and their line index are
 * cached per instance, so an instance belongs to one generation or comparison run.
 */
internal class RawDefaults {
    private val sources = HashMap<File, SourceText?>()

    /**
     * Returns the bytes of the default literal of the field declared at [location], or `null` when it cannot be read
     * or does not decode to [expected], the default wire-schema parsed.
     */
    fun bytes(location: Location, expected: String): ByteArray? {
        if (location.line < 1 || location.column < 1) return null
        val source = source(File(location.base, location.path)) ?: return null
        val text = source.text
        val start = source.offset(location.line, location.column) ?: return null
        val defaultStart = findDefault(text, start) ?: return null
        val literal = readStrings(text, defaultStart) ?: return null
        return literal.bytes.takeIf { literal.wireValue == expected }
    }

    private fun source(file: File): SourceText? {
        if (file in sources) return sources[file]
        return (if (file.isFile) SourceText(file.readText(Charsets.UTF_8)) else null).also { sources[file] = it }
    }

    private class SourceText(val text: String) {
        private val lineStarts: List<Int> by lazy(LazyThreadSafetyMode.NONE) { lineStarts(text) }

        /**
         * Returns the index of wire's [line]:[column], or `null` when the text before that line cannot be read.
         */
        fun offset(line: Int, column: Int): Int? {
            val starts = lineStarts
            if (line > starts.size) return null
            return (starts[line - 1] + column - 1).takeIf { it <= text.length }
        }
    }

    private class Literal(val bytes: ByteArray, val wireValue: String)

    private fun readStrings(text: String, start: Int): Literal? {
        val out = ByteArrayOutputStream()
        val wire = StringBuilder()
        var index = start
        var count = 0
        while (index < text.length && (text[index] == '"' || text[index] == '\'')) {
            index = decodeString(text, index, out, wire) ?: return null
            count++
            index = skipBlank(text, index)
        }
        return if (count == 0) null else Literal(out.toByteArray(), wire.toString())
    }

    /**
     * Decodes the quoted literal at [start] with protoc escape rules into [out] and with wire-schema's rules into
     * [wire]; returns the index after it.
     */
    private fun decodeString(text: String, start: Int, out: ByteArrayOutputStream, wire: StringBuilder): Int? {
        val quote = text[start]
        val plain = StringBuilder()
        fun flush() {
            if (plain.isNotEmpty()) out.write(plain.toString().encodeToByteArray())
            plain.clear()
        }

        var index = start + 1
        while (index < text.length) {
            val char = text[index]
            when {
                char == quote -> {
                    flush()
                    return index + 1
                }

                char == '\n' -> return null
                char != '\\' -> {
                    plain.append(char)
                    wire.append(char)
                    index++
                }

                else -> {
                    flush()
                    val (wireValue, end) = wireEscape(text, index + 1) ?: return null
                    wire.append(wireValue.toChar())
                    val escape = text[index + 1]
                    if (escape == 'u' || escape == 'U') {
                        val digits = if (escape == 'u') 4 else 8
                        val (codePoint, codeEnd) = number(text, end, 16, digits) ?: return null
                        if (codeEnd - end != digits || !Character.isValidCodePoint(codePoint)) return null
                        out.write(String(Character.toChars(codePoint)).encodeToByteArray())
                        // wire-schema reads the digits of \u and \U as plain characters
                        wire.append(text, end, codeEnd)
                        index = codeEnd
                    } else {
                        // protoc agrees with wire-schema on every other escape; unknown ones stand for the character
                        if (escape in VALUE_ESCAPES) out.write(wireValue and 0xFF) else out.write(escape.toString().encodeToByteArray())
                        index = end
                    }
                }
            }
        }
        return null
    }

    private companion object {
        /**
         * Escapes that stand for a byte value rather than for the escaped character.
         */
        const val VALUE_ESCAPES = "abfnrtvxX01234567"

        /**
         * Returns the start index of every line as wire-schema counts lines, up to the first construct it cannot read.
         *
         * wire-schema also counts every quoted character that decodes to a newline (`\n`, `\012`, `\x0a`) as a line
         * break, and starts the lines inside block comments at their `\n`. Lines reached inside one comment or string
         * share the start of the last of them.
         */
        fun lineStarts(text: String): List<Int> {
            val starts = mutableListOf(0)
            var currentLine = 1
            var lineStart = 0
            var index = 0
            val newline: (Int) -> Unit = { start ->
                currentLine++
                lineStart = start
            }
            while (index < text.length) {
                val lineBefore = currentLine
                when {
                    text[index] == '\n' -> {
                        index++
                        newline(index)
                    }

                    text[index] == '"' || text[index] == '\'' -> index = skipWireString(text, index, newline) ?: break
                    text.startsWith("//", index) -> {
                        val end = text.indexOf('\n', index)
                        if (end < 0) break
                        index = end + 1
                        newline(index)
                    }

                    text.startsWith("/*", index) -> {
                        val end = text.indexOf("*/", index + 2)
                        if (end < 0) break
                        for (position in index + 2 until end) {
                            if (text[position] == '\n') newline(position)
                        }
                        index = end + 2
                    }

                    else -> index++
                }
                repeat(currentLine - lineBefore) { starts += lineStart }
            }
            return starts
        }

        /**
         * Skips the quoted literal at [start] like wire-schema's reader; returns the index after it.
         */
        fun skipWireString(text: String, start: Int, newline: (Int) -> Unit): Int? {
            val quote = text[start]
            var index = start + 1
            while (index < text.length) {
                val char = text[index++]
                when (char) {
                    quote -> return index
                    '\n' -> newline(index)
                    '\\' -> {
                        val (decoded, end) = wireEscape(text, index) ?: return null
                        index = end
                        if (decoded == '\n'.code) newline(index)
                    }
                }
            }
            return null
        }

        /**
         * Returns the index of the literal after `[... default =` in the declaration that starts at [start].
         */
        fun findDefault(text: String, start: Int): Int? {
            var index = start
            var inOptions = false
            while (index < text.length) {
                val char = text[index]
                when {
                    char == ';' -> return null
                    char == '[' -> {
                        inOptions = true
                        index++
                    }
                    char == '"' || char == '\'' -> index = skipString(text, index) ?: return null
                    text.startsWith("//", index) -> index = text.indexOf('\n', index).let { if (it < 0) text.length else it }
                    text.startsWith("/*", index) -> index = text.indexOf("*/", index + 2).let { if (it < 0) return null else it + 2 }
                    inOptions && text.startsWith("default", index) && isWordStart(text, index) && !isWordChar(text.getOrNull(index + 7)) -> {
                        val equals = skipBlank(text, index + 7)
                        if (text.getOrNull(equals) == '=') return skipBlank(text, equals + 1)
                        index = equals
                    }

                    else -> index++
                }
            }
            return null
        }

        /**
         * Decodes the escape whose character after `\` is at [index] like wire-schema: returns the char code and the
         * index after the escape. Unknown escapes stand for the escaped character itself (wire has no `\u`).
         */
        fun wireEscape(text: String, index: Int): Pair<Int, Int>? {
            val escape = text.getOrNull(index) ?: return null
            return when (escape) {
                'a' -> 7 to index + 1
                'b' -> 8 to index + 1
                'f' -> 12 to index + 1
                'n' -> 10 to index + 1
                'r' -> 13 to index + 1
                't' -> 9 to index + 1
                'v' -> 11 to index + 1
                'x', 'X' -> number(text, index + 1, 16, 2)
                in '0'..'7' -> number(text, index, 8, 3)
                else -> escape.code to index + 1
            }
        }

        fun number(text: String, start: Int, radix: Int, maxDigits: Int): Pair<Int, Int>? {
            var index = start
            var value = 0
            while (index < text.length && index - start < maxDigits) {
                val digit = Character.digit(text[index], radix)
                if (digit < 0) break
                value = value * radix + digit
                index++
            }
            return if (index == start) null else value to index
        }

        fun skipString(text: String, start: Int): Int? {
            val quote = text[start]
            var index = start + 1
            while (index < text.length) {
                when (text[index]) {
                    '\\' -> index += 2
                    quote -> return index + 1
                    else -> index++
                }
            }
            return null
        }

        fun skipBlank(text: String, start: Int): Int {
            var index = start
            while (index < text.length && text[index].isWhitespace()) index++
            return index
        }

        fun isWordStart(text: String, index: Int): Boolean = !isWordChar(text.getOrNull(index - 1))

        fun isWordChar(char: Char?): Boolean = char != null && (char.isLetterOrDigit() || char == '_' || char == '.')
    }
}
