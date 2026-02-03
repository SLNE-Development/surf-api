package dev.slne.surf.surfapi.core.server.impl.component

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.core.api.component.SurfComponentApi
import dev.slne.surf.surfapi.core.server.component.ComponentService
import dev.slne.surf.surfapi.shared.api.component.Component

@AutoService(SurfComponentApi::class)
class SurfComponentApiImpl : SurfComponentApi {
    override suspend fun bootstrap(owner: Any) {
        for (component in components(owner)) {
            component.bootstrap()
        }
    }

    override suspend fun load(owner: Any) {
        for (component in components(owner)) {
            component.load()
        }
    }

    override suspend fun enable(owner: Any) {
        for (component in components(owner)) {
            component.enable()
        }
    }

    override suspend fun disable(owner: Any) {
        ComponentService.get().invokePostProcessorsBeforeDestruction(owner)

        for (component in ComponentService.get().awaitLoadedComponents(owner).reversed()) {
            component.disable()
        }
    }

    override suspend fun <T : Any> componentsOfType(
        owner: Any,
        type: Class<T>
    ): List<T> {
        return components(owner).filterIsInstance(type)
    }

    override fun <T : Any> componentsOfTypeLoaded(
        owner: Any,
        type: Class<T>
    ): List<T> {
        return componentsLoaded(owner).filterIsInstance(type)
    }

    override suspend fun <T : Any> componentsOfType(type: Class<T>): List<T> {
        return ComponentService.get().getAllComponents().filterIsInstance(type)
    }

    override fun <T : Any> componentsOfTypeLoaded(type: Class<T>): List<T> {
        return ComponentService.get().getAllComponentsLoaded().filterIsInstance(type)
    }

    override suspend fun components(owner: Any): List<Component> {
        return ComponentService.get().getOrLoadComponents(owner)
    }

    override fun componentsLoaded(owner: Any): List<Component> {
        return ComponentService.get().getLoadedComponents(owner)
    }
}