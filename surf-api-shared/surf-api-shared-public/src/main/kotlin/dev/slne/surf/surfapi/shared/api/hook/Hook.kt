package dev.slne.surf.surfapi.shared.api.hook

interface Hook : Comparable<Hook> {
    val priority: Short
    suspend fun bootstrap()
    suspend fun load()
    suspend fun enable()
    suspend fun disable()
}