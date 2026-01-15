package dev.slne.surf.surfapi.hytale.server

import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ExecutorService

class HytaleMain(init: JavaPluginInit) : JavaPlugin(init) {
    lateinit var executorService: ExecutorService
        private set

    override fun setup() {
        runBlocking {
            HytaleInstance.bootstrap()
            HytaleInstance.onLoad()

            executorService = HytaleServer.SCHEDULED_EXECUTOR
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