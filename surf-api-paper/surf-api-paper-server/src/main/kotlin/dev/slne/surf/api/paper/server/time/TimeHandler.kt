package dev.slne.surf.api.paper.server.time

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.ticksDuration
import dev.slne.surf.api.core.util.mutableObjectSetOf
import dev.slne.surf.api.core.util.synchronize
import dev.slne.surf.api.paper.server.plugin
import dev.slne.surf.api.paper.time.TimeSkipResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bukkit.World
import java.util.*

object TimeHandler {
    const val DEFAULT_SKIP_AMOUNT: Long = 100L
    private val skippingWorlds = mutableObjectSetOf<UUID>().synchronize()

    @Suppress("DEPRECATION")
    suspend fun skipTimeSmoothly(world: World, timeToAdd: Long, duration: Long): TimeSkipResult {
        require(duration > 0) { "duration must be positive, got $duration" }

        if (!skippingWorlds.add(world.uid)) return TimeSkipResult.ALREADY_SKIPPING

        try {
            return withContext(plugin.globalRegionDispatcher) {
                var remainingTime = timeToAdd
                var remainingTicks = duration

                while (remainingTicks > 0) {
                    val step = remainingTime / remainingTicks
                    world.fullTime += step
                    remainingTime -= step
                    remainingTicks--

                    if (remainingTicks > 0) {
                        delay(1.ticksDuration)
                    }
                }

                TimeSkipResult.SUCCESS
            }
        } finally {
            skippingWorlds.remove(world.uid)
        }
    }
}
