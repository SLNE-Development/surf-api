package dev.slne.minestom.example

import dev.slne.surf.api.minestom.permission.PermissionProvider
import dev.slne.surf.api.minestom.server.SurfMinestomServerBuilder
import net.kyori.adventure.util.TriState

/**
 * vendor/LuckPerms has not been generated, so every player is granted every permission instead.
 */
internal fun SurfMinestomServerBuilder.withExamplePermissions() {
    permissions(PermissionProvider { _, _ -> TriState.TRUE })
}
