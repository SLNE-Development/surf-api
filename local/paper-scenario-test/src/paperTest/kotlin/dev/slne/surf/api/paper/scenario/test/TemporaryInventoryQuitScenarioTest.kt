package dev.slne.surf.api.paper.scenario.test

import dev.slne.surf.api.paper.scenario.probe.ProbeProtocol
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Lets bots quit during a temporary inventory session with temporary items outside the inventory
 * and checks what they find after a rejoin: the original inventory, no session, no temporary item
 * in the world, and one end event with the cause `QUIT`.
 *
 * Every test joins a bot under a name of its own, so the tests do not depend on each other or on
 * their order.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TemporaryInventoryQuitScenarioTest {

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
    fun `quitting with a temporary item on the cursor and a chest open restores the original`() {
        val quit = beginBeforeQuit("ChestQuitter")
        val chest = probe.place(quit.name, "CHEST")

        quit.bot.useBlock(chest)
        quit.bot.awaitOpenContainer()
        quit.bot.click(CHEST_HOTBAR_START)
        probe.awaitInventory(quit.name, "the sticks on the cursor with the chest open") { report ->
            report.view == "CHEST" && report.cursor == "STICK:4"
        }

        assertQuitRestoresOriginalInventory(quit)
    }

    @Test
    fun `quitting with temporary items in a crafting table restores the original`() {
        val quit = beginBeforeQuit("TableQuitter")
        val table = probe.place(quit.name, "CRAFTING_TABLE")

        quit.bot.useBlock(table)
        quit.bot.awaitOpenContainer()
        moveIntoCraftingInput(quit.name, quit.bot, TABLE_HOTBAR_START, TABLE_FIRST_INPUT)
        moveIntoCraftingInput(quit.name, quit.bot, TABLE_HOTBAR_START + 1, TABLE_FIRST_INPUT + 1)
        assertEquals("WORKBENCH", probe.inventory(quit.name).view)

        assertQuitRestoresOriginalInventory(quit)
    }

    @Test
    fun `quitting with temporary items in the own crafting grid restores the original`() {
        val quit = beginBeforeQuit("GridQuitter")

        moveIntoCraftingInput(quit.name, quit.bot, GRID_HOTBAR_START, GRID_FIRST_INPUT)
        moveIntoCraftingInput(quit.name, quit.bot, GRID_HOTBAR_START + 1, GRID_FIRST_INPUT + 1)
        assertEquals("CRAFTING", probe.inventory(quit.name).view)

        assertQuitRestoresOriginalInventory(quit)
    }

    @Test
    fun `a kick with temporary items in a crafting table and on the cursor restores the original`() {
        val quit = beginBeforeQuit("TableKicked")
        val table = probe.place(quit.name, "CRAFTING_TABLE")

        quit.bot.useBlock(table)
        quit.bot.awaitOpenContainer()
        moveIntoCraftingInput(quit.name, quit.bot, TABLE_HOTBAR_START, TABLE_FIRST_INPUT)
        quit.bot.click(TABLE_HOTBAR_START + 1)
        probe.awaitInventory(quit.name, "the bones on the cursor with the table open") { report ->
            report.view == "WORKBENCH" && report.cursor == "BONE:2"
        }

        assertQuitRestoresOriginalInventory(quit, leave = ::kick)
    }

    /**
     * Joins [name] with an original inventory and begins a session that hands out
     * [TEMPORARY_ITEMS] in the first three hotbar slots.
     */
    private fun beginBeforeQuit(name: String): Quit {
        val bot = scenario.connect(name)
        probe.give(name, slot = 3, material = "DIRT", amount = 5)
        probe.give(name, slot = 20, material = "EMERALD", amount = 2)
        probe.give(name, slot = 40, material = "SHIELD", amount = 1)
        val original = probe.inventory(name)

        val begin = sessions.begin(name, "probe:quit", TEMPORARY_ITEMS)
        assertTrue(begin.startsWith("started "), begin)

        return Quit(name, bot, original, begin.substringAfter("started "))
    }

    /**
     * Picks the stack in [fromSlot] of the open view up and puts it down in [inputSlot], waiting
     * until the server applied each click.
     */
    private fun moveIntoCraftingInput(name: String, bot: Bot, fromSlot: Int, inputSlot: Int) {
        bot.click(fromSlot)
        val picked = probe.awaitInventory(name, "a stack on the cursor") { it.cursor != "AIR" }

        bot.click(inputSlot)
        probe.awaitInventory(name, "the cursor's ${picked.cursor} in crafting input $inputSlot") {
            it.cursor == "AIR"
        }
    }

    private fun kick(quit: Quit) {
        probe.request("kick", quit.name)
        quit.bot.awaitDisconnected()
    }

    /**
     * Lets the bot [leave], rejoins it and checks that it has the original inventory, that no
     * temporary item lies in the world, and that the session ended once with the cause `QUIT`.
     */
    private fun assertQuitRestoresOriginalInventory(
        quit: Quit,
        leave: (Quit) -> Unit = { it.bot.disconnect() },
    ) {
        val position = probe.position(quit.name)
        val beforeQuit = scenario.server.mark()

        leave(quit)
        probe.awaitEvent(ProbeProtocol.QUIT, quit.name, beforeQuit)
        scenario.connect(quit.name)

        assertEquals(
            emptyList<String>(),
            probe.items(scenario.worldName, position, radius = DROP_RADIUS),
        )

        val restored = probe.inventory(quit.name)
        assertEquals(quit.original.slots, restored.slots)
        assertEquals("AIR", restored.cursor)
        assertEquals(NO_SESSION, sessions.session(quit.name))

        assertEquals(
            TemporaryInventoryProbe.EndEvent(
                cause = "QUIT",
                sessionId = quit.sessionId,
                state = "probe:quit",
                discarded = DISCARDED_ITEMS,
            ),
            sessions.awaitEnd(quit.name, beforeQuit),
        )
        assertEquals(1, sessions.endEventCount(quit.sessionId))
    }

    /**
     * A bot in a session, about to quit.
     */
    private class Quit(
        val name: String,
        val bot: Bot,
        val original: InventoryReport,
        val sessionId: String,
    )

    private companion object {

        const val TEMPORARY_ITEMS = "0:STICK:4,1:BONE:2,2:OAK_PLANKS:3"

        /**
         * Every temporary item, as the end event reports it, wherever the player left it.
         */
        const val DISCARDED_ITEMS = "BONE:2,OAK_PLANKS:3,STICK:4"

        /**
         * The chest view's slot that shows the first hotbar slot: the chest's 27 slots come first,
         * then the 27 slots of the main inventory.
         */
        const val CHEST_HOTBAR_START = 54

        /**
         * The crafting table view: the result in slot 0, the inputs in 1 to 9, then the main
         * inventory and the hotbar.
         */
        const val TABLE_FIRST_INPUT = 1

        const val TABLE_HOTBAR_START = 37

        /**
         * The player's own inventory view: the result in slot 0, the inputs in 1 to 4, armor,
         * the main inventory, then the hotbar.
         */
        const val GRID_FIRST_INPUT = 1

        const val GRID_HOTBAR_START = 36

        const val NO_SESSION = "none"

        const val DROP_RADIUS = 8
    }
}
