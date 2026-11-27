package dev.slne.surf.api.paper.scenario.test

import dev.slne.surf.api.paper.scenario.probe.ProbeProtocol
import dev.slne.surf.api.paper.server.harness.PaperServer
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * The test's side of the probe plugin: sends requests to the server console and reads the replies
 * and events from its log.
 *
 * Each request carries an id of its own, so a reply is never mistaken for the reply to an earlier
 * request, also across restarts of the server.
 */
class Probe(private val server: PaperServer) {

    private val requestIds = AtomicInteger()

    /**
     * Runs the probe action [action] with [arguments] and returns its reply, or fails with the
     * probe's error.
     */
    fun request(action: String, vararg arguments: Any): String {
        val requestId = requestIds.incrementAndGet()
        val from = server.mark()
        val reply = "${ProbeProtocol.REPLY} $requestId "
        val error = "${ProbeProtocol.ERROR} $requestId "

        server.sendConsoleCommand(
            listOf(ProbeProtocol.COMMAND, requestId, action, *arguments).joinToString(" "),
        )

        val description = "the probe's answer to request $requestId ($action)"
        val line = server.awaitLine(description, from = from) { logged ->
            reply in logged || error in logged
        }

        if (error in line) {
            server.fail("The probe could not answer '$action': ${line.substringAfter(error)}")
        }

        return line.substringAfter(reply).trim()
    }

    /**
     * The player's inventory as the server sees it.
     */
    fun inventory(player: String): InventoryReport {
        return InventoryReport.parse(request("inventory", player))
    }

    /**
     * Polls the player's inventory until it matches [expectation], and returns the matching report.
     *
     * Bot actions reach the server asynchronously, so a report right after an action may still
     * show the state before it.
     */
    fun awaitInventory(
        player: String,
        description: String,
        expectation: (InventoryReport) -> Boolean,
    ): InventoryReport {
        val deadline = System.nanoTime() + AWAIT_TIMEOUT.toNanos()
        var report = inventory(player)

        while (!expectation(report)) {
            if (System.nanoTime() > deadline) {
                server.fail("$player's inventory never showed $description. Last report: $report")
            }

            Thread.sleep(POLL.toMillis())
            report = inventory(player)
        }

        return report
    }

    /**
     * Puts [amount] of [material] into [slot] of the player's inventory.
     */
    fun give(player: String, slot: Int, material: String, amount: Int) {
        request("give", player, slot, material, amount)
    }

    /**
     * The block position of the player's feet.
     */
    fun position(player: String): BlockPosition = blockPosition(request("position", player))

    /**
     * Sets a block of [material] within the player's reach and returns its position.
     */
    fun place(player: String, material: String): BlockPosition {
        return blockPosition(request("place", player, material))
    }

    /**
     * Kills the player.
     */
    fun kill(player: String) {
        request("kill", player)
    }

    /**
     * The name and unique id of the server's first world, which identify it across restarts.
     */
    fun world(): String = request("world")

    /**
     * The dropped items within [radius] blocks of the position, as `MATERIAL:amount`, sorted.
     */
    fun items(world: String, position: BlockPosition, radius: Int): List<String> {
        val reply = request("items", world, position.x, position.y, position.z, radius)

        return if (reply == NONE) emptyList() else reply.split(',')
    }

    /**
     * Waits for the probe's event line of [kind] for [player], logged at or after line [from] of
     * the current server run.
     *
     * @return the event's detail, such as the type of an opened inventory
     */
    fun awaitEvent(kind: String, player: String, from: Int = 0): String {
        val line = server.awaitLine("the probe event '$kind' for $player", from = from) { logged ->
            eventWords(logged)?.take(2) == listOf(kind, player)
        }

        return checkNotNull(eventWords(line)).drop(2).joinToString(" ")
    }

    private fun blockPosition(reply: String): BlockPosition {
        val (x, y, z) = reply.split(' ').map(String::toInt)

        return BlockPosition(x, y, z)
    }

    private fun eventWords(line: String): List<String>? {
        if (ProbeProtocol.EVENT !in line) return null

        return line.substringAfter(ProbeProtocol.EVENT)
            .trim()
            .split(' ')
    }

    private companion object {

        const val NONE = "none"

        val AWAIT_TIMEOUT: Duration = Duration.ofSeconds(10)

        val POLL: Duration = Duration.ofMillis(100)
    }
}

/**
 * A player's inventory as the probe reported it.
 *
 * @property slots the non-empty slots, by slot index, as `MATERIAL:amount`
 * @property cursor the item on the cursor as `MATERIAL:amount`, or `AIR`
 * @property view the Bukkit type of the open inventory, `CRAFTING` when no container is open
 */
data class InventoryReport(
    val heldSlot: Int,
    val cursor: String,
    val view: String,
    val slots: Map<Int, String>,
) {

    companion object {

        /**
         * Reads the reply of the probe's `inventory` action.
         */
        fun parse(reply: String): InventoryReport {
            val fields = reply.split(' ').associate { field ->
                field.substringBefore('=') to field.substringAfter('=')
            }
            val slots = fields.getValue("slots")
                .split(',')
                .filter { it != "none" }
                .associate { slot -> slot.substringBefore(':').toInt() to slot.substringAfter(':') }

            return InventoryReport(
                heldSlot = fields.getValue("held").toInt(),
                cursor = fields.getValue("cursor"),
                view = fields.getValue("view"),
                slots = slots,
            )
        }
    }
}
