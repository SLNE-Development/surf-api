package dev.slne.surf.nats.gradle.model

import com.squareup.wire.schema.Location
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Recovers the original bytes of Protobuf string and bytes defaults from source files.
 *
 * Wire's parsed default may lose the distinction between UTF-8 source characters and
 * escaped byte values. A literal is accepted only when decoding it according to
 * wire-schema's rules reproduces the expected parsed value. Source text and line
 * offsets are cached for the lifetime of this instance.
 */
internal class RawDefaults {
    private val sources = HashMap<File, SourceText?>()

    /**
     * Reads the bytes of a field's default literal at [location].
     *
     * @param location the field's wire-schema source location
     * @param expected the default value parsed by wire-schema
     * @return the original bytes, or `null` if the literal is missing, invalid, or differs from [expected]
     */
    fun bytes(location: Location, expected: String): ByteArray? {
        if (location.line < 1 || location.column < 1) return null

        val source = source(File(location.base, location.path)) ?: return null
        val start = source.offset(location.line, location.column) ?: return null
        val defaultStart = findDefault(source.text, start) ?: return null
        val literal = readStrings(source.text, defaultStart) ?: return null

        return literal.bytes.takeIf { literal.wireValue == expected }
    }

    /**
     * Loads and caches a source file, including absent files.
     *
     * @param file the Protobuf source file
     * @return its cached contents, or `null` if the file does not exist
     */
    private fun source(file: File): SourceText? {
        if (file in sources) return sources[file]
        return (if (file.isFile) SourceText(file.readText(Charsets.UTF_8)) else null)
            .also { sources[file] = it }
    }

    /**
     * Source text with lazily calculated line offsets matching wire-schema's line counting.
     *
     * @property text the contents of the Protobuf source file
     */
    private class SourceText(val text: String) {
        private val lineStarts: List<Int> by lazy(LazyThreadSafetyMode.NONE) { lineStarts(text) }

        /**
         * Converts a wire-schema line and column to a source-text offset.
         *
         * @param line the one-based line number
         * @param column the one-based column number
         * @return the character offset, or `null` if it lies beyond the indexed source
         */
        fun offset(line: Int, column: Int): Int? {
            val starts = lineStarts
            if (line > starts.size) return null
            return (starts[line - 1] + column - 1).takeIf { it <= text.length }
        }
    }

    /**
     * Holds both interpretations of consecutive quoted Protobuf string literals.
     *
     * @property bytes the bytes produced using protoc escape semantics
     * @property wireValue the string produced using wire-schema escape semantics
     */
    private class Literal(val bytes: ByteArray, val wireValue: String)

    /**
     * Decodes one or more adjacent quoted literals separated by whitespace.
     *
     * @param text the Protobuf source text
     * @param start the offset of the first opening quote
     * @return the combined literal, or `null` if no complete quoted literal can be read
     */
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
     * Decodes one quoted literal into its protoc bytes and wire-schema string value.
     *
     * Unescaped text is UTF-8 encoded in batches; escaped bytes are written directly.
     *
     * @param text the Protobuf source text
     * @param start the offset of the opening quote
     * @param out destination for protoc-decoded bytes
     * @param wire destination for the wire-schema-decoded value
     * @return the offset after the closing quote, or `null` for an incomplete literal
     */
    private fun decodeString(
        text: String,
        start: Int,
        out: ByteArrayOutputStream,
        wire: StringBuilder
    ): Int? {
        val quote = text[start]
        val plain = StringBuilder()
        var index = start + 1

        while (index < text.length) {
            val char = text[index]
            when {
                char == quote -> {
                    flushPlain(plain, out)
                    return index + 1
                }

                char == '\n' -> return null
                char != '\\' -> {
                    plain.append(char)
                    wire.append(char)
                    index++
                }

                else -> {
                    flushPlain(plain, out)
                    index = decodeEscape(text, index, out, wire) ?: return null
                }
            }
        }

        return null
    }

