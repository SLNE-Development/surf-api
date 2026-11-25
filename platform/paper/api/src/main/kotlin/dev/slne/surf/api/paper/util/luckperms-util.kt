@file:JvmName("UtilLuckPerms")

package dev.slne.surf.api.paper.util

import dev.slne.surf.api.luckperms.SurfLuckPerms
import dev.slne.surf.api.luckperms.getLuckPermsUser
import dev.slne.surf.api.luckperms.getLuckPermsUserOrNull
import dev.slne.surf.api.luckperms.loadLuckPermsUser
import dev.slne.surf.api.messages.minimessage.miniMessage
import net.kyori.adventure.text.Component
import net.luckperms.api.model.user.User
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

/**
 * Returns the LuckPerms user of this online player.
 *
 * @return the loaded user
 * @throws IllegalStateException if LuckPerms is not running or has not loaded the user
 */
fun Player.getLuckPermsUser(): User = uniqueId.getLuckPermsUser()

/**
 * Returns the LuckPerms user of this player if LuckPerms holds them in memory.
 *
 * @return the loaded user, or `null` if LuckPerms has not loaded them
 * @throws IllegalStateException if LuckPerms is not running
 */
fun Player.getLuckPermsUserOrNull(): User? = uniqueId.getLuckPermsUserOrNull()

/**
 * Returns the LuckPerms user of this player from memory, or loads them from storage when the
 * player is offline.
 *
 * @return the user
 * @throws IllegalStateException if LuckPerms is not running
 */
suspend fun OfflinePlayer.loadLuckPermsUser(): User = uniqueId.loadLuckPermsUser()

/**
 * Renders the player's LuckPerms prefix, parsed as MiniMessage, followed by their name.
 *
 * @return the prefixed name; only the name if the player has no prefix or is not loaded
 * @throws IllegalStateException if LuckPerms is not running
 */
fun Player.getPrefixedName(): Component {
    return miniMessage.deserialize(SurfLuckPerms.instance.prefix(uniqueId) + name)
}
