package dev.slne.surf.surfapi.bukkit.test.hook

import dev.slne.surf.api.di.Service
import dev.slne.surf.api.di.SurfComponent
import dev.slne.surf.api.paper.extensions.server

/** An unconditional bean that only needs one of the three lifecycle phases. */
@Service
class Test2Hook : SurfComponent {

    override suspend fun load() {
        System.err.println("Test2Hook loaded on server: ${server.name}")
    }
}
