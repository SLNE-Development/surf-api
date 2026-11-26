package dev.slne.surf.api.paper.sharedbean.consumer

import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.paper.command.executors.anyExecutorSuspend
import dev.slne.surf.api.paper.inventory.insertion.ItemInsertionPlan
import dev.slne.surf.api.paper.inventory.insertion.ItemInsertionPolicy
import dev.slne.surf.api.paper.inventory.insertion.planInsertion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.springframework.stereotype.Service
import java.util.*

/**
 * Runs item insertion scenarios against the server's own inventories and reports what it observed.
 *
 * Every field is a raw observation, such as `fits/leftover` or the item types left in a slot; the
 * boot test compares them with values worked out by hand.
 */
@Service
class ConsumerInsertionProbe {

    /**
     * Runs every scenario and returns its fields in `name=observation` form.
     *
     * Starts on the server thread, which owns every inventory the scenarios create.
     */
    suspend fun run(): List<String> {
        return listOf(
            observe("insertEmpty") { emptyInventory() },
            observe("insertPartial") { partlyFilledInventory() },
            observe("insertNearlyFull") { nearlyFullInventory() },
            observe("insertMultiStack") { moreThanOneStack() },
            observe("insertMaxStack16") { customMaxStackSize(maxStackSize = 16, amount = 150) },
            observe("insertMaxStack99") { customMaxStackSize(maxStackSize = 99, amount = 900) },
            observe("insertCommit99") { commitCustomMaxStackSize() },
            observe("insertCommit") { commitOnUnchangedInventory() },
            observe("insertCommitNotFitting") { commitNotFittingOnUnchangedInventory() },
            observe("insertReject") { rejectOnChangedInventory() },
            observe("insertPlayer") { playerStorageSlotsOnly() },
            observe("insertSeveral") { severalItemsTogether() },
            observe("insertSeveralMap") { severalItemsAsMap() },
            observe("insertSeveralCommit") { commitSeveralItems() },
            observe("insertSimilarMerged") { similarItemsMerged() },
            observe("insertPolicyPartial") { partialOnChangedInventory() },
            observe("insertPolicyDropPlayer") { dropAtPlayer() },
            observe("insertPolicyDropBlock") { dropAtBlock() },
            observe("insertDropNoLocation") { dropWithoutLocation() },
            observe("insertOffhand") { offhandOption() },
            observe("insertOffThreadPlan") { planOffThread() },
            observe("insertOffThreadCommit") { commitOffThread() },
            observe("insertOffThreadPlayer") { planOffThreadOnPlayer() },
        )
    }

    private fun emptyInventory(): String {
        return describe(Bukkit.createInventory(null, 27).planInsertion(dirt(), 100))
    }

    private fun partlyFilledInventory(): String {
        val inventory = Bukkit.createInventory(null, 27)

        for (slot in 0..9) {
            inventory.setItem(slot, ItemStack.of(Material.STONE, 64))
        }

        inventory.setItem(10, ItemStack.of(Material.DIRT, 30))

        return describe(inventory.planInsertion(dirt(), 1100))
    }

    private fun nearlyFullInventory(): String {
        val inventory = Bukkit.createInventory(null, 27)

        for (slot in 0..25) {
            inventory.setItem(slot, ItemStack.of(Material.STONE, 64))
        }

        inventory.setItem(26, ItemStack.of(Material.DIRT, 60))

        return describe(inventory.planInsertion(dirt(), 10))
    }

    private fun moreThanOneStack(): String {
        return describe(Bukkit.createInventory(null, 9).planInsertion(dirt(), 581))
    }

    private fun customMaxStackSize(maxStackSize: Int, amount: Int): String {
        val item = dirt(maxStackSize = maxStackSize)
        val plan = Bukkit.createInventory(null, 9).planInsertion(item, amount)

        return describe(plan)
    }

    private fun commitCustomMaxStackSize(): String {
        val inventory = Bukkit.createInventory(null, 9)
        val result = inventory.planInsertion(dirt(maxStackSize = 99), 891).commit()

        return "${result.inserted}/${result.leftover}/slot:${inventory.getItem(8)?.amount}"
    }

    private fun commitOnUnchangedInventory(): String {
        val inventory = Bukkit.createInventory(null, 27)

        for (slot in 0..4) {
            inventory.setItem(slot, ItemStack.of(Material.DIRT, 50))
        }

        val plan = inventory.planInsertion(dirt(), 200)
        val result = plan.commit()

        return "${describe(plan)}/${result.inserted}/${result.leftover}/" +
            "dirt:${inventory.countOf(Material.DIRT)}"
    }

