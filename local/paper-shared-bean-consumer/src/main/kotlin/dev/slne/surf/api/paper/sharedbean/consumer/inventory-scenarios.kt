package dev.slne.surf.api.paper.sharedbean.consumer

import dev.slne.surf.api.common.util.logger
import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

private val log = logger()

internal fun thrownBy(action: () -> Unit): String {
    return try {
        action()
        "none"
    } catch (failure: Exception) {
        failure.javaClass.simpleName
    }
}

internal fun observe(name: String, scenario: () -> String): String {
    return "$name=" + try {
        scenario()
    } catch (failure: Exception) {
        reportFailure(name, failure)
    }
}

internal suspend fun observeSuspend(name: String, scenario: suspend () -> String): String {
    return "$name=" + try {
        scenario()
    } catch (failure: Exception) {
        reportFailure(name, failure)
    }
}

internal fun reportFailure(name: String, failure: Exception): String {
    log.atWarning()
        .withCause(failure)
        .log("The inventory scenario '%s' failed", name)

    return "failed:${failure.javaClass.simpleName}"
}

internal fun dirtStack(amount: Int): ItemStack = ItemStack.of(Material.DIRT, amount)

internal fun cobblestone(amount: Int = 1): ItemStack = ItemStack.of(Material.COBBLESTONE, amount)

@Suppress("UnstableApiUsage")
internal fun dirt(maxStackSize: Int? = null): ItemStack {
    val item = ItemStack.of(Material.DIRT)

    if (maxStackSize != null) {
        item.setData(DataComponentTypes.MAX_STACK_SIZE, maxStackSize)
    }

    return item
}

internal fun Inventory.countOf(material: Material): Int {
    return contents.filterNotNull()
        .filter { it.type == material }
        .sumOf { it.amount }
}

internal fun ItemStack?.typeName(): String = (this?.type ?: Material.AIR).name

/**
 * Creates a player that never joined: a real server player with Bukkit's own player inventory,
 * neither connected nor added to a world.
 *
 * The boot test has no client, and Bukkit offers no way to create a player, so the server's own
 * classes are reached reflectively. The server runs with Mojang's names.
 */
internal fun createDetachedPlayer(): Player {
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
