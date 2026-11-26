package dev.slne.surf.api.paper.sharedbean.consumer

import dev.slne.surf.api.paper.inventory.removal.ItemRemovalPlan
import dev.slne.surf.api.paper.inventory.removal.ItemRemovalPolicy
import dev.slne.surf.api.paper.inventory.removal.planRemoval
import io.papermc.paper.datacomponent.DataComponentTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.springframework.stereotype.Service

/**
 * Runs item removal scenarios against the server's own inventories and reports what it observed.
 *
 * Every field is a raw observation, such as `isAvailable/missing` or the items left in a slot; the
 * boot test compares them with values worked out by hand.
 */
@Service
class ConsumerRemovalProbe {

    /**
     * Runs every scenario and returns its fields in `name=observation` form.
     *
     * Starts on the server thread, which owns every inventory the scenarios create.
     */
    suspend fun run(): List<String> {
        return listOf(
            observe("removeSpread") { amountSpreadOverStacks() },
            observe("removeCommit") { commitOnUnchangedInventory() },
            observe("removeSeveral") { severalItemsTogether() },
            observe("removeSeveralMap") { severalItemsAsMap() },
            observe("removeSeveralCommit") { commitSeveralItems() },
            observe("removeSeveralReject") { rejectSeveralItemsOnChangedInventory() },
            observe("removeSimilarMerged") { similarItemsMerged() },
            observe("removeNotSimilar") { renamedItemsNotCounted() },
            observe("removePolicyReject") { rejectOnChangedInventory() },
            observe("removePolicyPartial") { partialOnChangedInventory() },
            observe("removeArmor") { armorOption() },
            observe("removeArmorUntouched") { armorUntouchedByDefault() },
            observe("removeOffhand") { offhandOption() },
            observe("removeOffhandUntouched") { offhandUntouchedByDefault() },
            observe("removeOffThreadPlan") { planOffThread() },
            observe("removeOffThreadCommit") { commitOffThread() },
            observe("removeOffThreadPlayer") { planOffThreadOnPlayer() },
        )
    }

    private fun amountSpreadOverStacks(): String {
        val inventory = inventoryWithSpreadDirt()

        return "${describe(inventory.planRemoval(dirt(), 100))}/" +
            describe(inventory.planRemoval(dirt(), 200))
    }

    private fun commitOnUnchangedInventory(): String {
        val inventory = inventoryWithSpreadDirt()
        val plan = inventory.planRemoval(dirt(), 100)
        val result = plan.commit()

        return "${describe(plan)}/${result.removed}/${result.missing}/" +
            "dirt:${inventory.countOf(Material.DIRT)}"
    }

    private fun severalItemsTogether(): String {
        val plan = dirtAndCobblestone().planRemoval(listOf(dirtStack(30), cobblestone(20)))

        return describeEntries(plan)
    }

    private fun severalItemsAsMap(): String {
        val plan = dirtAndCobblestone().planRemoval(mapOf(dirt() to 30, cobblestone() to 10))

        return describeEntries(plan)
    }

    private fun commitSeveralItems(): String {
        val inventory = dirtAndCobblestone()
        val result = inventory.planRemoval(dirtStack(30), cobblestone(20))
            .commit(ItemRemovalPolicy.PARTIAL)
        val entries = result.entries.joinToString(",") { entry ->
            "${entry.item.type}:${entry.removed}/${entry.missing}"
        }

        return "$entries/${result.removed}/${result.missing}/" +
            "dirt:${inventory.countOf(Material.DIRT)}/" +
            "cobblestone:${inventory.countOf(Material.COBBLESTONE)}"
    }

    /**
     * Takes cobblestone away after planning, so only one of two items falls short.
     */
    private fun rejectSeveralItemsOnChangedInventory(): String {
        val inventory = dirtAndCobblestone()
        val plan = inventory.planRemoval(dirtStack(30), cobblestone(10))

        inventory.setItem(1, cobblestone(5))

        val result = plan.commit()

        return "${describe(plan)}/${result.removed}/${result.missing}/" +
            "dirt:${inventory.countOf(Material.DIRT)}/" +
            "cobblestone:${inventory.countOf(Material.COBBLESTONE)}"
    }

    private fun similarItemsMerged(): String {
        val inventory = Bukkit.createInventory(null, 9)

        inventory.setItem(0, dirtStack(64))

        val plan = inventory.planRemoval(dirtStack(10), dirtStack(20))

        return "${plan.entries.size}/${plan.amount}/${plan.missingOf(dirt())}"
    }

    /**
     * Holds 64 renamed dirt and 10 plain dirt, so only the plain dirt counts for plain dirt.
     */
    private fun renamedItemsNotCounted(): String {
        val inventory = Bukkit.createInventory(null, 9)

        inventory.setItem(0, renamedDirt(64))
        inventory.setItem(1, dirtStack(10))

        val plan = inventory.planRemoval(dirt(), 20)
        val result = plan.commit(ItemRemovalPolicy.PARTIAL)

        return "${describe(plan)}/${result.removed}/${result.missing}/" +
            "dirt:${inventory.countOf(Material.DIRT)}"
    }

