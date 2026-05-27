package dev.slne.surf.api.paper.server.command

import com.destroystokyo.paper.event.brigadier.AsyncPlayerSendCommandsEvent
import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.shynixn.mccoroutine.folia.launch
import com.google.auto.service.AutoService
import dev.jorel.commandapi.ExecutableCommand
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.paper.command.executors.CoroutineScopeProvider
import dev.slne.surf.api.paper.command.requirement.SuspendRequirementService
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.CommandSendPacketBlockerListener
import dev.slne.surf.api.paper.nms.common.NmsProvider
import dev.slne.surf.api.paper.server.plugin
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.*
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

@AutoService(SuspendRequirementService::class)
class SuspendRequirementServiceImpl : SuspendRequirementService {
    init {
        checkInstantiationByServiceLoader()
    }

    companion object {
        private val log = logger()

        fun get() = SuspendRequirementService.instance as SuspendRequirementServiceImpl
    }

    private val requirements = Caffeine.newBuilder()
        .build<UUID, Requirement>()

    private val blockedCommandPackets = ConcurrentHashMap.newKeySet<UUID>()
    private val ready = ConcurrentHashMap.newKeySet<UUID>()

    private val listener = EventListener()

    @OptIn(NmsUseWithCaution::class)
    private val commandSendPacketBlockerListener =
        NmsProvider.current.createCommandSendPacketBlockerListener(blockedCommandPackets)

    fun getCommandSendPacketBlockerListener(): CommandSendPacketBlockerListener {
        return commandSendPacketBlockerListener
    }

    fun getEventListener(): Listener {
        return listener
    }

    override fun <Impl : ExecutableCommand<Impl, CommandSender>> ExecutableCommand<Impl, CommandSender>.withSuspendRequirement(
        scope: CoroutineScopeProvider,
        requirement: suspend CoroutineScope.(Player) -> Boolean,
        allowIfNonPlayer: Boolean
    ) {
        val id = UUID.randomUUID()
        register(id, scope, requirement)
        withRequirement {
            if (it !is Player) {
                allowIfNonPlayer
            } else {
                testCached(id, it)
            }
        }
    }

    private fun register(
        id: UUID,
        scope: CoroutineScopeProvider,
        requirement: suspend CoroutineScope.(Player) -> Boolean
    ) {
        requirements.put(id, Requirement(scope, requirement))
    }

    private fun testCached(id: UUID, sender: Player): Boolean {
        val requirement = requirements.getIfPresent(id) ?: return false
        return requirement.testCached(sender)
    }

    suspend fun refreshForSender(sender: Player) {
        coroutineScope {
            for (requirement in requirements.asMap().values) {
                launch {
                    requirement.refreshForSender(sender)
                }
            }
        }

        ready.add(sender.uniqueId)
        sender.updateCommands()
    }

    fun triggerRefreshForSender(sender: Player) {
        plugin.launch {
            refreshForSender(sender)
        }
    }

    inner class EventListener : Listener {

        @EventHandler
        @Suppress("UnstableApiUsage")
        fun onAsyncPlayerSendCommand(event: AsyncPlayerSendCommandsEvent<CommandSourceStack>) {
            if (requirements.asMap().isEmpty()) return

            if (event.isAsynchronous || !event.hasFiredAsync()) {
                val uuid = event.player.uniqueId

                if (ready.remove(uuid)) {
                    blockedCommandPackets.remove(uuid)
                } else {
                    if (blockedCommandPackets.add(uuid)) {
                        triggerRefreshForSender(event.player)
                    }
                }
            }
        }

        @EventHandler
        fun onConnectionClosed(event: PlayerConnectionCloseEvent) {
            blockedCommandPackets.remove(event.playerUniqueId)
            ready.remove(event.playerUniqueId)
            commandSendPacketBlockerListener.removeReceivedCommandPacket(event.playerUniqueId)
            requirements.asMap().values.forEach { it.invalidate(event.playerUniqueId) }
        }
    }

    data class Requirement(
        val scope: CoroutineScopeProvider,
        val requirement: suspend CoroutineScope.(Player) -> Boolean
    ) {
        private val cached = Caffeine.newBuilder()
            .build<UUID, Boolean>()

        fun testCached(sender: Player): Boolean {
            return cached.getIfPresent(sender.uniqueId) ?: false
        }

        fun invalidate(uuid: UUID) {
            cached.invalidate(uuid)
        }

        suspend fun refreshForSender(sender: Player) {
            withContext(scope().coroutineContext.minusKey(Job)) {
                try {
                    cached.put(sender.uniqueId, requirement(sender))
                } catch (e: Throwable) {
                    if (e is CancellationException) throw e
                    log.atWarning()
                        .withCause(e)
                        .log("Failed to check requirement for sender $sender! Not updating cache.")
                }
            }
        }
    }
}