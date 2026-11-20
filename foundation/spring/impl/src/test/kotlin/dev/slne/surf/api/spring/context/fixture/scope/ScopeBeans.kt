package dev.slne.surf.api.spring.context.fixture.scope

import dev.slne.surf.api.spring.SurfLifecycle
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import org.springframework.stereotype.Service

/**
 * Injects a coroutine scope while the root context offers one as well, and records what its hooks
 * see of the scope.
 */
@Service
class ScopeUser(val scope: CoroutineScope) : SurfLifecycle {
    var enableCoroutineName: String? = null
    var scopeActiveDuringDisable: Boolean? = null

    override suspend fun onEnable() {
        enableCoroutineName = currentCoroutineContext()[CoroutineName]?.name
    }

    override suspend fun onDisable() {
        scopeActiveDuringDisable = scope.isActive
    }
}
