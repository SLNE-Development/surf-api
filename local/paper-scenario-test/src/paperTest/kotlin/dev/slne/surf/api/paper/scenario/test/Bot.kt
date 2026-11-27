package dev.slne.surf.api.paper.scenario.test

import org.cloudburstmc.math.vector.Vector3i
import org.geysermc.mcprotocollib.network.Session
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory
import org.geysermc.mcprotocollib.network.packet.Packet
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol
import org.geysermc.mcprotocollib.protocol.data.game.ClientCommand
import org.geysermc.mcprotocollib.protocol.data.game.entity.`object`.Direction
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType
import org.geysermc.mcprotocollib.protocol.data.game.item.HashedStack
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerClosePacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundChunkBatchFinishedPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundPlayerLoadedPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundChunkBatchReceivedPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket
import org.junit.jupiter.api.Assertions
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * A player connection that a scenario test drives, built on MCProtocolLib.
 *
 * The bot joins an offline-mode server under [name], answers what the server requires of a client
 * (keep-alives, teleport confirmations, chunk batch acknowledgements, the loaded signal after
 * spawning and respawning) and sends the actions a test asks for. It tracks only what it needs to
 * build its packets: the open container and the state id of each container.
 *
 * What the bot believes is no evidence: tests read the server's state through the probe.
 *
 * Packets arrive on MCProtocolLib's threads; the tracked state is safe to read from the test
 * thread.
 */
class Bot private constructor(val name: String, port: Int) : AutoCloseable {

    private val session = ClientNetworkSessionFactory.factory()
        .setAddress(HOST, port)
        .setProtocol(MinecraftProtocol(name))
        .create()

    private val stateIds = ConcurrentHashMap<Int, Int>()
    private val sequence = AtomicInteger()

    @Volatile
    private var openContainer: OpenContainer? = null

    @Volatile
    private var awaitingLoaded = false

    @Volatile
    private var isSpawned = false

    @Volatile
    private var isDead = false

    @Volatile
    private var disconnectReason: String? = null

    /**
     * `true` while the connection is open.
     */
    val isConnected: Boolean get() = session.isConnected

    init {
        session.addListener(PacketHandler())
    }

    /**
     * Switches the selected hotbar slot, 0 to 8.
     */
    fun selectHotbarSlot(slot: Int) {
        session.send(ServerboundSetCarriedItemPacket(slot))
    }

    /**
     * Right-clicks the top face of the block at the position with the main hand, which opens a
     * chest or a crafting table there.
     */
    fun useBlock(position: BlockPosition) {
        session.send(
            ServerboundUseItemOnPacket(
                Vector3i.from(position.x, position.y, position.z),
                Direction.UP,
                Hand.MAIN_HAND,
                HALF_BLOCK,
                FULL_BLOCK,
                HALF_BLOCK,
                false,
                false,
                sequence.incrementAndGet(),
            ),
        )
    }

    /**
     * Waits until the server opened a container for the bot, and returns its type.
     */
    fun awaitOpenContainer(): ContainerType {
        awaitCondition("the server to open a container") { openContainer != null }

        return checkNotNull(openContainer).type
    }

    /**
     * Left-clicks [slot] of the open container, or of the player's own inventory when none is
     * open, as a click without modifier keys does.
     *
     * The click carries no prediction of the result; the server applies it and sends the true
     * contents back.
     */
    fun click(slot: Int) {
        val containerId = openContainer?.id ?: PLAYER_INVENTORY

        session.send(
            ServerboundContainerClickPacket(
                containerId,
                stateIds[containerId] ?: 0,
                slot,
                ContainerActionType.CLICK_ITEM,
                ClickItemAction.LEFT_CLICK,
                null,
                emptyMap<Int, HashedStack>(),
            ),
        )
    }

    /**
     * Closes the open container, as pressing escape does.
     */
    fun closeContainer() {
        val container = openContainer ?: return

        openContainer = null
        session.send(ServerboundContainerClosePacket(container.id))
    }

    /**
     * Waits until the server told the bot that it died.
     */
    fun awaitDeath() {
        awaitCondition("the server to report the bot's death") { isDead }
    }

    /**
     * Presses the respawn button and waits until the bot spawned again.
     */
    fun respawn() {
        isSpawned = false
        session.send(ServerboundClientCommandPacket(ClientCommand.PERFORM_RESPAWN))
        awaitCondition("the bot to respawn") { isSpawned && !isDead }
    }

