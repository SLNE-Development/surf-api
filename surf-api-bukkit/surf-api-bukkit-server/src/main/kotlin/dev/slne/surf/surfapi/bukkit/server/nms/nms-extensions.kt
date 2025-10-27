@file:Suppress("UnstableApiUsage")

package dev.slne.surf.surfapi.bukkit.server.nms

import dev.slne.surf.surfapi.bukkit.api.extensions.server
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.SignBlockUpdateSettings
import io.papermc.paper.advancement.AdvancementDisplay
import io.papermc.paper.advancement.AdvancementDisplay.Frame.*
import io.papermc.paper.adventure.PaperAdventure
import io.papermc.paper.math.BlockPosition
import io.papermc.paper.math.FinePosition
import io.papermc.paper.math.Position
import net.minecraft.advancements.AdvancementType
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Display
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.SignText
import net.minecraft.world.phys.Vec3
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.block.BlockState
import org.bukkit.block.data.BlockData
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.block.CraftBlockState
import org.bukkit.craftbukkit.block.data.CraftBlockData
import org.bukkit.craftbukkit.damage.CraftDamageSource
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftEntityType
import org.bukkit.craftbukkit.entity.CraftLivingEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.craftbukkit.inventory.CraftItemType
import org.bukkit.craftbukkit.util.Commodore
import org.bukkit.craftbukkit.util.CraftMagicNumbers
import org.bukkit.damage.DamageSource
import org.bukkit.entity.Display.Billboard
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import org.spongepowered.math.imaginary.Quaternionf
import org.spongepowered.math.vector.Vector3f
import net.kyori.adventure.text.Component as AdventureComponent
import net.minecraft.world.item.ItemStack as NmsItemStack
import net.minecraft.world.level.block.state.BlockState as NmsBlockState
import org.joml.Quaternionf as NmsQuaternionf
import org.joml.Vector3f as NmsVector3f

fun Player.toNms(): ServerPlayer = (this as CraftPlayer).handle
fun Material.toNmsBlock(): Block = CraftMagicNumbers.getBlock(this)
fun Material.toNmsItem(): Item = CraftMagicNumbers.getItem(this)
fun BlockData.toNms(): NmsBlockState = (this as CraftBlockData).state
fun Vector3f?.toNms(): NmsVector3f? =
    if (this == null) null else NmsVector3f(this.x(), this.y(), this.z())

fun FinePosition.toNms() = Vec3(x(), y(), z())
fun BlockState.toNms(): NmsBlockState = (this as CraftBlockState).handle
fun Quaternionf?.toNms(): NmsQuaternionf? =
    if (this == null) null else NmsQuaternionf(x(), y(), z(), w())

fun Billboard.toNms(): Display.BillboardConstraints =
    Display.BillboardConstraints.valueOf(this.name)

fun ItemStack.toNms(): NmsItemStack = CraftItemStack.asNMSCopy(this)
fun ItemDisplayTransform.toNms(): ItemDisplayContext = ItemDisplayContext.BY_ID.apply(this.ordinal)
fun NmsItemStack.toBukkit(): ItemStack = CraftItemStack.asBukkitCopy(this)
val ItemType.nms: Item get() = CraftItemType.bukkitToMinecraftNew(this)
fun BlockPosition.toNms(): BlockPos = BlockPos(blockX(), blockY(), blockZ())
fun SignBlockUpdateSettings.SignText.toNms(): SignText {
    val lines = arrayOf(
        line1.toNms(),
        line2.toNms(),
        line3.toNms(),
        line4.toNms()
    )

    return SignText(lines, lines, DyeColor.BLACK, false)
}

fun InventoryType.toNms(): MenuType<*> = when (this) {
    InventoryType.ANVIL -> MenuType.ANVIL
    InventoryType.BEACON -> MenuType.BEACON
    InventoryType.BLAST_FURNACE -> MenuType.BLAST_FURNACE
    InventoryType.BREWING -> MenuType.BREWING_STAND
    InventoryType.CARTOGRAPHY -> MenuType.CARTOGRAPHY_TABLE
    InventoryType.CHEST -> MenuType.GENERIC_9x6
    InventoryType.DISPENSER, InventoryType.DROPPER -> MenuType.GENERIC_3x3
    InventoryType.ENCHANTING -> MenuType.ENCHANTMENT
    InventoryType.FURNACE -> MenuType.FURNACE
    InventoryType.GRINDSTONE -> MenuType.GRINDSTONE
    InventoryType.HOPPER -> MenuType.HOPPER
    InventoryType.LECTERN -> MenuType.LECTERN
    InventoryType.LOOM -> MenuType.LOOM
    InventoryType.MERCHANT -> MenuType.MERCHANT
    InventoryType.SHULKER_BOX -> MenuType.SHULKER_BOX
    InventoryType.SMOKER -> MenuType.SMOKER
    InventoryType.SMITHING -> MenuType.SMITHING
    InventoryType.STONECUTTER -> MenuType.STONECUTTER
    InventoryType.PLAYER -> MenuType.GENERIC_9x4
    InventoryType.CRAFTER -> MenuType.CRAFTER_3x3
    InventoryType.WORKBENCH -> MenuType.CRAFTING
    InventoryType.BARREL, InventoryType.CHISELED_BOOKSHELF, InventoryType.DECORATED_POT, InventoryType.JUKEBOX, InventoryType.COMPOSTER, InventoryType.ENDER_CHEST -> MenuType.GENERIC_9x3
    InventoryType.CREATIVE, InventoryType.CRAFTING -> throw UnsupportedOperationException("Can't open a $this inventory!")
    else -> throw UnsupportedOperationException("Unknown inventory type: $this")
}

fun BlockPos.toBukkit(): BlockPosition = Position.block(x, y, z)

fun Array<Component>.toBukkit() = this.map { it.toBukkit() }
fun AdventureComponent.toNms(): Component = PaperAdventure.asVanilla(this)
fun Component.toBukkit(): AdventureComponent = PaperAdventure.asAdventure(this)

fun Server.toCraft() = this as CraftServer
val craftServer: CraftServer get() = server.toCraft()
val commodore: Commodore get() = CraftMagicNumbers.INSTANCE.commodore
val dedicatedServer: DedicatedServer get() = MinecraftServer.getServer() as DedicatedServer

fun EntityType.toNms(): net.minecraft.world.entity.EntityType<*> = CraftEntityType.bukkitToMinecraft(this)
fun World.toNms(): ServerLevel = (this as CraftWorld).handle
fun Entity.toNms(): net.minecraft.world.entity.Entity = (this as CraftEntity).handle
fun LivingEntity.toNms(): net.minecraft.world.entity.LivingEntity = (this as CraftLivingEntity).handle
fun DamageSource.toNms(): net.minecraft.world.damagesource.DamageSource = (this as CraftDamageSource).handle

fun AdvancementDisplay.Frame.toNms() = when (this) {
    CHALLENGE -> AdvancementType.CHALLENGE
    GOAL -> AdvancementType.GOAL
    TASK -> AdvancementType.TASK
}