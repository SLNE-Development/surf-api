package dev.slne.surf.api.paper.scenario.test

import dev.slne.surf.api.paper.scenario.probe.ProbeProtocol
import dev.slne.surf.api.paper.scenario.probe.TemporaryInventoryEndLog
import dev.slne.surf.api.paper.scenario.probe.TemporaryInventoryEndOnCraftProbe
import dev.slne.surf.api.paper.scenario.probe.TemporaryInventoryMenuProbe
import dev.slne.surf.api.paper.server.harness.PaperServer

/**
 * The test's side of the probe's temporary inventory actions.
 *
 * Items are written as `slot:MATERIAL:amount,...`, or `none` for no items.
 */
class TemporaryInventoryProbe(private val server: PaperServer, private val probe: Probe) {

    /**
     * Begins a session whose fill puts [items] into the emptied inventory and selects the first
     * hotbar slot, and returns the probe's reply: `started <session id>`, `already-active <key>`,
     * `unstorable <slot>` or `unavailable`.
     */
    fun begin(player: String, key: String, items: String = NONE): String {
        return probe.request("temp-begin", player, key, items)
    }

    /**
     * Begins a session as [begin] does, with a fill that throws after it filled the inventory, and
     * returns `threw <exception class>`.
     */
    fun beginFailing(player: String, key: String, items: String): String {
        return probe.request("temp-begin-failing", player, key, items)
    }

    /**
     * Begins a session as [begin] does, from a coroutine that the fill cancels, and returns
     * `cancelled` or the begin's result.
     */
    fun beginCancelled(player: String, key: String, items: String): String {
        return probe.request("temp-begin-cancelled", player, key, items)
    }

    /**
     * Makes the player's next crafting update end the player's session.
     */
    fun endOnCraft(player: String) {
        probe.request(TemporaryInventoryEndOnCraftProbe.END_ON_CRAFT, player)
    }

    /**
     * Waits for the result of the end that a crafting update started, logged at or after line
     * [from].
     */
    fun awaitEndOnCraft(player: String, from: Int): String {
        return probe.awaitEvent(TemporaryInventoryEndOnCraftProbe.END_ON_CRAFT, player, from)
    }

    /**
     * Ends the session the probe began last for [player], with `end()` on the player's thread or,
     * with [async], through `endAsync()`, and returns whether it ended.
     */
    fun end(player: String, async: Boolean = false): Boolean {
        return probe.request("temp-end", player, if (async) "async" else "sync").toBooleanStrict()
    }

    /**
     * Calls `end()` off the player's thread and returns `threw <exception class>` or
     * `returned <result>`.
     */
    fun endOffThread(player: String): String = probe.request("temp-end-off-thread", player)

    /**
     * The player's active session as `<key> <session id>`, or `none`, read off the server threads.
     */
    fun session(player: String): String = probe.request("temp-session", player)

    /**
     * Opens a menu whose first slot begins a session with [key] and [items] when clicked.
     */
    fun openMenu(player: String, key: String, items: String) {
        probe.request("temp-menu", player, key, items)
    }

    /**
     * Puts `slot + 1` items of [material] into every slot of the player's inventory.
     */
    fun fillSlots(player: String, material: String) {
        probe.request("fill-slots", player, material)
    }

    /**
     * Waits for the result of a begin that a menu click started, logged at or after line [from].
     */
    fun awaitMenuBegin(player: String, from: Int): String {
        return probe.awaitEvent(TemporaryInventoryMenuProbe.BEGIN_EVENT, player, from)
    }

    /**
     * Waits for the end event of a session of [player], logged at or after line [from].
     */
    fun awaitEnd(player: String, from: Int): EndEvent {
        val (cause, sessionId, state, discarded) = probe
            .awaitEvent(TemporaryInventoryEndLog.END_EVENT, player, from)
            .split(' ')

        return EndEvent(cause = cause, sessionId = sessionId, state = state, discarded = discarded)
    }

    /**
     * How many end events the server logged for [sessionId] in the current run.
     */
    fun endEventCount(sessionId: String): Int {
        val marker = "${ProbeProtocol.EVENT} ${TemporaryInventoryEndLog.END_EVENT} "

        return server.lines().count { line -> marker in line && sessionId in line }
    }

    /**
     * A logged `TemporaryInventoryEndEvent`.
     *
     * @property state the session key the probe's fill stored, or `none`
     * @property discarded the discarded items as sorted `MATERIAL:amount,...`, or `none`
     */
    data class EndEvent(
        val cause: String,
        val sessionId: String,
        val state: String,
        val discarded: String,
    )

    companion object {
        const val NONE = "none"
    }
}
