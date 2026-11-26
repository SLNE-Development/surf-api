package dev.slne.surf.api.paper.inventory.removal

import dev.slne.surf.api.paper.inventory.internal.*
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

private const val OFF_THREAD_REASON = "Item removal off the thread that owns the inventory"

/**
 * A preview of removing a set of items from [inventory], for example to check in advance whether a
 * player can pay a price, a crafting cost or a quest requirement.
 *
 * The set is available only when the inventory holds every item in its full amount. Similar items,
 * as Bukkit's `ItemStack.isSimilar` defines them, form one entry, so an item with a different name,
 * enchantment or other data counts as a different item.
 *
 * The plan describes the inventory at the moment it was made and never updates. [commit] works on
 * the current contents and follows an [ItemRemovalPolicy], so it stays safe when the inventory
 * changed in between.
 *
 * Planning runs Bukkit's own `Inventory.removeItem` on a copy of the slots in scope, and committing
 * runs it on the inventory itself. On a player inventory, only the storage slots are in scope, plus
 * the offhand and the armor slots when the plan includes them; the crafting grid stays untouched.
 * A commit takes from the storage slots first, then from the offhand, then from the armor slots.
 *
 * Planning and committing must run on the thread that owns the inventory: the thread of the
 * holding entity, or the region thread of the inventory's location. An inventory with neither,
 * such as one from `Bukkit.createInventory`, belongs to no region, and every tick thread owns it.
 * On any other thread, planning and committing throw [IllegalStateException]. The suspending
 * variants, `planRemovalAsync` and [commitAsync], switch to an owning thread themselves, so a
 * coroutine can plan and commit from any thread:
 *
 * ```kotlin
 * scope.launch {
 *     val plan = player.planRemovalAsync(price, 16)
 *     if (!plan.isAvailable) return@launch
 *
 *     plan.commitAsync()
 * }
 * ```
 *
 * @property entries one entry per set of similar items, in the order they were first given
 */
class ItemRemovalPlan internal constructor(
    val inventory: Inventory,
    val entries: List<Entry>,
    private val scope: RemovalScope,
) {

    /**
     * The number of items to remove.
     */
    val amount: Int
        get() = entries.sumOf { it.amount }

    /**
     * The number of items the inventory lacks.
     */
    val missing: Int
        get() = entries.sumOf { it.missing }

    /**
     * Whether the inventory holds every item in its full amount.
     */
    val isAvailable: Boolean
        get() = missing == 0

    /**
     * Returns the number of items similar to [item] that the inventory lacks.
     *
     * @throws IllegalArgumentException if the plan holds no item similar to [item]
     */
    fun missingOf(item: ItemStack): Int {
        return entries.similarTo(item, Entry::item).missing
    }

    /**
     * Removes the items from the current inventory and applies [policy] when it no longer holds
     * enough of them.
     *
     * @throws IllegalStateException off the thread that owns the inventory
     */
    fun commit(policy: ItemRemovalPolicy = ItemRemovalPolicy.REJECT): ItemRemovalResult {
        inventory.ensureOwningThread(OFF_THREAD_REASON)

        val requests = entries.map { ItemAmount.of(it.item, it.amount) }

        if (policy == ItemRemovalPolicy.REJECT && !inventory.holdsNow(requests)) {
            return ItemRemovalResult(entries.map(::rejectedEntry))
        }

        val missingAmounts = inventory.removeInScope(requests, scope)

        return ItemRemovalResult(requests.zip(missingAmounts, ::resultEntry))
    }

    /**
     * Runs [commit] on the thread that owns the inventory, switching there first.
     *
     * Cancelling the coroutine before the commit starts leaves the inventory unchanged; a commit
     * that has started completes.
     *
     * @throws IllegalStateException if the calling coroutine does not belong to a plugin that runs
     * on surf-api
     * @throws kotlinx.coroutines.CancellationException if the coroutine is cancelled, or the entity
     * holding the inventory is removed, before the commit starts
     */
    suspend fun commitAsync(
        policy: ItemRemovalPolicy = ItemRemovalPolicy.REJECT,
    ): ItemRemovalResult {
        return inventory.onOwningThread { commit(policy) }
    }

    private fun Inventory.holdsNow(requests: List<ItemAmount>): Boolean {
        val slots = copySlotsInScope(scope)

        return requests.all { request -> slots.containsAtLeast(request.item, request.amount) }
    }

    override fun toString(): String = "ItemRemovalPlan(entries=$entries, scope=$scope)"

    /**
     * The planned removal of all items similar to [item].
     *
     * @property item a single item that stands for the entry; its amount carries no meaning
     * @property amount the number of these items to remove
     * @property missing the number of these items the inventory lacks
     */
    class Entry internal constructor(val item: ItemStack, val amount: Int, val missing: Int) {

        /**
         * Whether the inventory holds all [amount] items of this entry.
         */
        val isAvailable: Boolean
            get() = missing == 0

        override fun toString(): String {
            return "Entry(item=$item, amount=$amount, missing=$missing)"
        }
    }
}

