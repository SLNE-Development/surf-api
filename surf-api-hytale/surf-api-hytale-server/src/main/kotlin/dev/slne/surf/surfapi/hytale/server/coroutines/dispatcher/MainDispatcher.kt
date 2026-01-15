package dev.slne.surf.surfapi.hytale.server.coroutines.dispatcher

import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

internal open class MainDispatcher(
    private val plugin: JavaPlugin,
) : CoroutineDispatcher() {
    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return true
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        HytaleServer.SCHEDULED_EXECUTOR.schedule(block, 0L, TimeUnit.MILLISECONDS)
    }
}