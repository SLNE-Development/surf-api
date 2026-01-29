package dev.slne.surf.surfapi.core.api.component

import dev.slne.surf.surfapi.core.api.util.requiredService
import dev.slne.surf.surfapi.shared.api.component.Component

interface SurfComponentApi {

    suspend fun bootstrap(owner: Any)
    suspend fun load(owner: Any)
    suspend fun enable(owner: Any)
    suspend fun disable(owner: Any)

    suspend fun <T : Any> componentsOfType(owner: Any, type: Class<T>): List<T>
    fun <T : Any> componentsOfTypeLoaded(owner: Any, type: Class<T>): List<T>
    suspend fun <T : Any> componentsOfType(type: Class<T>): List<T>
    fun <T : Any> componentsOfTypeLoaded(type: Class<T>): List<T>

    suspend fun components(owner: Any): List<Component>
    fun componentsLoaded(owner: Any): List<Component>

    companion object {
        val instance = requiredService<SurfComponentApi>()
    }
}

val surfComponentApi get() = SurfComponentApi.instance
