package dev.slne.surf.api.paper.scoreboard

import dev.slne.surf.api.core.util.requiredService
import net.kyori.adventure.text.Component
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary

private val api = requiredService<SurfScoreboardApi>()

interface SurfScoreboardApi {
    fun scoreboardLibrary(): ScoreboardLibrary
    fun createScoreboard(title: Component): SurfScoreboardBuilder

    companion object : SurfScoreboardApi by api {
        val INSTANCE get() = api
    }
}