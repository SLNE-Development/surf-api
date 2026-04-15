@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.server.nms

import dev.slne.surf.api.paper.extensions.server
import net.minecraft.server.level.ServerPlayer
import org.bukkit.Server
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.util.Commodore
import org.bukkit.craftbukkit.util.CraftMagicNumbers
import org.bukkit.entity.Player

fun Player.toNms(): ServerPlayer = (this as CraftPlayer).handle

fun Server.toCraft() = this as CraftServer
val craftServer: CraftServer get() = server.toCraft()
val commodore: Commodore get() = CraftMagicNumbers.INSTANCE.commodore
