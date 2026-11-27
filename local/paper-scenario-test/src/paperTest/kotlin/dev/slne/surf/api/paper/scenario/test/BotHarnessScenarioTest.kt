package dev.slne.surf.api.paper.scenario.test

import dev.slne.surf.api.paper.scenario.probe.ProbeProtocol
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Proves the scenario harness itself: bots join, act and leave, the probe reports what the server
 * sees, and the server survives a kill on the same world.
 *
 * Every test joins bots under names of its own, so the tests do not depend on each other or on
 * their order.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BotHarnessScenarioTest {

    private lateinit var scenario: ScenarioServer

    private val probe get() = scenario.probe

    @BeforeAll
    fun startServer() {
        scenario = ScenarioServer.start()
    }

    @AfterAll
    fun stopServer() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
    }

    @Test
    fun `the probe reads back an item it gave a joined bot`() {
        scenario.connect("Receiver")

        assertEquals(emptyMap<Int, String>(), probe.inventory("Receiver").slots)

        probe.give("Receiver", slot = 0, material = "DIRT", amount = 5)

        assertEquals(mapOf(0 to "DIRT:5"), probe.inventory("Receiver").slots)
    }

    @Test
    fun `a bot disconnects and rejoins`() {
        val bot = scenario.connect("Returner")
        val beforeQuit = scenario.server.mark()

        bot.disconnect()

        probe.awaitEvent(ProbeProtocol.QUIT, "Returner", beforeQuit)
        assertFalse(bot.isConnected)

        scenario.connect("Returner")

        assertEquals("CRAFTING", probe.inventory("Returner").view)
    }

    @Test
    fun `a bot selects a hotbar slot`() {
        val bot = scenario.connect("Selector")

        bot.selectHotbarSlot(4)

        probe.awaitInventory("Selector", "hotbar slot 4 selected") { it.heldSlot == 4 }
    }

    @Test
    fun `a bot opens a chest and takes an item onto the cursor`() {
        val bot = scenario.connect("Chester")
        probe.give("Chester", slot = 0, material = "DIRT", amount = 5)
        val chest = probe.place("Chester", "CHEST")
        val beforeOpen = scenario.server.mark()

        bot.useBlock(chest)

        assertEquals("CHEST", probe.awaitEvent(ProbeProtocol.OPEN, "Chester", beforeOpen))
        bot.awaitOpenContainer()

        bot.click(CHEST_HOTBAR_START)

        val report = probe.awaitInventory("Chester", "the dirt on the cursor") { report ->
            report.cursor == "DIRT:5"
        }
        assertEquals("CHEST", report.view)
        assertEquals(emptyMap<Int, String>(), report.slots)
    }

    @Test
    fun `a bot opens a crafting table and closes it again`() {
        val bot = scenario.connect("Crafter")
        val table = probe.place("Crafter", "CRAFTING_TABLE")
        val beforeOpen = scenario.server.mark()

        bot.useBlock(table)

        assertEquals("WORKBENCH", probe.awaitEvent(ProbeProtocol.OPEN, "Crafter", beforeOpen))
        bot.awaitOpenContainer()
        assertEquals("WORKBENCH", probe.inventory("Crafter").view)

        val beforeClose = scenario.server.mark()
        bot.closeContainer()

        probe.awaitEvent(ProbeProtocol.CLOSE, "Crafter", beforeClose)
        probe.awaitInventory("Crafter", "no open container") { it.view == "CRAFTING" }
    }

    @Test
    fun `a bot dies, drops its items and respawns`() {
        val bot = scenario.connect("Victim")
        probe.give("Victim", slot = 0, material = "OAK_LOG", amount = 5)
        val deathPosition = probe.position("Victim")
        val beforeDeath = scenario.server.mark()

        probe.kill("Victim")

        probe.awaitEvent(ProbeProtocol.DEATH, "Victim", beforeDeath)
        bot.awaitDeath()
        assertEquals(
            listOf("OAK_LOG:5"),
            probe.items(scenario.worldName, deathPosition, radius = DROP_RADIUS),
        )

        bot.respawn()

        probe.awaitEvent(ProbeProtocol.RESPAWN, "Victim", beforeDeath)
        assertEquals(emptyMap<Int, String>(), probe.inventory("Victim").slots)
    }

    @Test
    fun `a killed server restarts on the same world and a bot rejoins`() {
        val bot = scenario.connect("Survivor")
        probe.give("Survivor", slot = 3, material = "DIRT", amount = 7)
        val world = probe.world()

        // The playerdata on disk then holds the item, so it proves the restart read the same
        // files; the kill itself saves nothing.
        val beforeSave = scenario.server.mark()
        scenario.server.sendConsoleCommand("save-all flush")
        scenario.server.awaitLine(SAVED_LINE, from = beforeSave)

        scenario.kill()
        bot.awaitDisconnected()

        scenario.start()
        scenario.connect("Survivor")

        assertEquals(world, probe.world())
        assertEquals(mapOf(3 to "DIRT:7"), probe.inventory("Survivor").slots)
    }

    private companion object {

        /**
         * The chest view's slot that shows the first hotbar slot: the chest's 27 slots come first,
         * then the 27 slots of the main inventory.
         */
        const val CHEST_HOTBAR_START = 54

        const val DROP_RADIUS = 4

        /**
         * What the server logs once `save-all flush` wrote every chunk and every player.
         */
        const val SAVED_LINE = "Saved the game"
    }
}
