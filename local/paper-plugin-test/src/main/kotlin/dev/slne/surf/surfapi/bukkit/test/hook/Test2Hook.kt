package dev.slne.surf.surfapi.bukkit.test.hook

import dev.slne.surf.api.core.component.AbstractComponent
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.api.shared.api.component.SurfComponentMeta

@SurfComponentMeta
class Test2Hook : AbstractComponent() {

    override suspend fun onLoad() {
        System.err.println("Test2Hook loaded on server: ${server.name}")
    }
}