package dev.slne.surf.surfapi.core.api.hook

import dev.slne.surf.surfapi.core.api.util.requiredService
import dev.slne.surf.surfapi.shared.api.hook.Hook

interface SurfHookApi {

    suspend fun bootstrap(owner: Any)
    suspend fun load(owner: Any)
    suspend fun enable(owner: Any)
    suspend fun disable(owner: Any)

    suspend fun <T : Any> hooksOfType(owner: Any, type: Class<T>): List<T>
    fun <T : Any> hooksOfTypeLoaded(owner: Any, type: Class<T>): List<T>
    suspend fun <T : Any> hooksOfType(type: Class<T>): List<T>
    fun <T : Any> hooksOfTypeLoaded(type: Class<T>): List<T>

    suspend fun hooks(owner: Any): List<Hook>
    fun hooksLoaded(owner: Any): List<Hook>

    companion object {
        val instance = requiredService<SurfHookApi>()
    }
}

val surfHookApi get() = SurfHookApi.instance

