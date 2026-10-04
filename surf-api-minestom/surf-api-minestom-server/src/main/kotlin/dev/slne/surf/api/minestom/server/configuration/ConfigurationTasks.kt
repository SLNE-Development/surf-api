package dev.slne.surf.api.minestom.server.configuration

import dev.slne.surf.api.minestom.extension.PacketListenerManager
import dev.slne.surf.api.minestom.extension.addListener
import dev.slne.surf.api.minestom.server.impl.configuration.ConfigurationPhase
import dev.slne.surf.api.minestom.server.impl.player.PlayerEvents
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.MinecraftServer
import net.minestom.server.ServerFlag
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerSettingsChangeEvent
import net.minestom.server.network.packet.client.configuration.ClientAcceptCodeOfConductPacket
import net.minestom.server.network.packet.server.common.PluginMessagePacket
import net.minestom.server.network.packet.server.configuration.CodeOfConductPacket
import net.minestom.server.network.packet.server.configuration.FinishConfigurationPacket
import net.minestom.server.network.packet.server.configuration.ResetChatPacket
import net.minestom.server.network.packet.server.configuration.SelectKnownPacksPacket
import net.minestom.server.network.packet.server.configuration.UpdateEnabledFeaturesPacket
import net.minestom.server.registry.Registries
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The steps of the configuration phase, in the order vanilla runs them.
 *
 * Every step can be replaced or removed through [ConfigurationPhaseBuilder].
 */
object ConfigurationTasks {
    private val LOGGER = ComponentLogger.logger()

    /**
     * Calls [dev.slne.surf.api.minestom.player.event.AsyncPlayerCountEvent] and kicks a refused
     * player. Only on the first configuration.
     */
    val PLAYER_COUNT = ConfigurationTaskId("player_count")

    /** Sends the server brand. */
    val BRAND = ConfigurationTaskId("brand")

    /** Calls [AsyncPlayerConfigurationEvent] and stores it in the [ConfigurationContext]. */
    val CONFIGURATION_EVENT = ConfigurationTaskId("configuration_event")

    /**
     * Calls [dev.slne.surf.api.minestom.player.event.AsyncPlayerSpawnLocationEvent] and applies
     * the location it chose. Only on the first configuration.
     */
    val SPAWN_LOCATION = ConfigurationTaskId("spawn_location")

    /** Sends the feature flags the configuration event collected and clears the chat if asked to. */
    val ENABLED_FEATURES = ConfigurationTaskId("enabled_features")

    /** Negotiates the known packs and sends the registry data. */
    val SYNCHRONIZE_REGISTRIES = ConfigurationTaskId("synchronize_registries")

    /** Waits until the client's settings, and with them its locale, have arrived. */
    val AWAIT_SETTINGS = ConfigurationTaskId("await_settings")

    /** Shows the code of conduct and waits until the player accepts it. Not part of the defaults. */
    val CODE_OF_CONDUCT = ConfigurationTaskId("code_of_conduct")

    /** Waits for the resource packs Minestom queued for the player. */
    val RESOURCE_PACK = ConfigurationTaskId("resource_pack")

    /** Hands the player over to the play phase. Always the last step. */
    val JOIN_WORLD = ConfigurationTaskId("join_world")

    /**
     * Stops the keep alives the configuration phase sends.
     *
     * Has to be called by a step that hands the player over to the play phase in place of
     * [JOIN_WORLD], right before it sends the finish configuration packet.
     */
    fun stopKeepAlive(player: Player) = ConfigurationPhase.stopKeepAlive(player)

    val playerCount = ConfigurationTask { context ->
        if (context.isFirstConfig) PlayerEvents.admit(context.player)
    }

    val spawnLocation = ConfigurationTask { context ->
        if (context.isFirstConfig) PlayerEvents.chooseSpawnLocation(context.event)
    }

    val brand = ConfigurationTask { context ->
        context.player.sendPacket(PluginMessagePacket.brandPacket(MinecraftServer.getBrandName()))
    }

    val configurationEvent = ConfigurationTask { context ->
        val event = AsyncPlayerConfigurationEvent(context.player, context.isFirstConfig)
        EventDispatcher.call(event)
        context.event = event
    }

    val enabledFeatures = ConfigurationTask { context ->
        val event = context.event

        context.player.sendPacket(
            UpdateEnabledFeaturesPacket(event.featureFlags.map { it.name() })
        )

        if (event.willClearChat()) {
            context.player.sendPacket(ResetChatPacket())
        }
    }

