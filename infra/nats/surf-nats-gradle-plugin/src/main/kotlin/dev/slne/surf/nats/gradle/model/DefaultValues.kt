package dev.slne.surf.nats.gradle.model

import com.squareup.kotlinpoet.*
import com.squareup.wire.schema.ProtoType
import dev.slne.surf.nats.gradle.model.DefaultValues.special
import java.math.BigInteger

/**
 * Converts Protobuf default literals into Kotlin source expressions.
 *
 * Supports integer, floating-point, boolean, string, and byte defaults.
 * Numeric literals are validated against their Protobuf type's range
 * and return `null` when they cannot be represented.
 */
internal object DefaultValues {
    private val INT_MIN: BigInteger = Int.MIN_VALUE.toBigInteger()
    private val INT_MAX: BigInteger = Int.MAX_VALUE.toBigInteger()
    private val UINT_MAX: BigInteger = (BigInteger.ONE shl 32) - BigInteger.ONE
    private val LONG_MIN: BigInteger = Long.MIN_VALUE.toBigInteger()
    private val LONG_MAX: BigInteger = Long.MAX_VALUE.toBigInteger()
    private val ULONG_MAX: BigInteger = (BigInteger.ONE shl 64) - BigInteger.ONE

    /**
     * Converts a Protobuf scalar default into a Kotlin expression.
     *
     * Supports signed and unsigned 32-bit and 64-bit integers, floating-point
     * numbers, and booleans. Unsigned integers are represented using their
     * corresponding signed Kotlin bit patterns.
     *
     * @param protoType the Protobuf scalar type name
     * @param literal the default value as declared in the schema
     * @return the Kotlin expression, or `null` if the literal is invalid or unsupported
     */
    fun scalar(protoType: ProtoType, literal: String): CodeBlock? = when (protoType) {
        ProtoType.INT32, ProtoType.SINT32, ProtoType.SFIXED32 -> {
            val parsed = integer(literal, INT_MIN, INT_MAX)
            parsed?.let { int(it.toInt()) }
        }

        ProtoType.UINT32, ProtoType.FIXED32 -> {
            val parsed = integer(literal, BigInteger.ZERO, UINT_MAX)
            parsed?.let { int(it.toLong().toInt()) }
        }

        ProtoType.INT64, ProtoType.SINT64, ProtoType.SFIXED64 -> {
            val parsed = integer(literal, LONG_MIN, LONG_MAX)
            parsed?.let { long(it.toLong()) }
        }

        ProtoType.UINT64, ProtoType.FIXED64 -> {
            val parsed = integer(literal, BigInteger.ZERO, ULONG_MAX)
            parsed?.let { long(it.toLong()) }
        }

        ProtoType.FLOAT -> {
            floating(
                literal = literal,
                type = FLOAT,
                format = "%Lf",
                fromInteger = { it.toFloat() },
                fromDecimal = String::toFloatOrNull,
            )
        }

        ProtoType.DOUBLE -> {
            floating(
                literal = literal,
                type = DOUBLE,
                format = "%L",
                fromInteger = { it.toDouble() },
                fromDecimal = String::toDoubleOrNull,
            )
        }

        ProtoType.BOOL -> {
            literal.toBooleanStrictOrNull()?.let { CodeBlock.of("%L", it) }
        }

        else -> null
    }

    /**
     * Generates a Kotlin integer expression.
     *
     * Uses `Int.MIN_VALUE` instead of a numeric literal for the minimum
     * representable value.
     *
     * @param value the integer to represent
     * @return the Kotlin integer expression
     */
    fun int(value: Int): CodeBlock {
        return if (value == Int.MIN_VALUE) {
            CodeBlock.of("%T.MIN_VALUE", INT)
        } else {
            CodeBlock.of("%L", value)
        }
    }

    /**
     * Generates a Kotlin long expression.
     *
     * Uses `Long.MIN_VALUE` for the minimum representable value and
     * appends the `L` suffix to other long literals.
     *
     * @param value the long value to represent
     * @return the Kotlin long expression
     */
    fun long(value: Long): CodeBlock {
        return if (value == Long.MIN_VALUE) {
            CodeBlock.of("%T.MIN_VALUE", LONG)
        } else {
            CodeBlock.of("%LL", value)
        }
    }

    /**
     * Generates a single-line Kotlin string literal.
     *
     * Escapes control characters, quotes, backslashes, dollar signs, and
     * non-ASCII characters to avoid raw strings and string interpolation.
     *
     * @param value the string to encode
     * @return the escaped Kotlin string expression
     */
    fun string(value: String): CodeBlock = CodeBlock.of("%L", kotlinString(value))

    /**
     * Generates a Kotlin byte array expression.
     *
     * Emits `byteArrayOf()` for empty arrays and decimal byte values
     * for non-empty arrays.
     *
     * @param value the bytes to encode
     * @return the Kotlin byte array expression
     */
    fun bytes(value: ByteArray): CodeBlock {
        if (value.isEmpty()) return CodeBlock.of("byteArrayOf()")
        return CodeBlock.of("byteArrayOf(%L)", value.joinToString(", "))
    }

