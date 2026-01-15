package dev.slne.surf.surfapi.hytale.server

import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import dev.slne.surf.surfapi.hytale.api.coroutines.SuspendingJavaPlugin
import kotlinx.coroutines.runBlocking

class HytaleMain(init: JavaPluginInit) : SuspendingJavaPlugin(init) {
    override fun setup() {
        runBlocking {
            HytaleInstance.bootstrap()
            HytaleInstance.onLoad()
        }
    }

    override fun start() {
        runBlocking {
            HytaleInstance.onEnable()
        }
    }

    override fun shutdown() {
        runBlocking {
            HytaleInstance.onDisable()
        }
    }

    companion object {
        lateinit var INSTANCE: HytaleMain
            private set
    }
}

val hytaleMain get() = HytaleMain.INSTANCE