    private fun rejectOnChangedInventory(): String {
        val inventory = Bukkit.createInventory(null, 9)

        inventory.setItem(0, dirtStack(64))

        val plan = inventory.planRemoval(dirt(), 50)

        inventory.setItem(0, dirtStack(30))

        val result = plan.commit()

        return "${describe(plan)}/${result.removed}/${result.missing}/" +
            "dirt:${inventory.countOf(Material.DIRT)}"
    }

    private fun partialOnChangedInventory(): String {
        val inventory = Bukkit.createInventory(null, 9)

        inventory.setItem(0, dirtStack(64))

        val plan = inventory.planRemoval(dirt(), 50)

        inventory.setItem(0, dirtStack(30))

        val result = plan.commit(ItemRemovalPolicy.PARTIAL)

        return "${describe(plan)}/${result.removed}/${result.missing}/" +
            "dirt:${inventory.countOf(Material.DIRT)}"
    }

    private fun armorOption(): String {
        val player = playerWithStoredAndWornHelmet()
        val withoutArmor = player.planRemoval(helmet(), 2)
        val withArmor = player.planRemoval(helmet(), 2, includeArmor = true)
        val result = withArmor.commit()

        return "${describe(withoutArmor)}/${describe(withArmor)}/" +
            "${result.removed}/${result.missing}/" +
            "helmets:${player.inventory.countOf(Material.DIAMOND_HELMET)}"
    }

    private fun armorUntouchedByDefault(): String {
        val player = playerWithStoredAndWornHelmet()
        val result = player.planRemoval(helmet(), 2).commit(ItemRemovalPolicy.PARTIAL)

        return "${result.removed}/${result.missing}/head:${player.inventory.helmet.typeName()}"
    }

    private fun offhandOption(): String {
        val player = playerWithStoredAndOffhandDirt()
        val withoutOffhand = player.planRemoval(dirt(), 15)
        val withOffhand = player.planRemoval(dirt(), 15, includeOffhand = true)
        val result = withOffhand.commit()

        return "${describe(withoutOffhand)}/${describe(withOffhand)}/" +
            "${result.removed}/${result.missing}/" +
            "offhand:${player.inventory.itemInOffHand.typeName()}/" +
            "dirt:${player.inventory.countOf(Material.DIRT)}"
    }

    private fun offhandUntouchedByDefault(): String {
        val player = playerWithStoredAndOffhandDirt()
        val result = player.planRemoval(dirt(), 15).commit(ItemRemovalPolicy.PARTIAL)
        val offhand = player.inventory.itemInOffHand

        return "${result.removed}/${result.missing}/offhand:${offhand.typeName()}:${offhand.amount}"
    }

    private suspend fun planOffThread(): String {
        val inventory = Bukkit.createInventory(null, 9)

        return withContext(Dispatchers.Default) {
            thrownBy { inventory.planRemoval(dirt(), 1) }
        }
    }

    private suspend fun commitOffThread(): String {
        val plan = Bukkit.createInventory(null, 9).planRemoval(dirt(), 1)

        return withContext(Dispatchers.Default) {
            thrownBy { plan.commit() }
        }
    }

    private suspend fun planOffThreadOnPlayer(): String {
        val player = createDetachedPlayer()

        return withContext(Dispatchers.Default) {
            thrownBy { player.planRemoval(dirt(), 1) }
        }
    }

    /**
     * Holds 148 dirt in three stacks of 64, 64 and 20 in non-adjacent slots.
     */
    private fun inventoryWithSpreadDirt(): Inventory {
        val inventory = Bukkit.createInventory(null, 27)

        inventory.setItem(0, dirtStack(64))
        inventory.setItem(5, dirtStack(64))
        inventory.setItem(9, dirtStack(20))

        return inventory
    }

    private fun dirtAndCobblestone(): Inventory {
        val inventory = Bukkit.createInventory(null, 9)

        inventory.setItem(0, dirtStack(64))
        inventory.setItem(1, cobblestone(10))

        return inventory
    }

    private fun playerWithStoredAndWornHelmet(): Player {
        val player = createDetachedPlayer()

        player.inventory.setItem(0, helmet())
        player.inventory.setHelmet(helmet())

        return player
    }

    private fun playerWithStoredAndOffhandDirt(): Player {
        val player = createDetachedPlayer()

        player.inventory.setItem(0, dirtStack(10))
        player.inventory.setItemInOffHand(dirtStack(5))

        return player
    }

    private fun describe(plan: ItemRemovalPlan) = "${plan.isAvailable}/${plan.missing}"

    private fun describeEntries(plan: ItemRemovalPlan): String {
        val missing = plan.entries.joinToString(",") { entry ->
            "${entry.item.type}:${entry.missing}"
        }

        return "${plan.isAvailable}/$missing"
    }

    private fun helmet(): ItemStack = ItemStack.of(Material.DIAMOND_HELMET)

    @Suppress("UnstableApiUsage")
    private fun renamedDirt(amount: Int): ItemStack {
        val item = dirtStack(amount)

        item.setData(DataComponentTypes.CUSTOM_NAME, Component.text("Special"))

        return item
    }
}
