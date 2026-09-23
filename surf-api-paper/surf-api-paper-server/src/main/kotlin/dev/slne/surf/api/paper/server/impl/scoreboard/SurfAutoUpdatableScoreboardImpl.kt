package dev.slne.surf.api.paper.server.impl.scoreboard

import com.github.shynixn.mccoroutine.folia.scope
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.api.paper.scoreboard.SurfAutoUpdatableScoreboard
import dev.slne.surf.api.paper.server.plugin
import io.papermc.paper.util.Tick
import kotlinx.coroutines.Job
import kotlin.concurrent.withLock
import kotlin.time.toKotlinDuration

open class SurfAutoUpdatableScoreboardImpl(
    definition: ScoreboardDefinition
) : SurfScoreboardImpl(definition), SurfAutoUpdatableScoreboard {
    private var updater: Job? = null

    override fun enable() {
        lock.withLock {
            super.enable()
            updater = launchUpdater()
        }
    }

    override fun disable() {
        lock.withLock {
            super.disable()
            updater?.cancel()
            updater = null
        }
    }

    private fun launchUpdater(): Job = plugin.scope.runAtFixedRate(
        fiveTicks,
        taskName = "SurfAutoUpdatableScoreboardUpdater"
    ) {
        updateIfEnabled()
    }

    companion object {
        private val fiveTicks = Tick.of(5).toKotlinDuration()
    }
}
