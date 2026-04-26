package dev.slne.surf.surfapi.bukkit.test.command.subcommands.eventhandler.listener

import dev.slne.surf.api.core.event.SurfEventBus
import dev.slne.surf.api.core.event.SurfEventHandler
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.eventhandler.event.TestAsyncEvent
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

object TestAsyncEventListener {
    private var lastEvent: TestAsyncEvent? = null
    private var lastHandledTime: Long = 0L

    fun register() {
        SurfEventBus.registerListeners(this)
    }

    fun unregister() {
        SurfEventBus.unregisterListeners(this)
    }

    fun getLastEvent(): TestAsyncEvent? = lastEvent

    fun getLastHandledTime(): Long = lastHandledTime

    fun clearLastEvent() {
        lastEvent = null
        lastHandledTime = 0L
    }

    @SurfEventHandler
    private suspend fun onTestAsyncEvent(event: TestAsyncEvent) {
        lastEvent = event
        lastHandledTime = System.currentTimeMillis()
        print("Delaying event for 1 second...")
        delay(1.seconds)
    }
}