/**
 * Plans the removal of [amount] items similar to [item] from this inventory.
 *
 * The amount of [item] itself is ignored; [amount] may exceed one stack and be spread over any
 * number of slots.
 *
 * @throws IllegalArgumentException if [item] is empty or [amount] is not positive
 * @throws IllegalStateException off the thread that owns the inventory
 * @see ItemRemovalPlan
 */
fun Inventory.planRemoval(item: ItemStack, amount: Int): ItemRemovalPlan {
    return planItemRemoval(listOf(ItemAmount.of(item, amount)), RemovalScope.STORAGE)
}

/**
 * Plans the removal of [items] from this inventory, each in its own amount.
 *
 * @throws IllegalArgumentException if one of [items] is empty
 * @throws IllegalStateException off the thread that owns the inventory
 * @see ItemRemovalPlan
 */
fun Inventory.planRemoval(vararg items: ItemStack): ItemRemovalPlan {
    return planRemoval(items.asList())
}

/**
 * Plans the removal of [items] from this inventory, each in its own amount.
 *
 * An empty collection is available.
 *
 * @throws IllegalArgumentException if one of [items] is empty
 * @throws IllegalStateException off the thread that owns the inventory
 * @see ItemRemovalPlan
 */
fun Inventory.planRemoval(items: Collection<ItemStack>): ItemRemovalPlan {
    return planItemRemoval(itemAmountsOf(items), RemovalScope.STORAGE)
}

/**
 * Plans the removal of [items] from this inventory, each key in the amount it maps to.
 *
 * The amount of each key itself is ignored. An empty map is available.
 *
 * @throws IllegalArgumentException if a key is empty or an amount is not positive
 * @throws IllegalStateException off the thread that owns the inventory
 * @see ItemRemovalPlan
 */
fun Inventory.planRemoval(items: Map<ItemStack, Int>): ItemRemovalPlan {
    return planItemRemoval(itemAmountsOf(items), RemovalScope.STORAGE)
}

/**
 * Plans the removal of [amount] items similar to [item] from the storage slots of this player.
 *
 * @param includeOffhand whether the offhand counts as well
 * @param includeArmor whether the worn armor counts as well
 * @see Inventory.planRemoval
 */
fun Player.planRemoval(
    item: ItemStack,
    amount: Int,
    includeOffhand: Boolean = false,
    includeArmor: Boolean = false,
): ItemRemovalPlan {
    return inventory.planItemRemoval(
        listOf(ItemAmount.of(item, amount)),
        RemovalScope(includeOffhand = includeOffhand, includeArmor = includeArmor),
    )
}

/**
 * Plans the removal of [items], each in its own amount, from the storage slots of this player.
 *
 * @param includeOffhand whether the offhand counts as well
 * @param includeArmor whether the worn armor counts as well
 * @see Inventory.planRemoval
 */
fun Player.planRemoval(
    vararg items: ItemStack,
    includeOffhand: Boolean = false,
    includeArmor: Boolean = false,
): ItemRemovalPlan {
    return planRemoval(
        items.asList(),
        includeOffhand = includeOffhand,
        includeArmor = includeArmor,
    )
}

/**
 * Plans the removal of [items], each in its own amount, from the storage slots of this player.
 *
 * @param includeOffhand whether the offhand counts as well
 * @param includeArmor whether the worn armor counts as well
 * @see Inventory.planRemoval
 */
fun Player.planRemoval(
    items: Collection<ItemStack>,
    includeOffhand: Boolean = false,
    includeArmor: Boolean = false,
): ItemRemovalPlan {
    return inventory.planItemRemoval(
        itemAmountsOf(items),
        RemovalScope(includeOffhand = includeOffhand, includeArmor = includeArmor),
    )
}

/**
 * Plans the removal of [items], each key in the amount it maps to, from the storage slots of this
 * player.
 *
 * @param includeOffhand whether the offhand counts as well
 * @param includeArmor whether the worn armor counts as well
 * @see Inventory.planRemoval
 */
fun Player.planRemoval(
    items: Map<ItemStack, Int>,
    includeOffhand: Boolean = false,
    includeArmor: Boolean = false,
): ItemRemovalPlan {
    return inventory.planItemRemoval(
        itemAmountsOf(items),
        RemovalScope(includeOffhand = includeOffhand, includeArmor = includeArmor),
    )
}

/**
 * Plans the removal of [amount] items similar to [item] from this inventory, on the thread that
 * owns it.
 *
 * @throws IllegalArgumentException if [item] is empty or [amount] is not positive
 * @throws IllegalStateException if the calling coroutine does not belong to a plugin that runs on
 * surf-api
 * @throws kotlinx.coroutines.CancellationException if the entity holding the inventory is removed
 * first
 * @see Inventory.planRemoval
 */
