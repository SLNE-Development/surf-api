package dev.slne.surf.surfapi.shared.api.component

interface Component : Comparable<Component> {
    val priority: Short
    suspend fun bootstrap()
    suspend fun load()
    suspend fun enable()
    suspend fun disable()
}
