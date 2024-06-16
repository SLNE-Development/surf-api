package dev.slne.surf.surfapi.bukkit.api.collections

class LazyIntList(private val defaultValueGenerator: () -> Int) {
    private val list = mutableListOf<Int?>()

    operator fun get(index: Int): Int {
        while (list.size <= index) {
            list.add(null)
        }
        return list[index] ?: defaultValueGenerator().also { list[index] = it }
    }

    operator fun set(index: Int, value: Int) {
        while (list.size <= index) {
            list.add(null)
        }
        list[index] = value
    }

    fun clear() {
        list.clear()
    }

    fun listCopy(): List<Int?> {
        return list.toList()
    }

    fun listCopyNotNull(): List<Int> {
        return list.filterNotNull()
    }
}