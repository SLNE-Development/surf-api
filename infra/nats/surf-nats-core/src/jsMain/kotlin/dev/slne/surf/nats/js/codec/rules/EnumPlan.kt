package dev.slne.surf.nats.js.codec.rules

import dev.slne.surf.nats.js.external.protobuf.DescEnum
import dev.slne.surf.nats.js.interop.getOrPut
import js.collections.WeakMap

internal class EnumPlan private constructor(enum: DescEnum) {

    private val known: Set<Int> = enum.values.mapTo(HashSet()) { it.number }

    val zero: Int = if (0 in known) 0 else enum.values.first().number

    fun isKnown(number: Int): Boolean = number in known

    companion object {
        private val plans = WeakMap<DescEnum, EnumPlan>()

        fun of(enum: DescEnum): EnumPlan = plans.getOrPut(enum) { EnumPlan(enum) }
    }
}
