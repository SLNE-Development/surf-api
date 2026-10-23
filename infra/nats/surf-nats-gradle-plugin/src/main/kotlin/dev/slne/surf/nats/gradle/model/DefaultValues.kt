package dev.slne.surf.nats.gradle.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import java.math.BigInteger

/**
 * Converts proto2 `[default = ...]` literals into Kotlin expressions; [scalar] covers the numeric and `bool` types and
 * returns `null` for an invalid literal.
 */
internal object DefaultValues {
    private val INT_MIN: BigInteger = BigInteger.valueOf(Int.MIN_VALUE.toLong())
    private val INT_MAX: BigInteger = BigInteger.valueOf(Int.MAX_VALUE.toLong())
    private val UINT_MAX: BigInteger = BigInteger.ONE.shiftLeft(32).subtract(BigInteger.ONE)
    private val LONG_MIN: BigInteger = BigInteger.valueOf(Long.MIN_VALUE)
    private val LONG_MAX: BigInteger = BigInteger.valueOf(Long.MAX_VALUE)
    private val ULONG_MAX: BigInteger = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE)

    fun scalar(protoType: String, literal: String): CodeBlock? = when (protoType) {
        "int32", "sint32", "sfixed32" -> integer(literal, INT_MIN, INT_MAX)?.let { int(it.toInt()) }
        "uint32", "fixed32" -> integer(literal, BigInteger.ZERO, UINT_MAX)?.let { int(it.toLong().toInt()) }
        "int64", "sint64", "sfixed64" -> integer(literal, LONG_MIN, LONG_MAX)?.let { long(it.toLong()) }
        "uint64", "fixed64" -> integer(literal, BigInteger.ZERO, ULONG_MAX)?.let { long(it.toLong()) }
        "float" -> floating(literal, FLOAT, "%Lf", { it.toFloat() }, String::toFloatOrNull)
        "double" -> floating(literal, DOUBLE, "%L", { it.toDouble() }, String::toDoubleOrNull)
        "bool" -> literal.toBooleanStrictOrNull()?.let { CodeBlock.of("%L", it) }
        else -> null
    }

    fun int(value: Int): CodeBlock {
        return if (value == Int.MIN_VALUE) CodeBlock.of("%T.MIN_VALUE", INT) else CodeBlock.of("%L", value)
    }

    fun long(value: Long): CodeBlock {
        return if (value == Long.MIN_VALUE) CodeBlock.of("%T.MIN_VALUE", LONG) else CodeBlock.of("%LL", value)
    }

    fun string(value: String): CodeBlock = CodeBlock.of("%L", kotlinString(value))

    fun bytes(value: ByteArray): CodeBlock {
        if (value.isEmpty()) return CodeBlock.of("byteArrayOf()")
        return CodeBlock.of("byteArrayOf(%L)", value.joinToString(", "))
    }

    /**
     * Parses a proto integer literal (decimal, `0x` hexadecimal, `0` octal, optional `-`) within [min]..[max].
     */
    fun integer(literal: String, min: BigInteger, max: BigInteger): BigInteger? {
        val negative = literal.startsWith('-')
        val unsigned = if (negative) literal.substring(1) else literal
        val (digits, radix) = when {
            unsigned.startsWith("0x") || unsigned.startsWith("0X") -> unsigned.substring(2) to 16
            unsigned.length > 1 && unsigned.startsWith('0') -> unsigned.substring(1) to 8
            else -> unsigned to 10
        }
        if (digits.isEmpty() || digits.any { Character.digit(it, radix) < 0 }) return null
        val magnitude = BigInteger(digits, radix)
        val value = if (negative) magnitude.negate() else magnitude
        return value.takeIf { it in min..max }
    }

    /**
     * Converts a `float` or `double` literal; [format] renders the finite value's `toString()`.
     */
    private fun floating(
        literal: String,
        type: ClassName,
        format: String,
        fromInteger: (BigInteger) -> Number,
        fromDecimal: (String) -> Number?,
    ): CodeBlock? {
        special(literal)?.let { member -> return CodeBlock.of("%T.%L", type, member) }
        val value = integer(literal, LONG_MIN, ULONG_MAX)?.let(fromInteger) ?: decimal(literal)?.let(fromDecimal) ?: return null
        if (value.toDouble().isInfinite()) return null
        return CodeBlock.of(format, value.toString())
    }

    /**
     * Returns a single-line, ASCII-only literal, because KotlinPoet's `%S` emits raw strings for line breaks.
     */
    private fun kotlinString(value: String): String = buildString {
        append('"')
        for (char in value) {
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '$' -> append("\\$")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                else -> if (char < ' ' || char > '~') append("\\u").append(char.code.toString(16).padStart(4, '0')) else append(char)
            }
        }
        append('"')
    }

    private fun decimal(literal: String): String? {
        val valid = literal.isNotEmpty() && literal.all { it.isDigit() || it in ".-+eE" }
        return literal.takeIf { valid }
    }

    /**
     * Returns the `Float`/`Double` member name of `inf`, `-inf` or `nan`, else `null`.
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
