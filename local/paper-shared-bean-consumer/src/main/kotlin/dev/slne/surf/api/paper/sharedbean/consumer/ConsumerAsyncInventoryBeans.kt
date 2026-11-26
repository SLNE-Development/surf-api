package dev.slne.surf.api.paper.sharedbean.consumer

import dev.slne.surf.api.paper.inventory.insertion.planInsertion
import dev.slne.surf.api.paper.inventory.insertion.planInsertionAsync
import dev.slne.surf.api.paper.inventory.removal.planRemoval
import dev.slne.surf.api.paper.inventory.removal.planRemovalAsync
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.entity.minecart.StorageMinecart
import org.bukkit.inventory.Inventory
import org.springframework.stereotype.Service
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/**
 * Runs the suspending variants of item insertion and item removal, mostly from a thread that does
 * not own the inventory, and reports what it observed.
 *
 * A commit succeeds only on the owning thread, so a scenario that inserts and removes from
 * `Dispatchers.Default` shows that the variant switched threads. Each scenario has a timeout, so a
 * switch that never happens becomes a field instead of a hanging test.
 */
@Service
class ConsumerAsyncInventoryProbe {

    /**
     * Runs every scenario and returns its fields in `name=observation` form.
     *
     * Starts on the server thread inside the consumer's coroutine scope.
     */
    suspend fun run(): List<String> {
        return listOf(
            observeAsync("asyncPlain") { insertAndRemoveOffThread(plainInventory()) },
            observeAsync("asyncPlainOnTickThread") { insertAndRemoveOnTickThread() },
            observeAsync("asyncBlock") { inChest(::insertAndRemoveOffThread) },
            observeAsync("asyncEntity") { inStorageMinecart(::insertAndRemoveOffThread) },
            observeAsync("asyncBlockingStillThrows") { blockingCommitOffThread() },
            observeAsync("asyncInsertCancelled") { insertionCancelledWhileSwitching() },
            observeAsync("asyncRemoveCancelled") { removalCancelledWhileSwitching() },
            observeAsync("asyncCancelledOnOwner") { insertionCancelledOnOwningThread() },
            observeAsync("asyncOutsidePlugin") { outsidePluginCoroutine() },
        )
    }

    /**
     * Inserts 10 dirt and removes 4 again, planning and committing from `Dispatchers.Default`.
     */
    private suspend fun insertAndRemoveOffThread(inventory: Inventory): String {
        val outcome = withContext(Dispatchers.Default) {
            val insertion = inventory.planInsertionAsync(dirt(), 10)
            val inserted = insertion.commitAsync()
            val removal = inventory.planRemovalAsync(dirt(), 4)
            val removed = removal.commitAsync()

            "${insertion.fits}/${inserted.inserted}/${removal.isAvailable}/${removed.removed}"
        }

        return "$outcome/dirt:${inventory.countOf(Material.DIRT)}"
    }

    /**
     * Inserts 10 dirt into an inventory without a region and removes 4 again from the server
     * thread, where the suspending variants run in place.
     */
    private suspend fun insertAndRemoveOnTickThread(): String {
        val inventory = plainInventory()
        val inserted = inventory.planInsertionAsync(dirt(), 10).commitAsync()
        val removal = inventory.planRemovalAsync(dirt(), 4)
        val removed = removal.commitAsync()

        return "${inserted.leftover == 0}/${inserted.inserted}/${removal.isAvailable}/" +
            "${removed.removed}/dirt:${inventory.countOf(Material.DIRT)}"
    }

    private suspend fun blockingCommitOffThread(): String {
        val inventory = Bukkit.createInventory(null, 9)

        return withContext(Dispatchers.Default) {
            val plan = inventory.planInsertionAsync(dirt(), 1)

            thrownBy { plan.commit() }
        }
    }

    private suspend fun insertionCancelledWhileSwitching(): String {
        val inventory = Bukkit.createInventory(null, 9)
        val plan = inventory.planInsertion(dirt(), 5)
        val isCancelled = cancelWhileSwitching { plan.commitAsync() }

        return "$isCancelled/dirt:${inventory.countOf(Material.DIRT)}"
    }

