package dev.slne.surf.api.paper.scenario.test

import dev.slne.surf.api.paper.scenario.probe.ProbeProtocol
import dev.slne.surf.api.paper.scenario.test.TemporaryInventoryProbe.EndEvent
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Crashes the server while bots carry temporary inventories and checks that each bot gets its
 * original inventory back when it joins the restarted server.
 *
 * Every case shares one crash: [crashDuringSessions] prepares one bot per case, saves the game,
 * kills the server, removes the owner plugin and the data pack of the unreadable item, and starts
 * the server again. Each test then joins its own bot.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TemporaryInventoryRecoveryScenarioTest {

    private lateinit var scenario: ScenarioServer
    private lateinit var sessions: TemporaryInventoryProbe

    private val probe get() = scenario.probe
    private val crashed = mutableMapOf<String, CrashedSession>()

    @BeforeAll
    fun crashDuringSessions() {
        scenario = ScenarioServer.start()
        sessions = TemporaryInventoryProbe(scenario.server, scenario.probe)

        installDataPack()
        scenario.stop()
        scenario.start()

        crashed[SAVED] = beginWithFilledInventory(SAVED, "probe:saved")
        crashed[REJOINED] = beginWithFilledInventory(REJOINED, "probe:rejoined")
        crashed[ENDED_AFTER_SAVE] = beginWithFilledInventory(ENDED_AFTER_SAVE, "probe:ended")
        crashed[ORPHANED] = beginOrphanedSession()
        crashed[UNREADABLE] = beginWithUnreadableItem()

        saveGame()
        assertTrue(sessions.end(ENDED_AFTER_SAVE))

        scenario.kill()
        uninstallOwnerPlugin()
        uninstallDataPack()
        scenario.start()
    }

    @AfterAll
    fun stopServer() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
    }

    @Test
    fun `a session saved before a crash is recovered at the next join`() {
        val session = crashed.getValue(SAVED)

        assertRecovered(session, rejoin(SAVED))
    }

    @Test
    fun `a session that ended after the last save is recovered without duplicating items`() {
        val session = crashed.getValue(ENDED_AFTER_SAVE)

        assertRecovered(session, rejoin(ENDED_AFTER_SAVE))
    }

    @Test
    fun `a session is recovered after its owning plugin was removed`() {
        val session = crashed.getValue(ORPHANED)

        assertRecovered(session, rejoin(ORPHANED))
    }

    @Test
    fun `an unreadable original item stays stored while the other slots are restored`() {
        val session = crashed.getValue(UNREADABLE)
        val beforeJoin = scenario.server.mark()
        val event = rejoin(UNREADABLE)

        assertEquals(session.recoveredEvent(), event)
        assertEquals(1, sessions.endEventCount(session.sessionId))
        assertEquals(
            session.original.slots - UNREADABLE_SLOT,
            probe.inventory(UNREADABLE).slots,
        )
        scenario.server.awaitLine(
            "that the unreadable slot stays in the player's data",
            from = beforeJoin,
        ) { line ->
            "Slot $UNREADABLE_SLOT of $UNREADABLE's original inventory" in line &&
                "it stays in the player's data" in line
        }
        assertNoItemsAround(UNREADABLE)
    }

    @Test
    fun `a recovered session fires its end event once and frees the player for a new session`() {
        val session = crashed.getValue(REJOINED)
        val beforeJoin = scenario.server.mark()
        val bot = scenario.connect(REJOINED)
        assertRecovered(session, sessions.awaitEnd(REJOINED, beforeJoin))

        val beforeQuit = scenario.server.mark()
        bot.disconnect()
        probe.awaitEvent(ProbeProtocol.QUIT, REJOINED, beforeQuit)
        scenario.connect(REJOINED)

        assertEquals(1, sessions.endEventCount(session.sessionId))
        assertEquals(session.original.slots, probe.inventory(REJOINED).slots)

        val begin = sessions.begin(REJOINED, "probe:again", "0:STICK:1")
        assertTrue(begin.startsWith("started "), begin)
        assertTrue(sessions.end(REJOINED))
    }

    private fun beginWithFilledInventory(name: String, key: String): CrashedSession {
        val bot = scenario.connect(name)
        sessions.fillSlots(name, "STONE")
        bot.selectHotbarSlot(HELD_SLOT)
        val original = probe.awaitInventory(name, "every slot filled, hotbar slot 4 selected") {
            it.heldSlot == HELD_SLOT && it.slots.size == PLAYER_SLOTS
        }

        return startedSession(name, key, original) {
            sessions.begin(name, key, TEMPORARY_ITEMS)
        }
    }

    private fun beginOrphanedSession(): CrashedSession {
        scenario.connect(ORPHANED)
        probe.give(ORPHANED, slot = 7, material = "COBBLESTONE", amount = 12)
        val original = probe.inventory(ORPHANED)
        val key = "probe:orphaned"

        return startedSession(ORPHANED, key, original) {
            probe.request("temp-begin-owned", ORPHANED, OWNER_PLUGIN, key, TEMPORARY_ITEMS)
        }
    }

    private fun beginWithUnreadableItem(): CrashedSession {
        scenario.connect(UNREADABLE)
        probe.give(UNREADABLE, slot = 0, material = "DIRT", amount = 5)
        probe.request(
            "give-enchanted",
            UNREADABLE,
            UNREADABLE_SLOT,
            "DIAMOND_SWORD",
            DATA_PACK_ENCHANTMENT,
        )
        val original = probe.inventory(UNREADABLE)
        assertEquals(mapOf(0 to "DIRT:5", UNREADABLE_SLOT to "DIAMOND_SWORD:1"), original.slots)
        val key = "probe:unreadable"

        return startedSession(UNREADABLE, key, original) {
            sessions.begin(UNREADABLE, key, TEMPORARY_ITEMS)
        }
    }

    /**
     * Runs [beginSession], asserts that it started a session with the temporary inventory, and
     * returns the session.
     */
    private fun startedSession(
        name: String,
        key: String,
        original: InventoryReport,
        beginSession: () -> String,
    ): CrashedSession {
        val reply = beginSession()
        assertTrue(reply.startsWith("started "), reply)
        assertEquals(mapOf(1 to "DIAMOND_PICKAXE:1"), probe.inventory(name).slots)

        return CrashedSession(name, key, reply.substringAfter("started "), original)
    }

    /**
     * Saves the game as an autosave does and waits until it finished.
     */
    private fun saveGame() {
        val beforeSave = scenario.server.mark()

        scenario.server.sendConsoleCommand("save-all")
        scenario.server.awaitLine(SAVED_THE_GAME, from = beforeSave)
    }

    /**
     * Joins the bot [name] and returns the end event its join fired.
     */
    private fun rejoin(name: String): EndEvent {
        val beforeJoin = scenario.server.mark()
        scenario.connect(name)

        return sessions.awaitEnd(name, beforeJoin)
    }

    private fun assertRecovered(session: CrashedSession, event: EndEvent) {
        assertEquals(session.recoveredEvent(), event)

        val restored = probe.inventory(session.player)
        assertEquals(session.original.slots, restored.slots)
        assertEquals(session.original.heldSlot, restored.heldSlot)
        assertEquals(NO_SESSION, sessions.session(session.player))
        assertEquals(1, sessions.endEventCount(session.sessionId))
        assertNoItemsAround(session.player)
    }

    private fun assertNoItemsAround(player: String) {
        assertEquals(
            emptyList<String>(),
            probe.items(scenario.worldName, probe.position(player), radius = DROP_RADIUS),
        )
    }

    /**
     * Installs a data pack with the enchantment [DATA_PACK_ENCHANTMENT]; it takes effect at the
     * next start.
     */
    private fun installDataPack() {
        val pack = dataPackDirectory()

        pack.resolve(ENCHANTMENT_FILE).parent.createDirectories()
        pack.resolve("pack.mcmeta").writeText(PACK_MCMETA)
        pack.resolve(ENCHANTMENT_FILE).writeText(ENCHANTMENT_JSON)
    }

    @OptIn(ExperimentalPathApi::class)
    private fun uninstallDataPack() {
        dataPackDirectory().deleteRecursively()
    }

    private fun uninstallOwnerPlugin() {
        val jars = scenario.server.directory.path
            .resolve("plugins")
            .listDirectoryEntries("$OWNER_JAR_PREFIX*.jar")

        assertEquals(1, jars.size, "owner plugin jars: $jars")

        for (jar in jars) {
            jar.deleteIfExists()
        }
    }

    private fun dataPackDirectory(): Path {
        return scenario.server.directory.path
            .resolve(scenario.worldName)
            .resolve("datapacks")
            .resolve(DATA_PACK)
    }

    /**
     * A bot whose session was active when the server crashed.
     *
     * @property original the inventory before the session began
     */
    private class CrashedSession(
        val player: String,
        val key: String,
        val sessionId: String,
        val original: InventoryReport,
    ) {

        fun recoveredEvent(): EndEvent {
            return EndEvent(
                cause = "RECOVERED",
                sessionId = sessionId,
                state = key,
                discarded = "DIAMOND_PICKAXE:1",
            )
        }
    }

    private companion object {

        const val SAVED = "Saved"
        const val REJOINED = "Rejoined"
        const val ENDED_AFTER_SAVE = "EndedAfterSave"
        const val ORPHANED = "Orphaned"
        const val UNREADABLE = "Unreadable"

        const val TEMPORARY_ITEMS = "1:DIAMOND_PICKAXE:1"

        const val HELD_SLOT = 4

        /**
         * Hotbar, main inventory, armor, offhand, body armor and saddle.
         */
        const val PLAYER_SLOTS = 43

        const val UNREADABLE_SLOT = 2

        const val NO_SESSION = "none"

        const val DROP_RADIUS = 8

        const val SAVED_THE_GAME = "Saved the game"

        const val OWNER_PLUGIN = "SurfScenarioOwner"

        const val OWNER_JAR_PREFIX = "surf-api-paper-scenario-owner"

        const val DATA_PACK = "surf_scenario_probe"

        const val DATA_PACK_ENCHANTMENT = "surf_probe:unreadable"

        const val ENCHANTMENT_FILE = "data/surf_probe/enchantment/unreadable.json"

        /**
         * Accepts every data pack format from the first one with minor versions on.
         */
        val PACK_MCMETA = """
            {
              "pack": {"description": "surf-api scenario test", "min_format": 82, "max_format": 1000}
            }
        """.trimIndent()

        val ENCHANTMENT_JSON = """
            {
              "description": "Unreadable",
              "supported_items": "minecraft:diamond_sword",
              "weight": 1,
              "max_level": 1,
              "min_cost": {"base": 1, "per_level_above_first": 1},
              "max_cost": {"base": 1, "per_level_above_first": 1},
              "anvil_cost": 1,
              "slots": ["mainhand"]
            }
        """.trimIndent()
    }
}