    /**
     * Writes pending unescaped text as UTF-8 bytes and resets the buffer.
     *
     * @param plain accumulated unescaped source characters
     * @param out destination for their UTF-8 bytes
     */
    private fun flushPlain(plain: StringBuilder, out: ByteArrayOutputStream) {
        if (plain.isNotEmpty()) out.write(plain.toString().encodeToByteArray())
        plain.clear()
    }

    /**
     * Decodes one escape into the protoc byte stream and wire-schema string.
     *
     * Unicode escapes are handled separately because wire-schema treats their
     * hexadecimal digits as plain characters, unlike protoc.
     *
     * @param text the Protobuf source text
     * @param start the offset of the backslash
     * @param out destination for protoc-decoded bytes
     * @param wire destination for the wire-schema-decoded value
     * @return the offset after the escape, or `null` if decoding fails
     */
    private fun decodeEscape(
        text: String,
        start: Int,
        out: ByteArrayOutputStream,
        wire: StringBuilder
    ): Int? {
        val (wireValue, end) = wireEscape(text, start + 1) ?: return null
        wire.append(wireValue.toChar())

        val escape = text[start + 1]
        if (escape == 'u' || escape == 'U') {
            return decodeUnicodeEscape(text, end, escape, out, wire)
        }

        // Other known escapes use the wire-schema value; unknown escapes use the character itself.
        if (escape in VALUE_ESCAPES) {
            out.write(wireValue and 0xFF)
        } else {
            out.write(escape.toString().encodeToByteArray())
        }
        return end
    }

    /**
     * Decodes the digits following a protoc Unicode escape.
     *
     * Appends the UTF-8 representation of the code point to [out], but leaves
     * the digits visible in [wire], matching wire-schema's interpretation.
     *
     * @param text the Protobuf source text
     * @param start the offset of the first hexadecimal digit
     * @param escape `u` for four digits or `U` for eight digits
     * @param out destination for UTF-8 bytes
     * @param wire destination for the wire-schema-decoded value
     * @return the offset after the digits, or `null` if they are invalid
     */
    private fun decodeUnicodeEscape(
        text: String,
        start: Int,
        escape: Char,
        out: ByteArrayOutputStream,
        wire: StringBuilder,
    ): Int? {
        val digits = if (escape == 'u') 4 else 8
        val (codePoint, end) = number(text, start, 16, digits) ?: return null
        if (end - start != digits || !Character.isValidCodePoint(codePoint)) return null

        out.write(String(Character.toChars(codePoint)).encodeToByteArray())
        wire.append(text, start, end)
        return end
    }