    /**
     * Commits a plan for 640 dirt into nine empty slots, which hold 576, once under each policy
     * that leaves the rest out.
     */
    private fun commitNotFittingOnUnchangedInventory(): String {
        val policies = listOf(ItemInsertionPolicy.REJECT, ItemInsertionPolicy.PARTIAL)

        return policies.joinToString(",") { policy ->
            val inventory = Bukkit.createInventory(null, 9)
            val result = inventory.planInsertion(dirt(), 640).commit(policy)

            "$policy:${result.inserted}/${result.leftover}/dirt:${inventory.countOf(Material.DIRT)}"
        }
    }

    private fun rejectOnChangedInventory(): String {
        val inventory = Bukkit.createInventory(null, 9)
        val plan = inventory.planInsertion(dirt(), 128)

        for (slot in 0 until inventory.size) {
            inventory.setItem(slot, ItemStack.of(Material.STONE, 64))
        }

        val result = plan.commit()

        return "${describe(plan)}/${result.inserted}/${result.leftover}/" +
            "dirt:${inventory.countOf(Material.DIRT)}"
    }

    /**
     * Fills the storage slots except for 32 free dirt and leaves armor, offhand and crafting grid
     * mostly empty, so an insertion that spilled out of the storage slots would fit where it must
     * not. The setup leaves the crafting grid empty: changing it notifies a client, which the
     * detached player does not have.
     */
    private fun playerStorageSlotsOnly(): String {
        val player = createDetachedPlayer()
        val inventory = player.inventory

        for (slot in 0 until inventory.storageContents.size) {
            inventory.setItem(slot, ItemStack.of(Material.STONE, 64))
        }

        inventory.setItem(0, ItemStack.of(Material.DIRT, 32))
        inventory.setItem(EquipmentSlot.HEAD, ItemStack.of(Material.DIAMOND_HELMET))

        val tooMuch = player.planInsertion(dirt(), 40)
        val result = player.planInsertion(dirt(), 32).commit()
        val armor = inventory.armorContents.joinToString(",") { it.typeName() }
        val crafting = player.openInventory.topInventory.contents.joinToString(",") { slot ->
            slot.typeName()
        }

        return "${describe(tooMuch)}/${result.inserted}/${result.leftover}/" +
            "dirt:${inventory.countOf(Material.DIRT)}/armor:$armor/" +
            "offhand:${inventory.itemInOffHand.typeName()}/crafting:$crafting"
    }

    /**
     * Leaves one free slot, so dirt and cobblestone each fit alone but not together.
     */
    private fun severalItemsTogether(): String {
        val inventory = inventoryWithFreeSlots(1)
        val alone = listOf(dirt(), cobblestone()).joinToString(",") { item ->
            inventory.planInsertion(item, 64).fits.toString()
        }
        val together = inventory.planInsertion(listOf(dirtStack(64), cobblestone(64)))

        return "alone:$alone/${describeEntries(together)}"
    }

    private fun severalItemsAsMap(): String {
        val plan = inventoryWithFreeSlots(1).planInsertion(mapOf(dirt() to 64, cobblestone() to 64))

        return describeEntries(plan)
    }

    private fun commitSeveralItems(): String {
        val inventory = inventoryWithFreeSlots(1)
        val result = inventory.planInsertion(dirtStack(64), cobblestone(64))
            .commit(ItemInsertionPolicy.PARTIAL)
        val entries = result.entries.joinToString(",") { entry ->
            "${entry.item.type}:${entry.inserted}/${entry.leftover}"
        }

        return "$entries/${result.inserted}/${result.leftover}/" +
            "dirt:${inventory.countOf(Material.DIRT)}/" +
            "cobblestone:${inventory.countOf(Material.COBBLESTONE)}"
    }

    private fun similarItemsMerged(): String {
        val plan = Bukkit.createInventory(null, 9).planInsertion(dirtStack(10), dirtStack(20))

        return "${plan.entries.size}/${plan.amount}/${plan.leftoverOf(dirt())}"
    }

    private fun partialOnChangedInventory(): String {
        val inventory = Bukkit.createInventory(null, 9)
        val plan = inventory.planInsertion(dirt(), 128)

        for (slot in 0..7) {
            inventory.setItem(slot, ItemStack.of(Material.STONE, 64))
        }

        val result = plan.commit(ItemInsertionPolicy.PARTIAL)

        return "${describe(plan)}/${result.inserted}/${result.leftover}/" +
            "dirt:${inventory.countOf(Material.DIRT)}"
    }

    private fun dropAtPlayer(): String {
        val player = createDetachedPlayer()
        val plan = player.planInsertion(dirt(), 100)

        player.location.chunk.load()
        fillStorageSlots(player)

        val result = plan.commit(ItemInsertionPolicy.DROP)
        val dropped = describeDroppedDirt(player.location) { owner -> owner == player.uniqueId }

        return "${describe(plan)}/${result.inserted}/${result.leftover}/$dropped"
    }

