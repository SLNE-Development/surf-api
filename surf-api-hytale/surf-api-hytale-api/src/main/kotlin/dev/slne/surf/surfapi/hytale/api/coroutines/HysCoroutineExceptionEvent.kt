package dev.slne.surf.surfapi.hytale.api.coroutines

import com.hypixel.hytale.event.ICancellable
import com.hypixel.hytale.event.IEvent
import com.hypixel.hytale.server.core.plugin.JavaPlugin

class HysCoroutineExceptionEvent(
    val plugin: JavaPlugin,
    val exception: Throwable
) : IEvent<Unit>, ICancellable {
    private var cancelled = false

    override fun isCancelled() = cancelled

    override fun setCancelled(cancelled: Boolean) {
        this@HysCoroutineExceptionEvent.cancelled = cancelled
    }
}