suspend fun Inventory.planRemovalAsync(item: ItemStack, amount: Int): ItemRemovalPlan {
    return onOwningThread { planRemoval(item, amount) }
}

/**
 * Plans the removal of [items], each in its own amount, from this inventory, on the thread that
 * owns it.
 *
 * @see Inventory.planRemovalAsync
 */
suspend fun Inventory.planRemovalAsync(vararg items: ItemStack): ItemRemovalPlan {
    return planRemovalAsync(items.asList())
}

/**
 * Plans the removal of [items], each in its own amount, from this inventory, on the thread that
 * owns it.
 *
 * @see Inventory.planRemovalAsync
 */
suspend fun Inventory.planRemovalAsync(items: Collection<ItemStack>): ItemRemovalPlan {
    return onOwningThread { planRemoval(items) }
}

/**
 * Plans the removal of [items], each key in the amount it maps to, from this inventory, on the
 * thread that owns it.
 *
 * @see Inventory.planRemovalAsync
 */
suspend fun Inventory.planRemovalAsync(items: Map<ItemStack, Int>): ItemRemovalPlan {
    return onOwningThread { planRemoval(items) }
}

/**
 * Plans the removal of [amount] items similar to [item] from the storage slots of this player, on
 * the player's thread.
 *
 * @param includeOffhand whether the offhand counts as well
 * @param includeArmor whether the worn armor counts as well
 * @see Inventory.planRemovalAsync
 */
suspend fun Player.planRemovalAsync(
    item: ItemStack,
    amount: Int,
    includeOffhand: Boolean = false,
    includeArmor: Boolean = false,
): ItemRemovalPlan {
    return inventory.onOwningThread {
        planRemoval(
            item,
            amount,
            includeOffhand = includeOffhand,
            includeArmor = includeArmor,
        )
    }
}

/**
 * Plans the removal of [items], each in its own amount, from the storage slots of this player, on
 * the player's thread.
 *
 * @param includeOffhand whether the offhand counts as well
 * @param includeArmor whether the worn armor counts as well
 * @see Inventory.planRemovalAsync
 */
suspend fun Player.planRemovalAsync(
    vararg items: ItemStack,
    includeOffhand: Boolean = false,
    includeArmor: Boolean = false,
): ItemRemovalPlan {
    return planRemovalAsync(
        items.asList(),
        includeOffhand = includeOffhand,
        includeArmor = includeArmor,
    )
}

/**
 * Plans the removal of [items], each in its own amount, from the storage slots of this player, on
 * the player's thread.
 *
 * @param includeOffhand whether the offhand counts as well
 * @param includeArmor whether the worn armor counts as well
 * @see Inventory.planRemovalAsync
 */
suspend fun Player.planRemovalAsync(
    items: Collection<ItemStack>,
    includeOffhand: Boolean = false,
    includeArmor: Boolean = false,
): ItemRemovalPlan {
    return inventory.onOwningThread {
        planRemoval(items, includeOffhand = includeOffhand, includeArmor = includeArmor)
    }
}

/**
 * Plans the removal of [items], each key in the amount it maps to, from the storage slots of this
 * player, on the player's thread.
 *
 * @param includeOffhand whether the offhand counts as well
 * @param includeArmor whether the worn armor counts as well
 * @see Inventory.planRemovalAsync
 */
suspend fun Player.planRemovalAsync(
    items: Map<ItemStack, Int>,
    includeOffhand: Boolean = false,
    includeArmor: Boolean = false,
): ItemRemovalPlan {
    return inventory.onOwningThread {
        planRemoval(items, includeOffhand = includeOffhand, includeArmor = includeArmor)
    }
}

private fun Inventory.planItemRemoval(
    requests: List<ItemAmount>,
    scope: RemovalScope,
): ItemRemovalPlan {
    ensureOwningThread(OFF_THREAD_REASON)

    val missingAmounts = copySlotsInScope(scope).removeInScope(requests, RemovalScope.STORAGE)
    val entries = requests.zip(missingAmounts) { request, missing ->
        ItemRemovalPlan.Entry(item = request.item, amount = request.amount, missing = missing)
    }

    return ItemRemovalPlan(this, entries, scope)
}

private fun resultEntry(request: ItemAmount, missing: Int): ItemRemovalResult.Entry {
    return ItemRemovalResult.Entry(
        item = request.item,
        removed = request.amount - missing,
        missing = missing,
    )
}

private fun rejectedEntry(entry: ItemRemovalPlan.Entry): ItemRemovalResult.Entry {
    return ItemRemovalResult.Entry(item = entry.item, removed = 0, missing = entry.amount)
}
