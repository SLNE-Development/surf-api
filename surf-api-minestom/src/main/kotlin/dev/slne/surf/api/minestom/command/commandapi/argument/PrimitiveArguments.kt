/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command.commandapi.argument

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.LongArgumentType
import dev.slne.surf.api.minestom.command.commandapi.argument.parser.FixedSetParser
import dev.slne.surf.api.minestom.command.commandapi.argument.parser.FloatRangeParser
import dev.slne.surf.api.minestom.command.commandapi.argument.parser.IntegerRangeParser
import dev.slne.surf.api.minestom.command.commandapi.argument.parser.UuidParser
import net.minestom.server.utils.Range
import java.util.*

class BooleanArgument(nodeName: String) : Argument<Boolean>(nodeName) {
    override val kind = ArgumentKind.Boolean
    override val rawType: ArgumentType<Boolean> = BoolArgumentType.bool()
    override fun stringify(value: Boolean): String = value.toString()
}

class IntegerArgument(
    nodeName: String,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
) : Argument<Int>(nodeName) {
    override val kind = ArgumentKind.Integer(min, max)
    override val rawType: ArgumentType<Int> = IntegerArgumentType.integer(min, max)

    init {
        require(min <= max) { "Minimum integer value must not exceed maximum value" }
    }

    override fun stringify(value: Int): String = value.toString()
}

class LongArgument(
    nodeName: String,
    min: Long = Long.MIN_VALUE,
    max: Long = Long.MAX_VALUE,
) : Argument<Long>(nodeName) {
    override val kind = ArgumentKind.Long(min, max)
    override val rawType: ArgumentType<Long> = LongArgumentType.longArg(min, max)

    init {
        require(min <= max) { "Minimum long value must not exceed maximum value" }
    }

    override fun stringify(value: Long): String = value.toString()
}

class FloatArgument(
    nodeName: String,
    min: Float = -Float.MAX_VALUE,
    max: Float = Float.MAX_VALUE,
) : Argument<Float>(nodeName) {
    override val kind = ArgumentKind.Float(min, max)
    override val rawType: ArgumentType<Float> = FloatArgumentType.floatArg(min, max)

    init {
        require(!min.isNaN() && !max.isNaN() && min <= max) {
            "Minimum float value must not exceed maximum value and bounds must not be NaN"
        }
    }

    override fun stringify(value: Float): String = value.toString()
}

class DoubleArgument(
    nodeName: String,
    min: Double = -Double.MAX_VALUE,
    max: Double = Double.MAX_VALUE,
) : Argument<Double>(nodeName) {
    override val kind = ArgumentKind.Double(min, max)
    override val rawType: ArgumentType<Double> = DoubleArgumentType.doubleArg(min, max)

    init {
        require(!min.isNaN() && !max.isNaN() && min <= max) {
            "Minimum double value must not exceed maximum value and bounds must not be NaN"
        }
    }

    override fun stringify(value: Double): String = value.toString()
}

class EnumArgument<E : Enum<E>>(
    nodeName: String,
    values: Collection<E>,
    private val formatter: (E) -> String = { value -> value.name.lowercase() },
) : Argument<E>(nodeName) {
    private val values = EnumSet.copyOf(values)

    override val kind = ArgumentKind.Enum(this.values, formatter)
    override val rawType: ArgumentType<E> = FixedSetParser(this.values.associateBy(formatter))

    init {
        require(this.values.isNotEmpty()) { "Enum argument must contain at least one value" }

        val formattedValues = this.values.map(formatter)

        require(formattedValues.all(String::isNotBlank)) { "Formatted enum values must not be blank" }
        require(formattedValues.distinct().size == formattedValues.size) { "Formatted enum values must be unique" }
    }

    override fun stringify(value: E): String = formatter(value)
}

/**
 * A UUID parsed in its canonical dashed hex form.
 */
class UUIDArgument(nodeName: String) : Argument<UUID>(nodeName) {
    override val kind = ArgumentKind.Uuid
    override val rawType: ArgumentType<UUID> = UuidParser
    override fun stringify(value: UUID): String = value.toString()
}

/**
 * An integer range parsed from vanilla's `min..max` syntax.
 *
 * Either bound may be omitted; an omitted bound resolves to `Int.MIN_VALUE` or `Int.MAX_VALUE`
 * respectively. A bare value is shorthand for a range containing only that value.
 */
class IntegerRangeArgument(nodeName: String) : Argument<Range.Int>(nodeName) {
    override val kind = ArgumentKind.IntegerRange
    override val rawType: ArgumentType<Range.Int> = IntegerRangeParser
    override fun stringify(value: Range.Int): String = stringifyRange(value.min(), value.max())
}

/**
 * A float range parsed from vanilla's `min..max` syntax.
 *
 * Either bound may be omitted; an omitted bound resolves to `-Float.MAX_VALUE` or
 * `Float.MAX_VALUE` respectively. A bare value is shorthand for a range containing only that
 * value.
 */
class FloatRangeArgument(nodeName: String) : Argument<Range.Float>(nodeName) {
    override val kind = ArgumentKind.FloatRange
    override val rawType: ArgumentType<Range.Float> = FloatRangeParser
    override fun stringify(value: Range.Float): String = stringifyRange(value.min(), value.max())
}

private fun stringifyRange(min: Number?, max: Number?): String = when {
    min != null && min == max -> min.toString()
    else -> "${min.orEmpty()}..${max.orEmpty()}"
}

private fun Number?.orEmpty(): String = this?.toString().orEmpty()
