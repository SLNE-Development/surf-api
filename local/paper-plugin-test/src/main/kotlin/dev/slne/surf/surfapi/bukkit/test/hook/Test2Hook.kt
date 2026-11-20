package dev.slne.surf.surfapi.bukkit.test.hook

import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.api.spring.SurfLifecycle
import org.springframework.stereotype.Service

/**
 * A hook that only takes part in the load phase.
 */
@Service
class Test2Hook : SurfLifecycle {

    override suspend fun onLoad() {
        System.err.println("Test2Hook loaded on server: ${server.name}")
    }
}
