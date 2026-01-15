package dev.slne.surf.surfapi.hytale.api.coroutines

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import kotlinx.coroutines.runBlocking

open class SuspendingJavaPlugin(init: JavaPluginInit) : JavaPlugin(init) {
    open suspend fun setupAsync() {}
    open suspend fun startAsync() {}
    open suspend fun shutdownAsync() {}

    override fun setup() {
        runBlocking {
            setupAsync()
        }
    }

    override fun start() {
        runBlocking {
            startAsync()
        }
    }

    override fun shutdown() {
        runBlocking {
            shutdownAsync()
        }
    }
}