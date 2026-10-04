package dev.slne.surf.api.minestom.permission

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.kyori.adventure.util.TriState
import net.minestom.server.command.CommandSender
import net.minestom.server.command.ConsoleSender
import net.minestom.server.command.ServerSender
import net.minestom.server.entity.Player

/**
 * Resolves whether a player holds a permission.
 *
 * Minestom has no permission system of its own, so the server installs one, e.g. through
 * `withLuckPerms()`.
 */
fun interface PermissionProvider {

    fun permissionValue(player: Player, permission: String): TriState
}

/**
 * The permission provider every surf feature asks, e.g. the CommandAPI for its requirements.
 *
 * Without an installed provider, players hold no permissions at all.
 */
object MinestomPermissions {

    private val NONE = PermissionProvider { _, _ -> TriState.NOT_SET }

    @Volatile
    var provider: PermissionProvider = NONE
        private set

    /** Installs [provider], or removes the installed one when `null`. */
    @InternalSurfApi
    fun installProvider(provider: PermissionProvider?) {
        this.provider = provider ?: NONE
    }

    fun permissionValue(sender: CommandSender, permission: String): TriState = when (sender) {
        is ConsoleSender, is ServerSender -> TriState.TRUE
        is Player -> provider.permissionValue(sender, permission)
        else -> TriState.NOT_SET
    }

    fun hasPermission(sender: CommandSender, permission: String): Boolean =
        permissionValue(sender, permission) == TriState.TRUE
}

/**
 * Whether this sender holds [permission].
 *
 * The console and the server sender hold every permission.
 */
fun CommandSender.hasPermission(permission: String): Boolean =
    MinestomPermissions.hasPermission(this, permission)

/**
 * The value of [permission] for this sender.
 */
fun CommandSender.permissionValue(permission: String): TriState =
    MinestomPermissions.permissionValue(this, permission)
