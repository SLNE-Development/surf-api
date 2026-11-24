package dev.slne.surf.api.paper.collections

import java.util.function.IntSupplier

class LazyIntList(
    private val defaultValueGenerator: IntSupplier,
) {
    private var values = IntArray(16)
    private var initialized = BooleanArray(16)
    private var size = 0

    operator fun get(index: Int): Int {
        ensureSize(index)

        if (initialized[index]) {
            return values[index]
        }

        val value = defaultValueGenerator.asInt
        values[index] = value
        initialized[index] = true

        return value
    }

    operator fun set(index: Int, value: Int) {
        ensureSize(index)

        values[index] = value
        initialized[index] = true
    }

    fun clear() {
        initialized.fill(false)
        size = 0
    }

    fun listCopy(): List<Int?> {
        return List(size) { index ->
            if (initialized[index]) values[index] else null
        }
    }

    fun listCopyNotNull(): List<Int> {
        return buildList {
            for (index in indices) {
                if (initialized[index]) {
                    add(values[index])
                }
            }
        }
    }

    private fun ensureSize(index: Int) {
        require(index >= 0) { "Index must be non-negative" }

        val requiredSize = index.toLong() + 1
        require(requiredSize <= Int.MAX_VALUE) {
            "Index is too large"
        }

        if (requiredSize > values.size) {
            val capacity = maxOf(
                requiredSize.toInt(),
                (values.size.toLong() * 2)
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt(),
            )

            values = values.copyOf(capacity)
            initialized = initialized.copyOf(capacity)
        }

        size = maxOf(size, requiredSize.toInt())
    }
}