    /**
     * Uses a real chest high above the spawn point, so the drop has a block location and no owner.
     */
    private fun dropAtBlock(): String {
        val world = Bukkit.getWorlds().first()
        val spawn = world.spawnLocation
        val block = world.getBlockAt(spawn.blockX, world.maxHeight - 4, spawn.blockZ)

        block.type = Material.CHEST

        try {
            val inventory = (block.state as Chest).inventory
            val plan = inventory.planInsertion(dirt(), 30)

            for (slot in 0 until inventory.size) {
                inventory.setItem(slot, ItemStack.of(Material.STONE, 64))
            }

            val result = plan.commit(ItemInsertionPolicy.DROP)
            val dropped = describeDroppedDirt(block.location.toCenterLocation()) { owner ->
                owner == null
            }

            inventory.clear()

            return "${describe(plan)}/${result.inserted}/${result.leftover}/$dropped"
        } finally {
            block.type = Material.AIR
        }
    }

    private fun dropWithoutLocation(): String {
        val inventory = Bukkit.createInventory(null, 9)
        val plan = inventory.planInsertion(dirt(), 1)
        val thrown = thrownBy { plan.commit(ItemInsertionPolicy.DROP) }

        return "$thrown/dirt:${inventory.countOf(Material.DIRT)}"
    }

    private fun offhandOption(): String {
        val player = createDetachedPlayer()

        fillStorageSlots(player)

        val withoutOffhand = player.planInsertion(dirt(), 40)
        val withOffhand = player.planInsertion(dirt(), 40, includeOffhand = true)
        val result = withOffhand.commit()
        val offhand = player.inventory.itemInOffHand

        return "${describe(withoutOffhand)}/${describe(withOffhand)}/" +
            "${result.inserted}/${result.leftover}/offhand:${offhand.typeName()}:${offhand.amount}"
    }

    private fun inventoryWithFreeSlots(freeSlots: Int): Inventory {
        val inventory = Bukkit.createInventory(null, 9)

        for (slot in 0 until inventory.size - freeSlots) {
            inventory.setItem(slot, ItemStack.of(Material.STONE, 64))
        }

        return inventory
    }

    private fun fillStorageSlots(player: Player) {
        val inventory = player.inventory

        for (slot in 0 until inventory.storageContents.size) {
            inventory.setItem(slot, ItemStack.of(Material.STONE, 64))
        }
    }

    /**
     * Describes the dirt lying at [location] as `dropped:amount/owner:ok/unlimited:flag` and
     * removes it.
     */
    private fun describeDroppedDirt(
        location: Location,
        isExpectedOwner: (UUID?) -> Boolean,
    ): String {
        val dropped = location.world
            .getNearbyEntitiesByType(Item::class.java, location, 1.0)
            .filter { item -> item.itemStack.type == Material.DIRT }
        val amount = dropped.sumOf { item -> item.itemStack.amount }
        val owner = if (dropped.all { item -> isExpectedOwner(item.owner) }) "ok" else "wrong"
        val isUnlimited = dropped.isNotEmpty() && dropped.all { item -> item.isUnlimitedLifetime }

        for (item in dropped) {
            item.remove()
        }

        return "dropped:$amount/owner:$owner/unlimited:$isUnlimited"
    }

    private suspend fun planOffThread(): String {
        val inventory = Bukkit.createInventory(null, 9)

        return withContext(Dispatchers.Default) {
            thrownBy { inventory.planInsertion(dirt(), 1) }
        }
    }

    private suspend fun commitOffThread(): String {
        val plan = Bukkit.createInventory(null, 9).planInsertion(dirt(), 1)

        return withContext(Dispatchers.Default) {
            thrownBy { plan.commit() }
        }
    }

    private suspend fun planOffThreadOnPlayer(): String {
        val player = createDetachedPlayer()

        return withContext(Dispatchers.Default) {
            thrownBy { player.planInsertion(dirt(), 1) }
        }
    }

    private fun describe(plan: ItemInsertionPlan) = "${plan.fits}/${plan.leftover}"

    private fun describeEntries(plan: ItemInsertionPlan): String {
        val leftovers = plan.entries.joinToString(",") { entry ->
            "${entry.item.type}:${entry.leftover}"
        }

        return "${plan.fits}/$leftovers"
    }
}

/**
 * A command bean whose console run executes [ConsumerInsertionProbe], [ConsumerRemovalProbe] and
 * [ConsumerAsyncInventoryProbe] and logs their fields on one line.
 */
@Service
class SpikeInventoryCommand(
    private val insertionProbe: ConsumerInsertionProbe,
    private val removalProbe: ConsumerRemovalProbe,
    private val asyncProbe: ConsumerAsyncInventoryProbe,
) : CommandAPICommand(SharedBeanReport.INVENTORY_COMMAND) {

    private val log = logger()

    init {
        anyExecutorSuspend { _, _ ->
            val fields = insertionProbe.run() + removalProbe.run() + asyncProbe.run()
            log.atInfo()
                .log("%s inventory %s", SharedBeanReport.SPIKE_MARKER, fields.joinToString(" "))
        }
    }
}
