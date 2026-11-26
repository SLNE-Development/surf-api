package dev.slne.surf.api.paper.sharedbean.consumer

import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.paper.command.executors.anyExecutorSuspend
import dev.slne.surf.api.paper.inventory.insertion.ItemInsertionPlan
import dev.slne.surf.api.paper.inventory.insertion.planInsertion
import io.papermc.paper.datacomponent.DataComponentTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Material
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
class ConsumerInventoryProbe {

    private val log = logger()

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
            observe("insertMaxStack16") { customMaxStackSize(16, 150) },
            observe("insertMaxStack99") { customMaxStackSize(99, 900) },
            observe("insertCommit99") { commitCustomMaxStackSize() },
            observe("insertCommit") { commitOnUnchangedInventory() },
            observe("insertReject") { rejectOnChangedInventory() },
            observe("insertPlayer") { playerStorageSlotsOnly() },
            observeSuspend("insertOffThreadPlan") { planOffThread() },
            observeSuspend("insertOffThreadCommit") { commitOffThread() },
            observeSuspend("insertOffThreadPlayer") { planOffThreadOnPlayer() },
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
        val plan = Bukkit.createInventory(null, 9).planInsertion(dirt(maxStackSize), amount)

        return describe(plan)
    }

    private fun commitCustomMaxStackSize(): String {
        val inventory = Bukkit.createInventory(null, 9)
        val result = inventory.planInsertion(dirt(99), 891).commit()

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
        val crafting = player.openInventory.topInventory.contents.joinToString(",") {
            it.typeName()
        }

        return "${describe(tooMuch)}/${result.inserted}/${result.leftover}/" +
            "dirt:${inventory.countOf(Material.DIRT)}/armor:$armor/" +
            "offhand:${inventory.itemInOffHand.typeName()}/crafting:$crafting"
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

    private fun thrownBy(action: () -> Unit): String {
        return try {
            action()
            "none"
        } catch (failure: Exception) {
            failure.javaClass.simpleName
        }
    }

    private fun observe(name: String, scenario: () -> String): String {
        return "$name=" + try {
            scenario()
        } catch (failure: Exception) {
            reportFailure(name, failure)
        }
    }

    private suspend fun observeSuspend(name: String, scenario: suspend () -> String): String {
        return "$name=" + try {
            scenario()
        } catch (failure: Exception) {
            reportFailure(name, failure)
        }
    }

    private fun reportFailure(name: String, failure: Exception): String {
        log.atWarning()
            .withCause(failure)
            .log("The inventory scenario '%s' failed", name)

        return "failed:${failure.javaClass.simpleName}"
    }

    private fun describe(plan: ItemInsertionPlan) = "${plan.fits}/${plan.leftover}"

    @Suppress("UnstableApiUsage")
    private fun dirt(maxStackSize: Int? = null): ItemStack {
        val item = ItemStack.of(Material.DIRT)

        if (maxStackSize != null) {
            item.setData(DataComponentTypes.MAX_STACK_SIZE, maxStackSize)
        }

        return item
    }

    private fun Inventory.countOf(material: Material): Int {
        return contents.filterNotNull()
            .filter { it.type == material }
            .sumOf { it.amount }
    }

    private fun ItemStack?.typeName(): String = (this?.type ?: Material.AIR).name

    /**
     * Creates a player that never joined: a real server player with Bukkit's own player inventory,
     * neither connected nor added to a world.
     *
     * The boot test has no client, and Bukkit offers no way to create a player, so the server's own
     * classes are reached reflectively. The server runs with Mojang's names.
     */
    private fun createDetachedPlayer(): Player {
        val server = Bukkit.getServer()
        val world = Bukkit.getWorlds().first()
        val minecraftServer = server.javaClass.getMethod("getServer").invoke(server)
        val level = world.javaClass.getMethod("getHandle").invoke(world)
        val profile = Class.forName("com.mojang.authlib.GameProfile")
            .getConstructor(UUID::class.java, String::class.java)
            .newInstance(UUID.randomUUID(), "InventoryProbe")
        val clientInformation = Class.forName("net.minecraft.server.level.ClientInformation")
            .getMethod("createDefault")
            .invoke(null)
        val serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer")
        val serverPlayer = serverPlayerClass.constructors
            .single { it.parameterCount == 4 }
            .newInstance(minecraftServer, level, profile, clientInformation)

        return serverPlayerClass.getMethod("getBukkitEntity").invoke(serverPlayer) as Player
    }
}

/**
 * A command bean whose console run executes [ConsumerInventoryProbe] and logs its fields.
 */
@Service
class SpikeInventoryCommand(private val probe: ConsumerInventoryProbe) :
    CommandAPICommand(SharedBeanReport.INVENTORY_COMMAND) {

    private val log = logger()

    init {
        anyExecutorSuspend { _, _ ->
            val fields = probe.run()
            log.atInfo()
                .log("%s inventory %s", SharedBeanReport.SPIKE_MARKER, fields.joinToString(" "))
        }
    }
}
