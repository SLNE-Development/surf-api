package dev.slne.surf.api.velocity.util

import com.velocitypowered.api.proxy.Player
import dev.slne.surf.api.core.luckperms.LuckPermsAccess
import dev.slne.surf.api.core.luckperms.prefix
import dev.slne.surf.api.core.minimessage.miniMessage

fun Player.getLuckPermsUser() = LuckPermsAccess.getUser(this.uniqueId)
    ?: error("LuckPerms user not found for online player ${this.username}")

fun Player.getPrefixedName() =
    miniMessage.deserialize("${this.getLuckPermsUserOrNull()?.prefix ?: ""}${this.username}")

fun Player.getLuckPermsUserOrNull() = LuckPermsAccess.getUser(this.uniqueId)