package dev.slne.surf.api.minestom.server.visibility

import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import net.minestom.server.MinecraftServer
import net.minestom.server.ServerFlag
import net.minestom.server.entity.AutomaticPlayerVisibility
import net.minestom.server.entity.Player
import net.minestom.server.instance.EntityTracker
import net.minestom.server.instance.Instance
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule

/**
 * Limits how many players a player sees at once to the nearest ones, refreshing the selection
 * every few ticks.
 */
internal class PlayerVisibilityService(
    private val settings: PlayerVisibilitySettings,
    private val handlers: (Player) -> PlayerVisibilityHandler?,
) {

    private val selector = NearestPlayerSelector(
        maxVisible = settings.maxVisible,
        activateAt = settings.activateAt,
        deactivateBelow = settings.deactivateBelow,
        retentionDistanceFactor = settings.retentionDistanceFactor,
    )

    private val candidateIds = IntOpenHashSet()

    private var task: Task? = null
    private var phase = 0

    fun start() {
        check(task == null) {
            "Player visibility service is already running"
        }

        task = MinecraftServer.getSchedulerManager()
            .buildTask(::tick)
            .delay(TaskSchedule.tick(1))
            .repeat(TaskSchedule.tick(1))
            .schedule()
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    private fun tick() {
        val currentPhase = phase

        phase = if (phase + 1 == settings.refreshIntervalTicks) {
            0
        } else {
            phase + 1
        }

        for (viewer in MinecraftServer.getConnectionManager().onlinePlayers) {
            val handler = handlers(viewer) ?: continue
            val instance = viewer.instance ?: continue

            if (!viewer.isActive || viewer.isRemoved) continue

            val ruleChanged = handler.applyRequestedRule() || handler.requiresNativeRuleRefresh

            val previous = handler.visibilitySelection
            val instanceChanged = previous.instance !== instance

            val scheduled = Math.floorMod(
                viewer.entityId,
                settings.refreshIntervalTicks,
            ) == currentPhase

            if (!ruleChanged && !instanceChanged && !scheduled) continue

            try {
                refresh(
                    viewer = viewer,
                    handler = handler,
                    instance = instance,
                    previous = previous,
                    instanceChanged = instanceChanged,
                    ruleChanged = ruleChanged,
                )
            } catch (exception: Exception) {
                MinecraftServer.LOGGER.error(
                    "Failed to refresh player visibility for {}",
                    viewer.username,
                    exception,
                )
            }
        }
    }

    private fun refresh(
        viewer: Player,
        handler: PlayerVisibilityHandler,
        instance: Instance,
        previous: PlayerVisibilityHandler.VisibilitySelection,
        instanceChanged: Boolean,
        ruleChanged: Boolean,
    ) {
        val previousIds = if (instanceChanged) {
            PlayerVisibilityHandler.EMPTY_IDS
        } else {
            previous.ids
        }

        selector.begin(
            previouslyVisible = previousIds,
            wasLimited = handler.visibilityLimited && !instanceChanged,
        )

        candidateIds.clear()

        if (viewer.autoViewEntities()) {
            val position = viewer.position

            instance.entityTracker.nearbyEntitiesByChunkRange(
                position,
                ServerFlag.ENTITY_VIEW_DISTANCE,
                EntityTracker.Target.PLAYERS,
            ) { target ->
                if (
                    target !== viewer &&
                    candidateIds.add(target.entityId) &&
                    target.instance === instance &&
                    target.isActive &&
                    !target.isRemoved &&
                    handler.allowsWithoutBudget(target) &&
                    AutomaticPlayerVisibility.targetAllows(target, viewer)
                ) {
                    selector.offer(
                        target.entityId,
                        position.distanceSquared(target.position),
                    )
                }
            }
        }

        val nextIds = selector.finish()
        handler.visibilityLimited = selector.limited

        handler.pausePlayerAdmission()

        try {
            val oldInstance = previous.instance

            if (oldInstance != null) {
                for (id in previous.ids) {
                    if (instanceChanged || nextIds.binarySearch(id) < 0) {
                        val target = oldInstance.getEntityById(id) as? Player
                            ?: continue

                        AutomaticPlayerVisibility.hide(target, viewer)
                    }
                }
            }

            handler.publishSelection(instance, nextIds)
        } finally {
            handler.resumePlayerAdmission()
        }

        for (id in nextIds) {
            val target = instance.getEntityById(id) as? Player
                ?: continue

            if (withinTrackingRange(viewer, target)) {
                AutomaticPlayerVisibility.show(target, viewer)
            }
        }

        if (ruleChanged) {
            handler.refreshNativeViewerRule()
        }
    }

    private fun withinTrackingRange(
        viewer: Player,
        target: Player,
    ): Boolean {
        val origin = viewer.position
        val position = target.position
        val range = ServerFlag.ENTITY_VIEW_DISTANCE

        return position.chunkX() >= origin.chunkX() - range &&
                position.chunkX() <= origin.chunkX() + range &&
                position.chunkZ() >= origin.chunkZ() - range &&
                position.chunkZ() <= origin.chunkZ() + range
    }
}
