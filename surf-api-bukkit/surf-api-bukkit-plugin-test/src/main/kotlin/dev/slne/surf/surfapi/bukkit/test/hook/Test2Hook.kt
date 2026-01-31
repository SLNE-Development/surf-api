package dev.slne.surf.surfapi.bukkit.test.hook

import dev.slne.surf.surfapi.bukkit.api.extensions.server
import dev.slne.surf.surfapi.core.api.component.AbstractComponent
import dev.slne.surf.surfapi.shared.api.component.ComponentMeta

@ComponentMeta
class Test2Hook : AbstractComponent() {

    override suspend fun onLoad() {
        System.err.println("Test2Hook loaded on server: ${server.name}")
    }
}