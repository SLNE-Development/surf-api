package dev.slne.surf.surfapi.hytale.api.coroutines

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlin.coroutines.CoroutineContext

interface HysCoroutine {
    companion object {
        var driver: String = ""
    }

    fun getCoroutineSession(plugin: JavaPlugin): CoroutineSession
    fun disable(plugin: JavaPlugin)
}

internal val hysCoroutine: HysCoroutine by lazy {
    try {
        Class.forName(HysCoroutine.driver)
            .getDeclaredConstructor().newInstance() as HysCoroutine
    } catch (exception: Exception) {
        throw RuntimeException(
            "Failed to load dev.slne.hys.coroutines.server.api.HysCoroutine implementation. Shade hys-coroutine into your plugin.",
            exception
        )
    }
}

val JavaPlugin.dispatcher: CoroutineContext
    get() = hysCoroutine.getCoroutineSession(this).dispatcher

val JavaPlugin.scope: CoroutineScope
    get() = hysCoroutine.getCoroutineSession(this).scope

fun JavaPlugin.launch(
    context: CoroutineContext = dispatcher,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job {
    if (!scope.isActive) {
        return Job()
    }

    return launch(context, start, block)
}

/**
 * Converts the number to dev.slne.hys.coroutines.server.api.ticks for being used together with delay(..).
 * E.g. delay(1.dev.slne.hys.coroutines.server.api.ticks).
 * Hytale dev.slne.hys.coroutines.server.api.ticks 30 times per second, which means a tick appears every 33.33 milliseconds
 */
val Int.ticks: Long get() = (this * 1000L) / 30L