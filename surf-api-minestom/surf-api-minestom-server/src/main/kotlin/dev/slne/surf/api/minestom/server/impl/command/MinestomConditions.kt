package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.CommandPath
import dev.slne.surf.api.minestom.command.argument.ArgumentDefinition
import dev.slne.surf.api.minestom.permission.hasPermission
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.minestom.server.command.CommandSender
import net.minestom.server.command.ConsoleSender
import net.minestom.server.entity.Player
import net.minestom.server.command.builder.condition.CommandCondition

internal data class AccumulatedConditions(
    val permissions: Set<String> = emptySet(),
    val requirements: List<(CommandSender) -> Boolean> = emptyList(),
) {
    operator fun plus(other: AccumulatedConditions): AccumulatedConditions {
        val permissions = ObjectLinkedOpenHashSet<String>(
            this.permissions.size + other.permissions.size,
        )
        permissions.addAll(this.permissions)
        permissions.addAll(other.permissions)

        val requirements = ObjectArrayList<(CommandSender) -> Boolean>(
            this.requirements.size + other.requirements.size,
        )
        requirements.addAll(this.requirements)
        requirements.addAll(other.requirements)

        return AccumulatedConditions(
            permissions = permissions,
            requirements = requirements,
        )
    }

    fun excluding(other: AccumulatedConditions): AccumulatedConditions {
        val permissions = ObjectLinkedOpenHashSet(this.permissions)
        permissions.removeAll(other.permissions)

        val excludedRequirements = ReferenceOpenHashSet<(CommandSender) -> Boolean>(
            other.requirements.size,
        )
        excludedRequirements.addAll(other.requirements)

        val requirements = ObjectArrayList<(CommandSender) -> Boolean>(
            this.requirements.size,
        )

        this.requirements.forEach { requirement ->
            if (requirement !in excludedRequirements) {
                requirements += requirement
            }
        }

        return AccumulatedConditions(
            permissions = permissions,
            requirements = requirements,
        )
    }
}

internal class MinestomConditions {
    fun commonForPaths(
        paths: List<CommandPath>,
        inherited: AccumulatedConditions,
    ): AccumulatedConditions {
        val first = paths.firstOrNull() ?: return AccumulatedConditions()
        val permissions = ObjectLinkedOpenHashSet<String>(first.permissions.size)

        first.permissions.forEach { permission ->
            if (paths.all { path -> permission in path.permissions }) {
                permissions += permission
            }
        }

        val requirements = ObjectArrayList<(CommandSender) -> Boolean>(first.requirements.size)
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
        ).excluding(inherited)
    }

    fun forSyntax(
        path: CommandPath,
        presentDefinitions: List<ArgumentDefinition<*>>,
        inherited: AccumulatedConditions,
    ): CommandCondition? {
        val pathConditions = AccumulatedConditions(
            permissions = path.permissions,
            requirements = path.requirements,
        ).excluding(inherited)

        var permissionCapacity = 0
        var requirementCapacity = 0

        presentDefinitions.forEach { definition ->
            permissionCapacity += definition.permissions.size
            requirementCapacity += definition.requirements.size
        }

        val permissions = ObjectLinkedOpenHashSet<String>(permissionCapacity)
        val requirements = ObjectArrayList<(CommandSender) -> Boolean>(requirementCapacity)

        presentDefinitions.forEach { definition ->
            permissions.addAll(definition.permissions)
            requirements.addAll(definition.requirements)
        }

        val argumentConditions = AccumulatedConditions(
            permissions = permissions,
            requirements = requirements,
        )

        return asNative(pathConditions + argumentConditions)
    }

    fun asNative(conditions: AccumulatedConditions): CommandCondition? {
        if (conditions.permissions.isEmpty() && conditions.requirements.isEmpty()) return null
        return CommandCondition { sender, _ ->
            canUse(sender, conditions.permissions, conditions.requirements)
        }
    }

    fun canUse(
        sender: CommandSender,
        permissions: Set<String>,
        requirements: List<(CommandSender) -> Boolean>,
    ): Boolean {
        val permissionsGranted = when (sender) {
            is ConsoleSender -> true
            is Player -> permissions.all(sender::hasPermission)
            else -> permissions.isEmpty()
        }
        return permissionsGranted && requirements.all { requirement -> requirement(sender) }
    }
}
