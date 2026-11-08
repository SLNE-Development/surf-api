package dev.slne.surf.api.paper.permission

import dev.slne.surf.api.paper.extensions.pluginManager
import org.bukkit.permissions.Permission

abstract class PermissionRegistry {
    private val permissions = mutableMapOf<String, Permission>()

    fun create(permission: String): String {
        val bukkitPermission = Permission(permission)
        pluginManager.addPermission(bukkitPermission)

        permissions[permission] = bukkitPermission
        return permission
    }
}