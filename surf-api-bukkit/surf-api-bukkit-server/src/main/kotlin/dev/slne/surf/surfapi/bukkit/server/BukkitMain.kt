package dev.slne.surf.surfapi.bukkit.server

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.surfapi.bukkit.server.libs.LibLoader
import dev.slne.surf.surfapi.core.api.util.logger
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary

class BukkitMain : SuspendingJavaPlugin() {
    private val log = logger()


    private var scoreboardLibrary: ScoreboardLibrary? = null

    override suspend fun onLoadAsync() {
        LibLoader(classLoader).loadLibs()
        BukkitInstance.onLoad()
    }

    override suspend fun onEnableAsync() {
        BukkitInstance.onEnable()

        try {
            scoreboardLibrary = ScoreboardLibrary.loadScoreboardLibrary(this)
        } catch (e: NoPacketAdapterAvailableException) {
            log.atSevere()
                .withCause(e)
                .log("No packet adapter available, using NoopScoreboardLibrary...")
            scoreboardLibrary = NoopScoreboardLibrary()
        }
    }

    override suspend fun onDisableAsync() {
        BukkitInstance.onDisable()
        scoreboardLibrary?.close()
    }

    fun getScoreboardLibrary(): ScoreboardLibrary = scoreboardLibrary
        ?: error("ScoreboardLibrary has not been initialized yet! Are you trying to access it before onEnable?")

    companion object {
        @JvmStatic
        val instance get() = getPlugin(BukkitMain::class.java)
    }
}

val plugin get() = BukkitMain.instance