    @Suppress("UnstableApiUsage")
    val synchronizeRegistries = ConfigurationTask { context ->
        if (!context.event.willSendRegistryData()) return@ConfigurationTask

        val player = context.player
        val response = player.playerConnection
            .requestKnownPacks(listOf(SelectKnownPacksPacket.MINECRAFT_CORE))

        val knownPacks = try {
            response.get(ServerFlag.KNOWN_PACKS_RESPONSE_TIMEOUT, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            LOGGER.warn("Player {} failed to respond to known packs query", player.username)
            player.playerConnection.disconnect()
            return@ConfigurationTask
        }

        val excludeVanilla = knownPacks.contains(SelectKnownPacksPacket.MINECRAFT_CORE)

        player.sendPackets(
            Registries.registryDataPackets(MinecraftServer.getRegistries(), excludeVanilla)
        )
        MinecraftServer.getConnectionManager().sendRegistryTags(player)
    }

    @Suppress("UnstableApiUsage")
    val resourcePack = ConfigurationTask { context ->
        context.player.resourcePackFuture?.join()
    }

    @Suppress("UnstableApiUsage")
    val joinWorld = ConfigurationTask { context ->
        val player = context.player

        stopKeepAlive(player)
        player.setPendingOptions(context.spawningInstance, context.event.isHardcore)
        player.sendPacket(FinishConfigurationPacket())
    }

    /**
     * Waits until the client's settings have arrived, using the defaults after [timeout].
     */
    class AwaitSettings(private val timeout: Duration = 2.seconds) : ConfigurationTask {
        private val received = ConcurrentHashMap<UUID, CompletableFuture<Unit>>()

        override fun run(context: ConfigurationContext) {
            try {
                settingsFuture(context.player.uuid)
                    .get(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            } catch (_: TimeoutException) {
                LOGGER.warn(
                    "Player {} did not send their settings, continuing with the defaults",
                    context.player.username
                )
            }
        }

        override fun install(node: EventNode<Event>) {
            node.addListener<PlayerSettingsChangeEvent> { event ->
                settingsFuture(event.player.uuid).complete(Unit)
            }
            node.addListener<PlayerDisconnectEvent> { event ->
                received.remove(event.player.uuid)?.complete(Unit)
            }
        }

        private fun settingsFuture(uuid: UUID): CompletableFuture<Unit> =
            received.computeIfAbsent(uuid) { CompletableFuture() }
    }

    /**
     * Sends the code of conduct [text] returns for a player and holds the configuration until the
     * player accepts it.
     *
     * Only shown on the first configuration. A player for whom [text] returns `null` is let through.
     */
    class CodeOfConduct(private val text: (Player) -> String?) : ConfigurationTask {
        private val pending = ConcurrentHashMap<UUID, CompletableFuture<Unit>>()

        override fun run(context: ConfigurationContext) {
            if (!context.isFirstConfig) return

            val player = context.player
            val codeOfConduct = text(player) ?: return

            val future = CompletableFuture<Unit>()
            val pendingFuture = pending.putIfAbsent(player.uuid, future) ?: future

            if (!player.isOnline) {
                pending.remove(player.uuid, future)
                return
            }

            if (pendingFuture === future) {
                player.sendPacket(CodeOfConductPacket(codeOfConduct))
            } else {
                LOGGER.warn(
                    "Configuration ran again for {} while its code of conduct was still pending",
                    player.username
                )
            }

            try {
                pendingFuture.join()
            } catch (_: CancellationException) {
                LOGGER.info("Player {} left before accepting the code of conduct", player.username)
                player.playerConnection.disconnect()
            }
        }

        override fun install(node: EventNode<Event>) {
            node.addListener<PlayerDisconnectEvent> { event ->
                pending.remove(event.player.uuid)?.cancel(true)
            }
            PacketListenerManager.setConfigurationListener(
                ClientAcceptCodeOfConductPacket::class.java,
                ::handleAccept,
            )
        }

        private fun handleAccept(
            @Suppress("unused") packet: ClientAcceptCodeOfConductPacket,
            player: Player,
        ) {
            val future = pending.remove(player.uuid)
            if (future == null) {
                LOGGER.warn("Player {} accepted a code of conduct that was never sent", player.username)
                player.playerConnection.disconnect()
                return
            }
            future.complete(Unit)
        }
    }
}
