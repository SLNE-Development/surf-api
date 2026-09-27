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

import dev.slne.surf.api.core.util.getValue
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.sidebar.SidebarLine
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectList
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

/**
 * Packet-based sidebar scoreboard shown to a single player.
 */
abstract class PlayerNmsScoreboard(player: Player, title: Component) {

    val playerUuid: UUID = player.uniqueId
    val player: Player? by WeakReference(player)

    val id = "sb-" + NEXT_ID.getAndIncrement()

    private val lock = Any()

    @Volatile
    var title: Component = title
        private set

    @Volatile
    var lines: ObjectList<SidebarLine> = ObjectImmutableList.of()
        private set

    @Volatile
    var deleted = false
        private set

    init {
        send(createObjective(title))
    }

    fun updateTitle(title: Component) {
        synchronized(lock) {
            checkNotDeleted()
            if (this.title == title) return
            this.title = title

            send(updateObjective(title))
        }
    }

    fun updateLines(vararg lines: Component) {
        applyLines(ObjectImmutableList(Array(lines.size) { SidebarLine(lines[it]) }))
    }

    /**
     * Replaces all lines. Only lines that differ from the current state are resent.
     *
     * @throws IllegalArgumentException if there are more than [MAX_LINES] lines
     */
    fun updateLines(lines: List<SidebarLine>) {
        applyLines(ObjectImmutableList(lines))
    }

    private fun applyLines(newLines: ObjectImmutableList<SidebarLine>) {
        require(newLines.size <= MAX_LINES) { "Too many lines: ${newLines.size} (max $MAX_LINES)" }

        synchronized(lock) {
            checkNotDeleted()
            replaceLines(newLines)
        }
    }

    /**
     * Sets [line] to [text] and [score]. A [line] beyond the current size appends it and fills the
     * gap with empty lines.
     */
    fun updateLine(line: Int, text: Component, score: Component? = null) {
        require(line in 0 until MAX_LINES) { "Line must be in 0 until $MAX_LINES: $line" }

        synchronized(lock) {
            checkNotDeleted()
            val current = lines
            val newLines = Array(max(current.size, line + 1)) { index ->
                when {
                    index == line -> SidebarLine(text, score)
                    index < current.size -> current[index]
                    else -> EMPTY_LINE
                }
            }
            replaceLines(ObjectImmutableList(newLines))
        }
    }

    /**
     * Removes [line] and shifts the lines below it up. Does nothing if [line] does not exist.
     */
    fun removeLine(line: Int) {
        synchronized(lock) {
            checkNotDeleted()
            val current = lines
            if (line !in current.indices) return

            val newLines =
                Array(current.size - 1) { index -> current[if (index < line) index else index + 1] }
            replaceLines(ObjectImmutableList(newLines))
        }
    }

    /**
     * Removes the sidebar from the client. Subsequent calls do nothing.
     */
    fun delete() {
        synchronized(lock) {
            if (deleted) return

            deleted = true
            send(removeObjective())
        }
    }

    protected abstract fun createObjective(title: Component): PacketOperation
    protected abstract fun updateObjective(title: Component): PacketOperation
    protected abstract fun removeObjective(): PacketOperation
    protected abstract fun setScore(owner: String, score: Int, line: SidebarLine): PacketOperation
    protected abstract fun resetScore(owner: String): PacketOperation

    private fun replaceLines(newLines: ObjectImmutableList<SidebarLine>) {
        assert(Thread.holdsLock(lock)) { "Must be called under lock" }

        val oldLines = lines
        lines = newLines

        var operation: PacketOperation? = null
        for (index in 0 until max(oldLines.size, newLines.size)) {
            val line = newLines.getOrNull(index)
            val change = when {
                line == null -> resetScore(ENTRIES[index])
                line != oldLines.getOrNull(index) -> setScore(ENTRIES[index], MAX_LINES - index, line)
                else -> continue
            }
            operation = (operation ?: PacketOperation.start()).add(change)
        }

        if (operation != null) send(operation)
    }

    private fun send(operation: PacketOperation) {
        if (operation.isEmpty()) return

        val viewer = player ?: return
        if (viewer.isConnected) {
            operation.execute(viewer)
        }
    }

    private fun checkNotDeleted() {
        check(!deleted) { "This scoreboard is deleted" }
    }

    companion object {
        /**
         * Maximum number of sidebar entries the client renders.
         */
        const val MAX_LINES = 15

        private val NEXT_ID = AtomicInteger()
        private val EMPTY_LINE = SidebarLine(Component.empty())

        private val ENTRIES = List(MAX_LINES) { "§" + it.toString(16) }
    }
}
