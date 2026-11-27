package dev.slne.surf.api.paper.scenario.test

import dev.slne.surf.api.paper.scenario.probe.ProbeProtocol
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Kills bots during a temporary inventory session and checks that the death ends the session: no
 * temporary item and no copy of an original item lies in the world, the bot respawns with the
 * original inventory, and the end event fires once with the cause `DEATH`.
 *
 * The world keeps the default game rules, so a death without a session drops the inventory.
 * Original and temporary items use different materials, so every dropped item tells which side it
 * came from. Every test joins bots under names of its own.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TemporaryInventoryDeathScenarioTest {

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
    fun `a death ends the session and keeps the original inventory`() {
        val bot = scenario.connect("Faller")
        val original = giveOriginalInventory("Faller", bot)
        val begin = sessions.begin("Faller", SESSION_KEY, "1:DIAMOND_PICKAXE:1,40:SHIELD:1")
        assertTrue(begin.startsWith("started "), begin)

        val death = killDuringSession("Faller", bot)

        assertEquals(
            TemporaryInventoryProbe.EndEvent(
                cause = "DEATH",
                sessionId = begin.sessionId(),
                state = SESSION_KEY,
                discarded = "DIAMOND_PICKAXE:1,SHIELD:1",
            ),
            death.event,
        )
        assertRespawnsWithOriginalInventory("Faller", bot, original, death)
        assertEquals(1, sessions.endEventCount(begin.sessionId()))
    }

    @Test
    fun `a death discards temporary items in an open crafting table and on the cursor`() {
        val bot = scenario.connect("Tinkerer")
        val original = giveOriginalInventory("Tinkerer", bot)
        val begin = sessions.begin(
            "Tinkerer",
            SESSION_KEY,
            "0:GRAVEL:4,1:STICK:2,2:DIAMOND_PICKAXE:1",
        )
        assertTrue(begin.startsWith("started "), begin)

        openCraftingTable("Tinkerer", bot)
        moveIntoCraftingInput("Tinkerer", bot)
        takeOntoCursor("Tinkerer", bot)

        val death = killDuringSession("Tinkerer", bot)

        assertEquals("DEATH", death.event.cause)
        assertEquals("DIAMOND_PICKAXE:1,GRAVEL:4,STICK:2", death.event.discarded)
        assertRespawnsWithOriginalInventory("Tinkerer", bot, original, death)
        assertEquals(1, sessions.endEventCount(begin.sessionId()))
    }

    /**
     * Gives the player items in the main inventory, an armor slot and the offhand, selects hotbar
     * slot 4, and returns the resulting inventory.
     */
    private fun giveOriginalInventory(player: String, bot: Bot): InventoryReport {
        probe.give(player, slot = 3, material = "STONE", amount = 7)
        probe.give(player, slot = 20, material = "OAK_LOG", amount = 5)
        probe.give(player, slot = HELMET_SLOT, material = "IRON_HELMET", amount = 1)
        probe.give(player, slot = OFFHAND_SLOT, material = "TORCH", amount = 9)
        bot.selectHotbarSlot(ORIGINAL_HOTBAR_SLOT)

        return probe.awaitInventory(player, "the original inventory") { report ->
            report.heldSlot == ORIGINAL_HOTBAR_SLOT && report.slots.size == ORIGINAL_ITEMS
        }
    }

    private fun openCraftingTable(player: String, bot: Bot) {
        val table = probe.place(player, "CRAFTING_TABLE")
        val beforeOpen = scenario.server.mark()

        bot.useBlock(table)

        assertEquals("WORKBENCH", probe.awaitEvent(ProbeProtocol.OPEN, player, beforeOpen))
        bot.awaitOpenContainer()
    }

    /**
     * Moves the gravel from hotbar slot 0 into the first crafting input.
     */
    private fun moveIntoCraftingInput(player: String, bot: Bot) {
        bot.click(TABLE_HOTBAR_START)
        probe.awaitInventory(player, "the gravel on the cursor") { it.cursor == "GRAVEL:4" }

        bot.click(TABLE_FIRST_INPUT)
        probe.awaitInventory(player, "an empty cursor") { it.cursor == "AIR" }
        assertEquals("GRAVEL:4", craftingInputs(player))
    }

    /**
     * Takes the sticks from hotbar slot 1 onto the cursor.
     */
    private fun takeOntoCursor(player: String, bot: Bot) {
        bot.click(TABLE_HOTBAR_START + 1)

        val report = probe.awaitInventory(player, "the sticks on the cursor") { report ->
            report.cursor == "STICK:2"
        }
        assertEquals("WORKBENCH", report.view)
        assertEquals(mapOf(2 to "DIAMOND_PICKAXE:1"), report.slots)
    }

    /**
     * Kills the player, checks that nothing dropped, and returns where the player died and the end
     * event of the session.
     */
    private fun killDuringSession(player: String, bot: Bot): Death {
        val position = probe.position(player)
        val beforeDeath = scenario.server.mark()

        probe.kill(player)
        probe.awaitEvent(ProbeProtocol.DEATH, player, beforeDeath)
        bot.awaitDeath()

        assertEquals(emptyList<String>(), droppedItems(position))

        return Death(position, sessions.awaitEnd(player, beforeDeath))
    }

    private fun assertRespawnsWithOriginalInventory(
        player: String,
        bot: Bot,
        original: InventoryReport,
        death: Death,
    ) {
        val beforeRespawn = scenario.server.mark()

        bot.respawn()
        probe.awaitEvent(ProbeProtocol.RESPAWN, player, beforeRespawn)

        assertEquals(original, probe.inventory(player))
        assertEquals(NO_SESSION, sessions.session(player))
        assertEquals(emptyList<String>(), droppedItems(death.position))
    }

    private fun droppedItems(position: BlockPosition): List<String> {
        return probe.items(scenario.worldName, position, radius = DROP_RADIUS)
    }

    private fun craftingInputs(player: String): String = probe.request("crafting-inputs", player)

    private fun String.sessionId(): String = substringAfter("started ")

    private class Death(val position: BlockPosition, val event: TemporaryInventoryProbe.EndEvent)

    private companion object {

        const val SESSION_KEY = "probe:death"

        const val HELMET_SLOT = 39

        const val OFFHAND_SLOT = 40

        const val ORIGINAL_HOTBAR_SLOT = 4

        const val ORIGINAL_ITEMS = 4

        /**
         * The crafting table view's slot that shows the first hotbar slot: the result slot and the
         * nine inputs come first, then the 27 slots of the main inventory.
         */
        const val TABLE_HOTBAR_START = 37

        const val TABLE_FIRST_INPUT = 1

        const val NO_SESSION = "none"

        const val DROP_RADIUS = 8
    }
}
