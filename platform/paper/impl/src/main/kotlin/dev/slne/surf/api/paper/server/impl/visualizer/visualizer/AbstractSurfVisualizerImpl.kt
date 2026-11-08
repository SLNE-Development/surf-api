@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalVisualizerApi::class)

package dev.slne.surf.api.paper.server.impl.visualizer.visualizer

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.collection.TransformingSet2ObjectSet
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.paper.server.impl.visualizer.SurfBukkitVisualizerApiImpl
import dev.slne.surf.api.paper.server.plugin
import dev.slne.surf.api.paper.visualizer.visualizer.ExperimentalVisualizerApi
import dev.slne.surf.api.paper.visualizer.visualizer.SurfVisualizer
import it.unimi.dsi.fastutil.objects.ObjectSet
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.entity.Player
import java.lang.ref.Cleaner
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

abstract class AbstractSurfVisualizerImpl : SurfVisualizer {
    protected val log = logger()

    companion object {
        val cleaner: Cleaner = Cleaner.create()
    }

    protected val internalViewerUuids: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    override val viewerUuids: MutableSet<UUID> = Collections.unmodifiableSet(internalViewerUuids)

    @Deprecated("Use viewerUuids instead", replaceWith = ReplaceWith("viewerUuids"))
    override val viewers: ObjectSet<Player> =
        TransformingSet2ObjectSet(viewerUuids, Bukkit::getPlayer, Player::getUniqueId)

    override val uid: UUID = UUID.randomUUID()

    protected val visualizing = AtomicBoolean(false)
    protected val closed = AtomicBoolean(false)
    private val stateVersion = AtomicLong(0)

    abstract class CleanupState : Runnable {
        private val log = logger()

        override fun run() {
            try {
                cleanup()
            } catch (e: Throwable) {
                log.atWarning()
                    .withCause(e)
                    .log("Failed to clean up visualizer on GC")
            }
        }

        protected abstract fun cleanup()
    }

    override fun startVisualizing(): Boolean {
        ensureNotClosed()
        if (!visualizing.compareAndSet(false, true)) return false
        nextStateVersion()

        try {
            startVisualizingInternal()
            return true
        } catch (e: Throwable) {
            visualizing.set(false)
            log.atSevere()
                .withCause(e)
                .log("Failed to start visualizing")
            return false
        }
    }

    override fun stopVisualizing(): Boolean {
        ensureNotClosed()
        return stopVisualizing(false)
    }

    fun stopVisualizing(force: Boolean): Boolean {
        if (!visualizing.compareAndSet(true, false) && !force) return false
        if (!force) {
            ensureNotClosed()
        }

        nextStateVersion()

        try {
            stopVisualizingInternal()
            return true
        } catch (e: Throwable) {
            log.atSevere()
                .withCause(e)
                .log("Failed to stop visualizing")
            return false
        }
    }

    override fun isVisualizing() = visualizing.get()

    override fun addViewer(player: Player) {
        ensureNotClosed()
        if (player.isOnline && internalViewerUuids.add(player.uniqueId)) {
            SurfBukkitVisualizerApiImpl.INSTANCE.onViewerAdded(uid, player.uniqueId)
            onViewerAdded(player)
        }
    }

    override fun removeViewer(player: Player) {
        ensureNotClosed()
        if (internalViewerUuids.remove(player.uniqueId)) {
            SurfBukkitVisualizerApiImpl.INSTANCE.onViewerRemoved(uid, player.uniqueId)
            if (player.isOnline) {
                onViewerRemoved(player)
            } else {
                clearStaleData(player.uniqueId)
            }
        }
    }

    override fun clearViewers() {
        ensureNotClosed()
        val iterator = internalViewerUuids.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            val player = Bukkit.getPlayer(next)

            SurfBukkitVisualizerApiImpl.INSTANCE.onViewerRemoved(uid, next)
            if (player != null) {
                onViewerRemoved(player)
            } else {
                clearStaleData(next)
            }

            iterator.remove()
        }
    }

    override fun hasViewers() = internalViewerUuids.isNotEmpty()
    override fun visibleTo(player: Player) = player.uniqueId in internalViewerUuids

    open fun onViewerAdded(player: Player) {
        ensureNotClosed()
        if (!visualizing.get()) return

        val version = currentStateVersion()
        player.enterContextIfNeeded {
            if (!isActiveVersion(version)) return@enterContextIfNeeded
            player.sentChunks.forEach { chunk ->
                onPlayerReceiveChunk(player, chunk)
            }
        }
    }

    abstract fun onViewerRemoved(player: Player)
    abstract fun clearStaleData(uuid: UUID)

    @Synchronized
    final override fun close() {
        if (!closed.compareAndSet(false, true)) return

        SurfBukkitVisualizerApiImpl.INSTANCE.onVisualizerClose(this)

        stopVisualizing(true)
        onClose()

        for (viewUuid in internalViewerUuids) {
            SurfBukkitVisualizerApiImpl.INSTANCE.onViewerRemoved(uid, viewUuid)
        }
        internalViewerUuids.clear()
    }

    override fun isClosed(): Boolean {
        return closed.get()
    }

    protected abstract fun onClose()
    protected abstract fun startVisualizingInternal()
    protected abstract fun stopVisualizingInternal()

    abstract fun onPlayerReceiveChunk(player: Player, chunk: Chunk)
    abstract fun onPlayerUnloadChunk(player: Player, chunk: Chunk)

    protected inline fun Player.enterContextIfNeeded(crossinline action: () -> Unit) {
        if (server.isOwnedByCurrentRegion(this)) {
            action()
        } else {
            plugin.launch(plugin.entityDispatcher(this)) {
                action()
            }
        }
    }

    protected fun ensureNotClosed() {
        if (closed.get()) {
            throw IllegalStateException("Visualizer is already closed!")
        }
    }

    protected fun nextStateVersion(): Long = stateVersion.incrementAndGet()
    protected fun currentStateVersion(): Long = stateVersion.get()
    protected fun isActiveVersion(version: Long): Boolean =
        visualizing.get() && stateVersion.get() == version

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractSurfVisualizerImpl) return false

        if (uid != other.uid) return false

        return true
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }
}