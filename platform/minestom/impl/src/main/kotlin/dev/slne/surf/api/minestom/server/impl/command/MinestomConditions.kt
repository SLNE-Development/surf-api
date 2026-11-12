package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.CommandPath
import dev.slne.surf.api.minestom.permission.hasPermission
import net.minestom.server.command.CommandSender

internal data class AccumulatedConditions(
    val permissions: Set<String> = emptySet(),
    val requirements: List<(CommandSender) -> Boolean> = emptyList(),
)

internal class MinestomConditions {
    fun commonForPaths(paths: List<CommandPath>): AccumulatedConditions {
        val first = paths.firstOrNull() ?: return AccumulatedConditions()
        val permissions = LinkedHashSet<String>(first.permissions.size)

        first.permissions.forEach { permission ->
            if (paths.all { path -> permission in path.permissions }) {
                permissions += permission
            }
        }

        val requirements = ArrayList<(CommandSender) -> Boolean>(first.requirements.size)
        first.requirements.forEach { requirement ->
            if (
                paths.all { path ->
                    path.requirements.any { candidate ->
                        candidate === requirement
                    }
                }
            ) {
                requirements += requirement
            }
        }

        return AccumulatedConditions(
            permissions = permissions,
            requirements = requirements,
        )
    }

    fun canUse(
        sender: CommandSender,
        permissions: Set<String>,
        requirements: List<(CommandSender) -> Boolean>,
    ): Boolean {
        return permissions.all(sender::hasPermission) &&
                requirements.all { requirement -> requirement(sender) }
    }
}
