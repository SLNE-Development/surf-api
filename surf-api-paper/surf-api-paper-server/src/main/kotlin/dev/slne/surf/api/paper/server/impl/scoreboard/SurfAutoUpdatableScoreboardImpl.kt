package dev.slne.surf.api.paper.server.impl.scoreboard

import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.ticksDuration
import dev.slne.surf.api.paper.scoreboard.SurfAutoUpdatableScoreboard
import dev.slne.surf.api.paper.server.plugin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.SidebarAnimation

open class SurfAutoUpdatableScoreboardImpl(
    title: Component,
    maxLines: Int,
    sidebarComponent: SidebarComponent,
    animations: List<SidebarAnimation<Component>>
) : SurfScoreboardImpl(title, maxLines, sidebarComponent, animations), SurfAutoUpdatableScoreboard {
    private var updater: Job? = null

    override fun enable() {
        super.enable()

        updater?.cancel()
        updater = launchUpdater()
    }

    override fun disable() {
        updater?.cancel()
        updater = null

        super.disable()
    }

    private fun launchUpdater() = plugin.launch {
        while (true) {
            update()
            delay(5.ticksDuration)
        }
    }
}
