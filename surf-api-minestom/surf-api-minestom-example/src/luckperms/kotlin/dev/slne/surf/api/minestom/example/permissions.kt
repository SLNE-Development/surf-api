package dev.slne.surf.api.minestom.example

import dev.slne.surf.api.minestom.server.SurfMinestomServerBuilder
import dev.slne.surf.api.minestom.server.luckperms.withLuckPerms

/** Resolves permissions with LuckPerms; grant them with `/lp user <name> permission set ...`. */
internal fun SurfMinestomServerBuilder.withExamplePermissions() {
    withLuckPerms()
}