    /**
     * Closes the connection, as quitting the game does.
     */
    fun disconnect() {
        session.disconnect("Disconnected by the scenario test")
        awaitCondition("the connection to close") { !session.isConnected }
    }

    /**
     * Waits until the server closed the connection, for example because it stopped or died.
     */
    fun awaitDisconnected(timeout: Duration = WAIT_TIMEOUT) {
        awaitCondition("the server to close the connection", timeout) { !session.isConnected }
    }

    override fun close() {
        if (session.isConnected) {
            session.disconnect("Closed by the scenario test")
        }
    }

    private fun connect(timeout: Duration) {
        session.connect(true)
        awaitCondition("the bot to spawn", timeout) { isSpawned }
    }

    private fun awaitCondition(
        description: String,
        timeout: Duration = WAIT_TIMEOUT,
        condition: () -> Boolean,
    ) {
        val deadline = System.nanoTime() + timeout.toNanos()

        while (System.nanoTime() < deadline) {
            if (condition()) return

            if (!session.isConnected && disconnectReason != null) {
                Assertions.fail<Unit>(
                    "Bot $name was disconnected while waiting for $description: " +
                        disconnectReason,
                )
            }

            Thread.sleep(POLL.toMillis())
        }

        Assertions.fail<Unit>("Bot $name waited ${timeout.toSeconds()}s for $description.")
    }

    /**
     * Answers the server and records what the bot's next packets need.
     */
    private inner class PacketHandler : SessionAdapter() {

        override fun packetReceived(session: Session, packet: Packet) {
            when (packet) {
                is ClientboundLoginPacket, is ClientboundRespawnPacket -> awaitingLoaded = true
                is ClientboundPlayerPositionPacket -> acceptTeleport(session, packet)
                is ClientboundChunkBatchFinishedPacket -> {
                    session.send(ServerboundChunkBatchReceivedPacket(CHUNKS_PER_TICK))
                }

                is ClientboundOpenScreenPacket -> {
                    openContainer = OpenContainer(packet.containerId, packet.type)
                }

                is ClientboundContainerSetContentPacket -> {
                    stateIds[packet.containerId] = packet.stateId
                }

                is ClientboundContainerSetSlotPacket -> {
                    stateIds[packet.containerId] = packet.stateId
                }

                is ClientboundContainerClosePacket -> openContainer = null
                is ClientboundPlayerCombatKillPacket -> isDead = true
            }
        }

        override fun disconnected(event: DisconnectedEvent) {
            disconnectReason = "${event.reason}" + (event.cause?.let { " ($it)" } ?: "")
        }

        /**
         * Confirms a teleport, and signals after the first position of a spawn that the client
         * finished loading, which the server waits for before the player counts as spawned.
         */
        private fun acceptTeleport(session: Session, packet: ClientboundPlayerPositionPacket) {
            val position = packet.position

            session.send(
                ServerboundAcceptTeleportationPacket(
                    packet.id,
                    position.x,
                    position.y,
                    position.z,
                    packet.yRot,
                    packet.xRot,
                ),
            )

            if (awaitingLoaded) {
                awaitingLoaded = false
                session.send(ServerboundPlayerLoadedPacket.INSTANCE)
                isDead = false
                isSpawned = true
            }
        }
    }

    private class OpenContainer(val id: Int, val type: ContainerType)

    companion object {

        private const val HOST = "127.0.0.1"

        /**
         * The container id of the player's own inventory, which is open whenever no other
         * container is.
         */
        private const val PLAYER_INVENTORY = 0

        private const val HALF_BLOCK = 0.5f

        private const val FULL_BLOCK = 1.0f

        /**
         * What a vanilla client asks for once it has caught up.
         */
        private const val CHUNKS_PER_TICK = 20f

        private val WAIT_TIMEOUT: Duration = Duration.ofSeconds(15)

        private val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(30)

        private val POLL: Duration = Duration.ofMillis(50)

        /**
         * Joins the server on [port] as [name] and waits until the player spawned.
         *
         * @param name a valid player name: 3 to 16 letters, digits or underscores
         */
        fun connect(name: String, port: Int): Bot {
            val bot = Bot(name, port)
            bot.connect(CONNECT_TIMEOUT)

            return bot
        }
    }
}

/**
 * A block position in the world.
 */
data class BlockPosition(val x: Int, val y: Int, val z: Int)
