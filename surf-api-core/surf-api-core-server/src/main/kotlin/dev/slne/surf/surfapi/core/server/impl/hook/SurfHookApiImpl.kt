package dev.slne.surf.surfapi.core.server.impl.hook

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.core.api.hook.SurfHookApi
import dev.slne.surf.surfapi.core.server.hook.HookService
import dev.slne.surf.surfapi.shared.api.hook.Hook

@AutoService(SurfHookApi::class)
class SurfHookApiImpl : SurfHookApi {
    override suspend fun bootstrap(owner: Any) {
        for (hook in hooks(owner)) {
            hook.bootstrap()
        }
    }

    override suspend fun load(owner: Any) {
        for (hook in hooks(owner)) {
            hook.load()
        }
    }

    override suspend fun enable(owner: Any) {
        for (hook in hooks(owner)) {
            hook.enable()
        }
    }

    override suspend fun disable(owner: Any) {
        for (hook in hooks(owner).reversed()) {
            hook.disable()
        }
    }

    override suspend fun <T : Any> hooksOfType(
        owner: Any,
        type: Class<T>
    ): List<T> {
        return hooks(owner).filterIsInstance(type)
    }

    override fun <T : Any> hooksOfTypeLoaded(
        owner: Any,
        type: Class<T>
    ): List<T> {
        return hooksLoaded(owner).filterIsInstance(type)
    }

    override suspend fun <T : Any> hooksOfType(type: Class<T>): List<T> {
        return HookService.get().getAllHooks().filterIsInstance(type)
    }

    override fun <T : Any> hooksOfTypeLoaded(type: Class<T>): List<T> {
        return HookService.get().getAllHooksLoaded().filterIsInstance(type)
    }

    override suspend fun hooks(owner: Any): List<Hook> {
        return HookService.get().getHooks(owner)
    }

    override fun hooksLoaded(owner: Any): List<Hook> {
        return HookService.get().getHooksLoaded(owner)
    }
}