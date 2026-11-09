@file:OptIn(InternalSurfApi::class)
@file:Suppress("DEPRECATION")

package dev.slne.surf.api.paper.scoreboard

import dev.slne.surf.api.di.surfBean
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.kyori.adventure.text.Component
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary

private val api by surfBean<SurfScoreboardApi>()

@Deprecated(
    "Replaced by the functions sidebar, autoUpdatingSidebar and globalSidebar in " +
        "dev.slne.surf.api.paper.sidebar. The sidebar API sends its packets itself and does not " +
        "expose a ScoreboardLibrary instance.",
    ReplaceWith("SurfSidebar", "dev.slne.surf.api.paper.sidebar.SurfSidebar")
)
interface SurfScoreboardApi {
    fun scoreboardLibrary(): ScoreboardLibrary
    fun createScoreboard(title: Component): SurfScoreboardBuilder

    companion object {
        val INSTANCE get() = api
    }
}
