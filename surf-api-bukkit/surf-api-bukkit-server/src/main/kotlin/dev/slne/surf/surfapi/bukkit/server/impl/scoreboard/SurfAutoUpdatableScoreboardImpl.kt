package dev.slne.surf.surfapi.bukkit.server.impl.scoreboard

import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.ticks
import dev.slne.surf.surfapi.bukkit.api.scoreboard.ObsoleteScoreboardApi
import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfAutoUpdatableScoreboard
import dev.slne.surf.surfapi.bukkit.server.plugin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.SidebarAnimation

@ObsoleteScoreboardApi
open class SurfAutoUpdatableScoreboardImpl(
    title: Component,
    maxLines: Int,
    sidebarComponent: SidebarComponent,
    animations: List<SidebarAnimation<Component>>
) : SurfScoreboardImpl(title, maxLines, sidebarComponent, animations), SurfAutoUpdatableScoreboard {
    private var updater: Job? = null

    override fun enable() {
        super.enable()

        this.updater = launchUpdater()
    }

    override fun disable() {
        super.disable()

        updater!!.cancel()
    }

    private fun launchUpdater() = plugin.launch {
        while (true) {
            update()
            delay(5.ticks)
        }
    }
}