    private companion object {
        /** Escape characters interpreted as byte values rather than literal characters. */
        const val VALUE_ESCAPES = "abfnrtvxX01234567"

        /**
         * Computes line starts using wire-schema's line-counting behavior.
         *
         * Escaped newlines in strings and newlines inside block comments also
         * advance the line count. When multiple newlines occur inside one such
         * construct, all affected entries use its last recorded line start.
         *
         * @param text the Protobuf source text
         * @return offsets for lines encountered before scanning fails or reaches the end
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
                index = when {
                    text[index] == '\n' -> {
                        newline(index + 1)
                        index + 1
                    }

                    text[index] == '"' || text[index] == '\'' -> skipWireString(
                        text,
                        index,
                        newline
                    ) ?: break

                    text.startsWith("//", index) -> skipWireLineComment(text, index, newline)
                        ?: break

                    text.startsWith("/*", index) -> skipWireBlockComment(text, index, newline)
                        ?: break

                    else -> index + 1
                }
                repeat(currentLine - lineBefore) { starts += lineStart }
            }

            return starts
        }

        /**
         * Skips a quoted string while counting newlines as wire-schema does.
         *
         * @param text the Protobuf source text
         * @param start the offset of the opening quote
         * @param newline callback receiving the new line's offset
         * @return the offset after the closing quote, or `null` if it is missing
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
         * Skips a line comment and records its terminating newline.
         *
         * @param text the Protobuf source text
         * @param start the offset of the `//` marker
         * @param newline callback receiving the offset after the newline
         * @return the offset after the newline, or `null` if none exists
         */
        fun skipWireLineComment(text: String, start: Int, newline: (Int) -> Unit): Int? {
            val end = text.indexOf('\n', start)
            if (end < 0) return null
            val next = end + 1
            newline(next)
            return next
        }

        /**
         * Skips a block comment and records its internal newlines.
         *
         * For compatibility with wire-schema, line starts inside block comments
         * are recorded at the newline character rather than after it.
         *
         * @param text the Protobuf source text
         * @param start the offset of the `/*` marker
         * @param newline callback receiving each newline's offset
         * @return the offset after `*/`, or `null` if the comment is unterminated
         */
        fun skipWireBlockComment(text: String, start: Int, newline: (Int) -> Unit): Int? {
            val end = text.indexOf("*/", start + 2)
            if (end < 0) return null
            for (position in start + 2 until end) {
                if (text[position] == '\n') newline(position)
            }
            return end + 2
        }

        /**
         * Finds the value of a `default =` field option starting at [start].
         *
         * Ignores quoted text and comments while scanning and stops at a
         * semicolon or an unterminated construct.
         *
         * @param text the Protobuf source text
         * @param start the offset of the field declaration
         * @return the offset after `default =` and whitespace, or `null` if absent
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
                    text.startsWith("//", index) -> {
                        index = text.indexOf('\n', index).let { if (it < 0) text.length else it }
                    }

                    text.startsWith("/*", index) -> {
                        val end = text.indexOf("*/", index + 2)
                        index = if (end < 0) return null else end + 2
                    }

                    inOptions && isDefaultKeyword(text, index) -> {
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
         * Checks whether `default` starts at a complete identifier boundary.
         *
         * @param text the Protobuf source text
         * @param index the candidate keyword offset
         * @return `true` if the keyword is not part of a larger identifier
         */
        fun isDefaultKeyword(text: String, index: Int): Boolean =
            text.startsWith("default", index) && isWordStart(
                text,
                index
            ) && !isWordChar(text.getOrNull(index + 7))

        /**
         * Decodes one escape using wire-schema's escape rules.
         *
         * Unknown escapes evaluate to the escaped character. Unicode escapes
         * are not recognized as special sequences by wire-schema.
         *
         * @param text the Protobuf source text
         * @param index the offset immediately after the backslash
         * @return the decoded character code and following offset, or `null` if incomplete
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

        /**
         * Parses up to [maxDigits] digits in the given [radix].
         *
         * @param text the source text containing the digits
         * @param start the first digit's offset
         * @param radix the numeric base
         * @param maxDigits the maximum number of digits to consume
         * @return the parsed integer and next offset, or `null` if no digits were read
         */
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

        /**
         * Skips a quoted source string without decoding its value.
         *
         * @param text the Protobuf source text
         * @param start the offset of the opening quote
         * @return the offset after the closing quote, or `null` if unterminated
         */
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

        /**
         * Skips whitespace from a source-text offset.
         *
         * @param text the Protobuf source text
         * @param start the first offset to inspect
         * @return the offset of the next non-whitespace character or the end of the text
         */
        fun skipBlank(text: String, start: Int): Int {
            var index = start
            while (index < text.length && text[index].isWhitespace()) index++
            return index
        }

        /**
         * Checks that an identifier does not continue immediately before [index].
         *
         * @param text the Protobuf source text
         * @param index the candidate identifier's starting offset
         * @return `true` if the preceding character is not part of an identifier
         */
        fun isWordStart(text: String, index: Int): Boolean = !isWordChar(text.getOrNull(index - 1))

        /**
         * Checks whether a character belongs to a Protobuf option identifier.
         *
         * @param char the character to check, or `null` at a text boundary
         * @return `true` for letters, digits, underscores, and periods
         */
        fun isWordChar(char: Char?): Boolean =
            char != null && (char.isLetterOrDigit() || char == '_' || char == '.')
    }
}
