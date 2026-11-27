package dev.slne.surf.api.paper.scenario.test

import dev.slne.surf.api.paper.scenario.probe.ProbeProtocol
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Begins and ends temporary inventory sessions for bots and checks what the server sees: the
 * temporary inventory while the session runs, the original inventory afterwards, the end event,
 * and that no item lies in the world.
 *
 * Every test joins bots under names of its own, so the tests do not depend on each other or on
 * their order.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TemporaryInventorySessionScenarioTest {

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
    fun `a menu click begins a session after the click took effect`() {
        val bot = scenario.connect("MenuUser")
        probe.give("MenuUser", slot = 3, material = "DIRT", amount = 5)
        val beforeClick = scenario.server.mark()

        sessions.openMenu("MenuUser", "probe:menu", "1:DIAMOND_PICKAXE:1")
        bot.awaitOpenContainer()
        bot.click(MENU_BUTTON)

        val begin = sessions.awaitMenuBegin("MenuUser", beforeClick)
        assertTrue(begin.startsWith("started "), begin)

        val temporary = probe.awaitInventory("MenuUser", "the temporary inventory") { report ->
            report.slots == mapOf(1 to "DIAMOND_PICKAXE:1")
        }
        assertEquals("AIR", temporary.cursor)
        assertEquals("CRAFTING", temporary.view)

        assertTrue(sessions.end("MenuUser"))

        val original = probe.inventory("MenuUser")
        assertEquals("DIRT:5", original.slots[3])
        assertEquals(listOf("DIRT:5", "EMERALD:1"), original.slots.values.sorted())
        assertNoItemsAround("MenuUser")
    }

    @Test
    fun `a begin whose cursor item does not fit refuses and drops nothing`() {
        val bot = scenario.connect("Packrat")
        sessions.fillSlots("Packrat", "STONE")
        val full = probe.awaitInventory("Packrat", "every slot filled") { report ->
            report.slots.size == PLAYER_SLOTS
        }
        val beforeClick = scenario.server.mark()

        sessions.openMenu("Packrat", "probe:full", "1:DIAMOND_PICKAXE:1")
        bot.awaitOpenContainer()
        bot.click(MENU_BUTTON)

        assertEquals("unavailable", sessions.awaitMenuBegin("Packrat", beforeClick))

        val refused = probe.inventory("Packrat")
        assertEquals(full.slots, refused.slots)
        assertEquals("EMERALD:1", refused.cursor)
        assertEquals(NO_SESSION, sessions.session("Packrat"))
        assertNoItemsAround("Packrat")

        probe.give("Packrat", slot = 0, material = "AIR", amount = 0)
        bot.closeContainer()
        probe.awaitInventory("Packrat", "the emerald put away") { it.slots[0] == "EMERALD:1" }
    }

    @Test
    fun `ending on the player's thread restores the original inventory`() {
        assertEndRestoresOriginalInventory("SyncEnder", async = false)
    }

    @Test
    fun `ending through endAsync restores the original inventory`() {
        assertEndRestoresOriginalInventory("AsyncEnder", async = true)
    }

    @Test
    fun `a second begin reports the active session and changes nothing`() {
        scenario.connect("Doubler")

        val first = sessions.begin("Doubler", "probe:first", "0:STICK:1")
        assertTrue(first.startsWith("started "), first)

        val second = sessions.begin("Doubler", "probe:second", "1:BONE:1")

        assertEquals("already-active probe:first", second)
        assertEquals(mapOf(0 to "STICK:1"), probe.inventory("Doubler").slots)
        assertEquals("probe:first ${first.sessionId()}", sessions.session("Doubler"))

        assertTrue(sessions.end("Doubler"))
        assertNoItemsAround("Doubler")
    }

    @Test
    fun `a fill that throws puts the original inventory back`() {
        scenario.connect("Fumbler")
        probe.give("Fumbler", slot = 3, material = "DIRT", amount = 5)

        assertEquals(
            "threw java.lang.IllegalStateException",
            sessions.beginFailing("Fumbler", "probe:fumble", "0:STICK:1"),
        )
        assertEquals(mapOf(3 to "DIRT:5"), probe.inventory("Fumbler").slots)
        assertEquals(NO_SESSION, sessions.session("Fumbler"))

        val begin = sessions.begin("Fumbler", "probe:fumble", "0:STICK:1")
        assertTrue(begin.startsWith("started "), begin)
        assertTrue(sessions.end("Fumbler"))
        assertEquals(mapOf(3 to "DIRT:5"), probe.inventory("Fumbler").slots)
        assertNoItemsAround("Fumbler")
    }

    @Test
    fun `a begin whose caller is cancelled after it ran ends the session again`() {
        scenario.connect("Canceller")
        probe.give("Canceller", slot = 3, material = "DIRT", amount = 5)
        val beforeBegin = scenario.server.mark()

        assertEquals("cancelled", sessions.beginCancelled("Canceller", "probe:cancel", "0:STICK:1"))

        val event = sessions.awaitEnd("Canceller", beforeBegin)
        assertEquals("ENDED", event.cause)
        assertEquals("STICK:1", event.discarded)
        assertEquals(mapOf(3 to "DIRT:5"), probe.inventory("Canceller").slots)
        assertEquals(NO_SESSION, sessions.session("Canceller"))
        assertNoItemsAround("Canceller")
    }

    @Test
    fun `an end that a crafting callback repeats fires one end event`() {
        scenario.connect("Crafter")
        val begin = sessions.begin("Crafter", "probe:craft", "0:STICK:1")
        assertTrue(begin.startsWith("started "), begin)

        probe.request("set-crafting-input", "Crafter", 1, "BONE", 1)
        sessions.endOnCraft("Crafter")
        val beforeEnd = scenario.server.mark()

        assertTrue(sessions.end("Crafter"))

        assertEquals("false", sessions.awaitEndOnCraft("Crafter", beforeEnd))
        assertEquals("BONE:1,STICK:1", sessions.awaitEnd("Crafter", beforeEnd).discarded)
        assertEquals(1, sessions.endEventCount(begin.sessionId()))
        assertEquals(emptyMap<Int, String>(), probe.inventory("Crafter").slots)
        assertNoItemsAround("Crafter")
    }

    @Test
    fun `an end in creative mode discards temporary items in the own crafting grid`() {
        scenario.connect("Creator")
        val begin = sessions.begin("Creator", "probe:creative", "0:STICK:1")
        assertTrue(begin.startsWith("started "), begin)

        probe.request("set-crafting-input", "Creator", 0, "BONE", 1)
        probe.request("game-mode", "Creator", "CREATIVE")
        val beforeEnd = scenario.server.mark()

        assertTrue(sessions.end("Creator"))

        assertEquals("BONE:1,STICK:1", sessions.awaitEnd("Creator", beforeEnd).discarded)
        assertEquals("none", probe.request("crafting-inputs", "Creator"))
        assertEquals(emptyMap<Int, String>(), probe.inventory("Creator").slots)
        assertNoItemsAround("Creator")
    }

    @Test
    fun `an end discards temporary items in an open anvil`() {
        scenario.connect("Smith")
        probe.give("Smith", slot = 3, material = "DIRT", amount = 5)
        val begin = sessions.begin("Smith", "probe:anvil", "0:STICK:1")
        assertTrue(begin.startsWith("started "), begin)

        probe.request("open-anvil", "Smith", "BONE", 1)
        val beforeEnd = scenario.server.mark()

        assertTrue(sessions.end("Smith"))
        assertEquals("BONE:1,STICK:1", sessions.awaitEnd("Smith", beforeEnd).discarded)

        probe.request("close-view", "Smith")
        assertEquals(mapOf(3 to "DIRT:5"), probe.inventory("Smith").slots)
        assertNoItemsAround("Smith")
    }

    @Test
    fun `a session whose stored entry another plugin removed is gone after a rejoin`() {
        val bot = scenario.connect("Wiped")
        val begin = sessions.begin("Wiped", "probe:wiped", "0:STICK:1")
        assertTrue(begin.startsWith("started "), begin)

        probe.request("clear-data", "Wiped")
        val beforeQuit = scenario.server.mark()
        bot.disconnect()
        probe.awaitEvent(ProbeProtocol.QUIT, "Wiped", beforeQuit)
        scenario.connect("Wiped")

        assertEquals(NO_SESSION, sessions.session("Wiped"))
    }

    @Test
    fun `a begin for a dead player is unavailable`() {
        val bot = scenario.connect("Ghost")
        val beforeDeath = scenario.server.mark()

        probe.kill("Ghost")
        probe.awaitEvent(ProbeProtocol.DEATH, "Ghost", beforeDeath)
        bot.awaitDeath()

        assertEquals("unavailable", sessions.begin("Ghost", "probe:dead", "0:STICK:1"))
        assertEquals(NO_SESSION, sessions.session("Ghost"))

        bot.respawn()
    }

    @Test
    fun `a begin for a player who left is unavailable`() {
        val bot = scenario.connect("Leaver")
        val beforeQuit = scenario.server.mark()

        bot.disconnect()
        probe.awaitEvent(ProbeProtocol.QUIT, "Leaver", beforeQuit)

        assertEquals("unavailable", sessions.begin("Leaver", "probe:gone", "0:STICK:1"))
    }

    @Test
    fun `an item that cannot be stored refuses the begin and changes nothing`() {
        scenario.connect("Hoarder")
        probe.give("Hoarder", slot = 0, material = "DIRT", amount = 5)
        // Item stacks serialize with a count of at most 99.
        probe.give("Hoarder", slot = 5, material = "STONE", amount = 127)

        assertEquals("unstorable 5", sessions.begin("Hoarder", "probe:hoard", "0:STICK:1"))
        assertEquals(mapOf(0 to "DIRT:5", 5 to "STONE:127"), probe.inventory("Hoarder").slots)
        assertEquals(NO_SESSION, sessions.session("Hoarder"))

        probe.give("Hoarder", slot = 5, material = "AIR", amount = 0)
    }

    @Test
    fun `ending off the player's thread throws`() {
        scenario.connect("Stranger")
        val begin = sessions.begin("Stranger", "probe:stranger", "0:STICK:1")
        assertTrue(begin.startsWith("started "), begin)

        assertEquals("threw java.lang.IllegalStateException", sessions.endOffThread("Stranger"))
        assertEquals("probe:stranger ${begin.sessionId()}", sessions.session("Stranger"))

        assertTrue(sessions.end("Stranger"))
    }

    private fun assertEndRestoresOriginalInventory(name: String, async: Boolean) {
        val bot = scenario.connect(name)
        sessions.fillSlots(name, "STONE")
        bot.selectHotbarSlot(4)
        val original = probe.awaitInventory(name, "every slot filled, hotbar slot 4 selected") {
            it.heldSlot == 4 && it.slots.size == PLAYER_SLOTS
        }

        val begin = sessions.begin(name, "probe:end", "1:DIAMOND_PICKAXE:1,40:SHIELD:1")
        assertTrue(begin.startsWith("started "), begin)

        val temporary = probe.inventory(name)
        assertEquals(mapOf(1 to "DIAMOND_PICKAXE:1", 40 to "SHIELD:1"), temporary.slots)
        assertEquals(0, temporary.heldSlot)
        assertEquals("probe:end ${begin.sessionId()}", sessions.session(name))

        val beforeEnd = scenario.server.mark()
        assertTrue(sessions.end(name, async))

        val restored = probe.inventory(name)
        assertEquals(original.slots, restored.slots)
        assertEquals(4, restored.heldSlot)
        assertEquals(NO_SESSION, sessions.session(name))

        val event = sessions.awaitEnd(name, beforeEnd)
        assertEquals(
            TemporaryInventoryProbe.EndEvent(
                cause = "ENDED",
                sessionId = begin.sessionId(),
                state = "probe:end",
                discarded = "DIAMOND_PICKAXE:1,SHIELD:1",
            ),
            event,
        )

        assertFalse(sessions.end(name, async))
        assertEquals("returned false", sessions.endOffThread(name))
        assertEquals(1, sessions.endEventCount(begin.sessionId()))
        assertNoItemsAround(name)
    }

    private fun assertNoItemsAround(player: String) {
        assertEquals(
            emptyList<String>(),
            probe.items(scenario.worldName, probe.position(player), radius = DROP_RADIUS),
        )
    }

    private fun String.sessionId(): String = substringAfter("started ")

    private companion object {

        /**
         * The menu slot that begins a session.
         */
        const val MENU_BUTTON = 0

        /**
         * Hotbar, main inventory, armor, offhand, body armor and saddle.
         */
        const val PLAYER_SLOTS = 43

        const val NO_SESSION = "none"

        const val DROP_RADIUS = 8
    }
}
