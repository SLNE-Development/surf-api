package dev.slne.surf.api.minestom.coroutine

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import net.minestom.server.MinecraftServer

internal object MinestomScopeProvider {

    val scope: CoroutineScope
    val asyncScope: CoroutineScope
    val blockingScope: CoroutineScope

    init {
        val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
            MinecraftServer.LOGGER.error("Coroutine exception in context: $context", throwable)
        }
        val rootScope = CoroutineScope(exceptionHandler)

        scope = rootScope + SupervisorJob() + MinestomDispatchers.Main
        asyncScope = rootScope + SupervisorJob() + Dispatchers.Default
        blockingScope = rootScope + SupervisorJob() + MinestomDispatchers.Blocking
    }
}

val minestomScope get() = MinestomScopeProvider.scope
val minestomAsyncScope get() = MinestomScopeProvider.asyncScope
val minestomBlockingScope get() = MinestomScopeProvider.blockingScope
