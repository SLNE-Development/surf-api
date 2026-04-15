package dev.slne.surf.api.velocity.util

import com.velocitypowered.api.proxy.Player
import dev.slne.surf.api.core.luckperms.LuckPermsAccess

fun Player.getLuckPermsUser() = LuckPermsAccess.getUser(this.uniqueId)
    ?: error("LuckPerms user not found for online player ${this.username}")

fun Player.getLuckPermsUserOrNull() = LuckPermsAccess.getUser(this.uniqueId)