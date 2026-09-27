/*
 * This file contains code derived from FastBoard, licensed under the MIT License.
 *
 * Copyright (c) 2019-2026 MrMicky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.slne.surf.api.paper.nms.common.scoreboard

import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.sidebar.SidebarLine
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

/**
 * Packet-based sidebar objective of a single player.
 *
 * Not thread-safe: the owner serializes all calls. Nothing is sent before the first [update],
 * which creates and displays the objective; after [delete], further calls do nothing.
 */
abstract class PlayerNmsScoreboard(protected val player: Player) {

    val id = "sb-" + NEXT_ID.getAndIncrement()

    private var shown = false
    private var deleted = false
    private var title: Component = Component.empty()
    private var lines: List<SidebarLine> = emptyList()

    /**
     * Shows [title] and [lines], sending only what changed since the previous call as one bundle.
     *
     * [lines] is retained and must neither be modified afterwards nor exceed [MAX_LINES] entries.
     */
    fun update(title: Component, lines: List<SidebarLine>) {
        if (deleted) return

        val operation = PacketOperation.start()
        if (!shown) {
            shown = true
            operation.add(createObjective(title))
        } else if (this.title != title) {
            operation.add(updateObjective(title))
        }
        this.title = title

        val oldLines = this.lines
        this.lines = lines
        for (index in 0 until max(oldLines.size, lines.size)) {
            val line = lines.getOrNull(index)
            if (line == null) {
                operation.add(resetScore(ENTRIES[index]))
            } else if (line != oldLines.getOrNull(index)) {
                operation.add(setScore(ENTRIES[index], MAX_LINES - index, line))
            }
        }

        send(operation)
    }

    /** Removes the objective from the client if it was shown. */
    fun delete() {
        if (deleted) return

        deleted = true
        if (shown) send(removeObjective())
    }

    protected abstract fun createObjective(title: Component): PacketOperation
    protected abstract fun updateObjective(title: Component): PacketOperation
    protected abstract fun removeObjective(): PacketOperation
    protected abstract fun setScore(owner: String, score: Int, line: SidebarLine): PacketOperation
    protected abstract fun resetScore(owner: String): PacketOperation

    private fun send(operation: PacketOperation) {
        if (!operation.isEmpty() && player.isConnected) {
            operation.execute(player)
        }
    }

    companion object {
        /**
         * Maximum number of sidebar entries the client renders.
         */
        const val MAX_LINES = 15

        private val NEXT_ID = AtomicInteger()
        private val ENTRIES = List(MAX_LINES) { "§" + it.toString(16) }
    }
}
