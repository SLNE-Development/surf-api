@file:Suppress("UnstableApiUsage")

package dev.slne.surf.surfapi.bukkit.server.impl.nms

import net.minecraft.world.item.Item
import org.bukkit.craftbukkit.inventory.CraftItemType
import org.bukkit.inventory.ItemType

val ItemType.nms: Item get() = CraftItemType.bukkitToMinecraftNew(this)