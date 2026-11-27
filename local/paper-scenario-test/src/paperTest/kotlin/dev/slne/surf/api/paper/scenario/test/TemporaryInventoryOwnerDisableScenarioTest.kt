package dev.slne.surf.api.paper.scenario.test

import dev.slne.surf.api.paper.scenario.probe.TemporaryInventoryEndContextLog
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Ends temporary inventory sessions by disabling the plugin that owns them, once while the server
 * stops and once while it keeps running, and checks that each player gets the original inventory
 * back.
 *
 * The probe owns the sessions that end at shutdown, so it observes its own end event. A second
 * plugin owns the sessions that end at runtime, because a disabled probe observes nothing.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TemporaryInventoryOwnerDisableScenarioTest {

    private lateinit var scenario: ScenarioServer
    private lateinit var sessions: TemporaryInventoryProbe

    private val probe get() = scenario.probe

    @BeforeAll
    fun startServer() {
        scenario = ScenarioServer.start()
        sessions = TemporaryInventoryProbe(scenario.server, scenario.probe)
    }

    @AfterAll
    fun stopServer() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
    }

    @Test
    fun `stopping the server ends the session while its owner is still enabled`() {
        val bot = scenario.connect("Stopper")
        probe.give("Stopper", slot = 3, material = "DIRT", amount = 5)
        val sessionId = beginSession("Stopper")
        val position = probe.position("Stopper")

        scenario.stop()

        try {
            bot.awaitDisconnected()

            assertEquals(
                TemporaryInventoryProbe.EndEvent(
                    cause = "SHUTDOWN",
                    sessionId = sessionId,
                    state = SESSION_KEY,
                    discarded = TEMPORARY_ITEM,
                ),
                sessions.awaitEnd("Stopper", from = 0),
            )
            assertEquals(
                EndContext(cause = "SHUTDOWN", sessionId, ownsPlayer = true, isProbeEnabled = true),
                awaitEndContext("Stopper", from = 0),
            )
        } finally {
            scenario.start()
        }

        scenario.connect("Stopper")

        assertEquals(ORIGINAL_SLOTS, probe.inventory("Stopper").slots)
        assertEquals(NO_SESSION, sessions.session("Stopper"))
        assertEquals(0, sessions.endEventCount(sessionId))
        assertEquals(
            emptyList<String>(),
            probe.items(scenario.worldName, position, radius = DROP_RADIUS),
        )
    }

    @Test
    fun `disabling the owner at runtime ends its sessions on each player's thread`() {
        val sessionIds = RUNTIME_PLAYERS.associateWith { name ->
            scenario.connect(name)
            probe.give(name, slot = 3, material = "DIRT", amount = 5)
            beginSession(name, owner = RUNTIME_OWNER)
        }
        val beforeDisable = scenario.server.mark()

        probe.request("disable-plugin", RUNTIME_OWNER)

        for ((name, sessionId) in sessionIds) {
            assertEquals(
                TemporaryInventoryProbe.EndEvent(
                    cause = "OWNER_DISABLED",
                    sessionId = sessionId,
                    state = SESSION_KEY,
                    discarded = TEMPORARY_ITEM,
                ),
                sessions.awaitEnd(name, beforeDisable),
            )
            assertTrue(awaitEndContext(name, beforeDisable).ownsPlayer)
            assertEquals(ORIGINAL_SLOTS, probe.inventory(name).slots)
            assertEquals(NO_SESSION, sessions.session(name))
        }
    }

    /**
     * Begins a session that puts the temporary item into the first slot and returns its id.
     */
    private fun beginSession(player: String, owner: String? = null): String {
        val reply = if (owner == null) {
            sessions.begin(player, SESSION_KEY, "0:$TEMPORARY_ITEM")
        } else {
            probe.request("temp-begin-owned", player, owner, SESSION_KEY, "0:$TEMPORARY_ITEM")
        }

        assertTrue(reply.startsWith("started "), reply)

        return reply.substringAfter("started ")
    }

    private fun awaitEndContext(player: String, from: Int): EndContext {
        val (cause, sessionId, ownsPlayer, isProbeEnabled) = probe
            .awaitEvent(TemporaryInventoryEndContextLog.END_CONTEXT_EVENT, player, from)
            .split(' ')

        return EndContext(
            cause,
            sessionId,
            ownsPlayer.toBooleanStrict(),
            isProbeEnabled.toBooleanStrict(),
        )
    }

    /**
     * Where an end event fired.
     *
     * @property ownsPlayer whether the firing thread owned the player
     * @property isProbeEnabled whether the probe was still enabled
     */
    private data class EndContext(
        val cause: String,
        val sessionId: String,
        val ownsPlayer: Boolean,
        val isProbeEnabled: Boolean,
    )

    private companion object {

        const val SESSION_KEY = "probe:owner"

        const val TEMPORARY_ITEM = "DIAMOND_PICKAXE:1"

        const val NO_SESSION = "none"

        const val DROP_RADIUS = 8

        /**
         * A plugin besides the probe that the server runs, so the probe keeps observing after it
         * was disabled.
         */
        const val RUNTIME_OWNER = "SurfSharedBeanProvider"

        val RUNTIME_PLAYERS = listOf("OwnedOne", "OwnedTwo")

        val ORIGINAL_SLOTS = mapOf(3 to "DIRT:5")
    }
}
