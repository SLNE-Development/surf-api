package dev.slne.surf.api.paper.server.time

import dev.slne.surf.api.paper.coroutine.ticks
import dev.slne.surf.api.paper.server.coroutine.surfDispatchers
import dev.slne.surf.api.paper.time.TimeSkipResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bukkit.World
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object TimeHandler {
    const val DEFAULT_SKIP_AMOUNT: Long = 100L
    private val skippingWorlds: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    suspend fun skipTimeSmoothly(world: World, timeToAdd: Long, duration: Long): TimeSkipResult {
        val targetTime = world.fullTime + timeToAdd
        val step = timeToAdd / duration
        val inProcess = !skippingWorlds.add(world.uid)

        if (inProcess) return TimeSkipResult.ALREADY_SKIPPING

        return withContext(surfDispatchers.main) {
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