    /**
     * Parses a Protobuf integer literal within the specified bounds.
     *
     * Supports decimal, hexadecimal (`0x` or `0X`), and octal
     * (leading `0`) notation, with an optional negative sign.
     *
     * @param literal the integer literal to parse
     * @param min the minimum accepted value, inclusive
     * @param max the maximum accepted value, inclusive
     * @return the parsed integer, or `null` if invalid or outside the allowed range
     */
    fun integer(literal: String, min: BigInteger, max: BigInteger): BigInteger? {
        val negative = literal.startsWith('-')
        val unsigned = if (negative) literal.substring(1) else literal
        val magnitude = parseMagnitude(unsigned) ?: return null

        val value = if (negative) magnitude.negate() else magnitude
        return value.takeIf { it in min..max }
    }

    /**
     * Parses the unsigned magnitude of a Protobuf integer literal.
     *
     * Detects the radix from the literal's prefix and rejects empty
     * or invalid digit sequences.
     *
     * @return the parsed non-negative integer, or `null` if invalid
     */
    private fun parseMagnitude(literal: String): BigInteger? {
        val (digits, radix) = when {
            literal.startsWith("0x") || literal.startsWith("0X") -> literal.substring(2) to 16
            literal.length > 1 && literal.startsWith('0') -> literal.substring(1) to 8
            else -> literal to 10
        }

        if (digits.isEmpty() || digits.any { Character.digit(it, radix) < 0 }) return null
        return BigInteger(digits, radix)
    }

    /**
     * Converts a Protobuf floating-point literal into a Kotlin expression.
     *
     * Recognizes infinity and NaN constants before parsing numeric values.
     * Finite values are formatted using their numeric `toString()` representation.
     *
     * @return the Kotlin expression, or `null` if the literal is invalid
     */
    private fun floating(
        literal: String,
        type: ClassName,
        format: String,
        fromInteger: (BigInteger) -> Number,
        fromDecimal: (String) -> Number?,
    ): CodeBlock? {
        special(literal)?.let { member ->
            return CodeBlock.of("%T.%L", type, member)
        }

        val value = finiteFloatingValue(literal, fromInteger, fromDecimal) ?: return null
        return CodeBlock.of(format, value.toString())
    }

    /**
     * Parses a finite floating-point value from an integer or decimal literal.
     *
     * Integer parsing takes precedence over decimal parsing. Values that
     * convert to infinity are rejected. Explicit infinity literals are
     * handled separately by [special].
     *
     * @return the finite numeric value, or `null` if parsing fails or the result is infinite
     */
    private fun finiteFloatingValue(
        literal: String,
        fromInteger: (BigInteger) -> Number,
        fromDecimal: (String) -> Number?,
    ): Number? {
        val value = integer(literal, LONG_MIN, ULONG_MAX)?.let(fromInteger)
            ?: decimal(literal)?.let(fromDecimal)
            ?: return null

        return value.takeUnless { it.toDouble().isInfinite() }
    }

    /**
     * Encodes a string as a single-line, ASCII-only Kotlin literal.
     *
     * KotlinPoet's `%S` may generate raw strings for multiline values,
     * so escaping is performed explicitly instead.
     *
     * @return the quoted and escaped Kotlin string literal
     */
    private fun kotlinString(value: String): String = buildString {
        append('"')

        for (char in value) {
            appendEscaped(char)
        }

        append('"')
    }

    /**
     * Appends a character using Kotlin string literal escaping rules.
     *
     * Special characters use dedicated escape sequences. Control characters
     * and non-ASCII characters without dedicated escapes use Unicode escapes.
     */
    private fun StringBuilder.appendEscaped(char: Char) {
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '$' -> append("\\$")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '\b' -> append("\\b")
            else -> {
                if (char !in ' '..'~') {
                    append("\\u").append(char.code.toString(16).padStart(4, '0'))
                } else {
                    append(char)
                }
            }
        }
    }

    /**
     * Validates a decimal floating-point literal before numeric parsing.
     *
     * Accepts digits, signs, decimal points, and exponent markers.
     * The numeric parser performs the remaining validation.
     *
     * @return the unchanged literal if its characters are allowed, otherwise `null`
     */
    private fun decimal(literal: String): String? {
        val valid = literal.isNotEmpty() && literal.all { it.isDigit() || it in ".-+eE" }
        return literal.takeIf { valid }
    }

    /**
     * Resolves the Kotlin floating-point constant for a special Protobuf literal.
     *
     * Recognizes `inf`, `infinity`, `-inf`, `-infinity`, and `nan`
     * case-insensitively.
     *
     * @return the corresponding Float/Double member name, or `null` if not recognized
     */
    private fun special(literal: String): String? {
        val negative = literal.startsWith('-')

        return when (literal.removePrefix("-").lowercase()) {
            "inf", "infinity" -> if (negative) "NEGATIVE_INFINITY" else "POSITIVE_INFINITY"
            "nan" -> "NaN"
            else -> null
        }
    }
}
