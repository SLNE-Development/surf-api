package dev.slne.surf.surfapi.bukkit.server.time

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.ticks
import dev.slne.surf.surfapi.bukkit.api.time.TimeSkipResult
import dev.slne.surf.surfapi.bukkit.server.plugin
import dev.slne.surf.surfapi.core.api.util.mutableObjectSetOf
import dev.slne.surf.surfapi.core.api.util.synchronize
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bukkit.World
import java.util.*

object TimeHandler {
    const val DEFAULT_SKIP_AMOUNT: Long = 100L
    private val skippingWorlds = mutableObjectSetOf<UUID>().synchronize()

    suspend fun skipTimeSmoothly(world: World, timeToAdd: Long, duration: Long): TimeSkipResult {
        val targetTime = world.fullTime + timeToAdd
        val step = timeToAdd / duration
        val inProcess = !skippingWorlds.add(world.uid)

        if (inProcess) return TimeSkipResult.ALREADY_SKIPPING

        return withContext(plugin.globalRegionDispatcher) {
            while (true) {
                val newTime = world.fullTime + step
                if (newTime >= targetTime) {
                    world.fullTime = targetTime
                    skippingWorlds.remove(world.uid)
                    break
                } else {
                    world.fullTime = newTime
                }

                delay(1.ticks)
            }

            TimeSkipResult.SUCCESS
        }
    }
}
