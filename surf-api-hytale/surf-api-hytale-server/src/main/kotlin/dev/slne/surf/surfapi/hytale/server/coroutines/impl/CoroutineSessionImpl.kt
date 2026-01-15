package dev.slne.surf.surfapi.hytale.server.coroutines.impl

import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import dev.slne.surf.surfapi.hytale.api.coroutines.CoroutineSession
import dev.slne.surf.surfapi.hytale.api.coroutines.HysCoroutineExceptionEvent
import dev.slne.surf.surfapi.hytale.server.coroutines.dispatcher.MainDispatcher
import dev.slne.surf.surfapi.hytale.server.coroutines.dispatcher.PluginDispatcher
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

internal class CoroutineSessionImpl(
    private val plugin: JavaPlugin,
) : CoroutineSession {
    override val scope: CoroutineScope

    override val mainDispatcher: CoroutineContext by lazy {
        MainDispatcher(plugin)
    }

    override val pluginDispatcher: CoroutineContext by lazy {
        PluginDispatcher(plugin, 16)
    }

    init {
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            val hysCoroutineExceptionEvent = HysCoroutineExceptionEvent(plugin, exception)

            if (plugin.isEnabled) {
                HytaleServer.SCHEDULED_EXECUTOR.schedule({
                    HytaleServer.get().eventBus.dispatchFor(HysCoroutineExceptionEvent::class.java)
                        .dispatch(hysCoroutineExceptionEvent)

                    if (!hysCoroutineExceptionEvent.isCancelled) {
                        if (exception !is CancellationException) {
                            plugin.logger.atSevere().log(
                                "This is not an error of HysCoroutine! See sub exception for details.",
                                exception
                            )
                        }
                    }
                }, 0L, TimeUnit.MILLISECONDS)
            }
        }

        val rootCoroutineScope = CoroutineScope(exceptionHandler)

        scope = rootCoroutineScope + SupervisorJob() + mainDispatcher
    }

    fun dispose() {
        scope.coroutineContext.cancelChildren()
        scope.cancel()
    }
}