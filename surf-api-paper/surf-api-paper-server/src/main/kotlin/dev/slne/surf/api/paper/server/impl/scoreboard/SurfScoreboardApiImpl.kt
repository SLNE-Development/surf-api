package dev.slne.surf.api.paper.server.impl.scoreboard

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.paper.scoreboard.SurfScoreboardApi
import dev.slne.surf.api.paper.server.plugin
import net.kyori.adventure.text.Component
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary

@AutoService(SurfScoreboardApi::class)
class SurfScoreboardApiImpl : SurfScoreboardApi {
    private lateinit var scoreboardLibrary: ScoreboardLibrary

    fun onEnable() {
        try {
            scoreboardLibrary = ScoreboardLibrary.loadScoreboardLibrary(plugin)
        } catch (exception: NoPacketAdapterAvailableException) {
            log.atSevere().withCause(exception)
                .log("No packet adapter available, using NooScoreboardLibrary...")
            scoreboardLibrary = NoopScoreboardLibrary()
        }
    }

    fun onDisable() {
        scoreboardLibrary.close()
    }

    override fun scoreboardLibrary(): ScoreboardLibrary {
        TODO("Not yet implemented")
    }

    override fun createScoreboard(title: Component) = SurfScoreboardBuilderImpl(title)

    companion object {
        private val log = logger()
        
        val INSTANCE get() = SurfScoreboardApi.INSTANCE as SurfScoreboardApiImpl
    }
}