    private suspend fun removalCancelledWhileSwitching(): String {
        val inventory = Bukkit.createInventory(null, 9)

        inventory.setItem(0, dirtStack(10))

        val plan = inventory.planRemoval(dirt(), 5)
        val isCancelled = cancelWhileSwitching { plan.commitAsync() }

        return "$isCancelled/dirt:${inventory.countOf(Material.DIRT)}"
    }

    /**
     * Starts [commit] on `Dispatchers.Default` and cancels it while it waits for the server thread.
     *
     * The calling coroutine holds the server thread from the start of [commit] until it joins, so
     * the commit cannot run before the cancellation.
     *
     * @return whether the commit ended cancelled
     */
    private suspend fun cancelWhileSwitching(commit: suspend () -> Unit): Boolean {
        val started = CountDownLatch(1)

        return coroutineScope {
            val job = launch(Dispatchers.Default) {
                started.countDown()
                commit()
            }

            check(started.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                "The commit did not start."
            }

            job.cancel()
            job.join()

            job.isCancelled
        }
    }

    /**
     * Cancels the coroutine on the server thread itself, where no thread switch is needed.
     */
    private suspend fun insertionCancelledOnOwningThread(): String {
        val inventory = Bukkit.createInventory(null, 9)
        val plan = inventory.planInsertion(dirt(), 5)
        val commit = coroutineScope {
            launch(start = CoroutineStart.UNDISPATCHED) {
                cancel()
                plan.commitAsync()
            }
        }

        return "${commit.isCancelled}/dirt:${inventory.countOf(Material.DIRT)}"
    }

    /**
     * Calls a suspending variant from a coroutine that does not belong to a plugin.
     */
    private suspend fun outsidePluginCoroutine(): String {
        val inventory = Bukkit.createInventory(null, 9)
        val foreignScope = CoroutineScope(Dispatchers.Default)

        try {
            foreignScope.async { inventory.planInsertionAsync(dirt(), 1) }.await()

            return "none"
        } catch (failure: IllegalStateException) {
            return failure.javaClass.simpleName
        } finally {
            foreignScope.cancel()
        }
    }

    /**
     * Runs [scenario] on the inventory of a chest high above the spawn point, in a chunk that stays
     * loaded until the scenario ends.
     */
    private suspend fun inChest(scenario: suspend (Inventory) -> String): String {
        val location = probeLocation()
        val block = location.block

        return withForceLoadedChunk(location) {
            block.type = Material.CHEST

            try {
                scenario((block.state as Chest).inventory)
            } finally {
                block.type = Material.AIR
            }
        }
    }

    /**
     * Runs [scenario] on the inventory of a chest minecart, an inventory held by an entity.
     */
    private suspend fun inStorageMinecart(scenario: suspend (Inventory) -> String): String {
        val location = probeLocation()

        return withForceLoadedChunk(location) {
            val minecart = location.world.spawn(location, StorageMinecart::class.java)

            minecart.setGravity(false)

            try {
                scenario(minecart.inventory)
            } finally {
                minecart.inventory.clear()
                minecart.remove()
            }
        }
    }

    private suspend fun withForceLoadedChunk(
        location: Location,
        scenario: suspend () -> String,
    ): String {
        val chunk = location.chunk
        val wasForceLoaded = chunk.isForceLoaded

        chunk.isForceLoaded = true

        try {
            return scenario()
        } finally {
            chunk.isForceLoaded = wasForceLoaded
        }
    }

    private fun plainInventory(): Inventory = Bukkit.createInventory(null, 9)

    private fun probeLocation(): Location {
        val world = Bukkit.getWorlds().first()
        val spawn = world.spawnLocation

        return Location(world, spawn.blockX + 0.5, world.maxHeight - 8.0, spawn.blockZ + 0.5)
    }

    private suspend fun observeAsync(name: String, scenario: suspend () -> String): String {
        return observe(name) {
            withTimeout(SCENARIO_TIMEOUT) { scenario() }
        }
    }

    private companion object {
        val SCENARIO_TIMEOUT = 10.seconds
        const val LATCH_TIMEOUT_SECONDS = 5L
    }
}
