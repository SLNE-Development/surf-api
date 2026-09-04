/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command.commandapi

import dev.slne.surf.api.minestom.command.commandapi.argument.Argument
import dev.slne.surf.api.minestom.command.commandapi.exception.CommandValidationException
import dev.slne.surf.api.minestom.command.commandapi.executor.CommandExecutable
import dev.slne.surf.api.minestom.command.commandapi.executor.ExecutorDefinition
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import net.minestom.server.command.CommandSender
import org.jetbrains.annotations.ApiStatus

class CommandTree(val name: String) : CommandExecutable<CommandTree> {
    private val aliases = ObjectArrayList<String>()
    private val permissions = ObjectLinkedOpenHashSet<String>()
    private val requirements = ObjectArrayList<(CommandSender) -> Boolean>()
    private val arguments = ObjectArrayList<Argument<*>>()
    private val executors = ObjectArrayList<ExecutorDefinition>()

    private var shortDescription: String? = null
    private val fullDescription = ObjectArrayList<String>()
    private val usage = ObjectArrayList<String>()
    private var help: String? = null

    fun withAliases(vararg aliases: String): CommandTree = apply {
        this.aliases += aliases
    }

    fun withPermission(permission: String): CommandTree = apply {
        val normalized = permission.trim()
        if (normalized.isBlank()) {
            throw CommandValidationException("Command permission must not be blank")
        }
        permissions += normalized
    }

    fun withRequirement(requirement: (CommandSender) -> Boolean): CommandTree = apply {
        requirements += requirement
    }

    fun withShortDescription(description: String): CommandTree = apply {
        shortDescription = description
    }

    fun withFullDescription(vararg description: String): CommandTree = apply {
        fullDescription.clear()
        fullDescription += description
    }

    fun withUsage(vararg usage: String): CommandTree = apply {
        this.usage.clear()
        this.usage += usage
    }

    fun withHelp(help: String): CommandTree = apply {
        this.help = help
    }

    fun then(argument: Argument<*>): CommandTree = apply {
        arguments += argument
    }

    /**
     * Registers an executor for the tree root, which runs when the command is invoked with no
     * arguments. Root executors are independent of the branches added through [then]; a tree may
     * carry both.
     */
    override fun addExecutor(definition: ExecutorDefinition): CommandTree = apply {
        executors += definition
    }

    @ApiStatus.Internal
    fun toDefinition(): CommandDefinition {
        validateCommandLabels(name, aliases)

        val output = ObjectArrayList<CommandPath>()
        val rootPermissions = immutableSet(permissions)
        val rootRequirements = immutableList(requirements)

        if (executors.isNotEmpty()) {
            output += CommandPath(
                arguments = emptyList(),
                executors = immutableList(executors),
                permissions = rootPermissions,
                requirements = rootRequirements,
            )
        }

        arguments.forEach { argument ->
            flatten(
                prefix = emptyList(),
                inheritedPermissions = rootPermissions,
                inheritedRequirements = rootRequirements,
                node = argument.toTreeNode(),
                output = output,
            )
        }

        return CommandDefinition(
            name = name,
            aliases = immutableSet(aliases),
            metadata = CommandMetadata.snapshot(shortDescription, fullDescription, usage, help),
            paths = validateAndFreezePaths(output, name),
        )
    }

    fun register(namespace: String? = null): RegisteredCommand = CommandAPI.register(toDefinition(), namespace)
}
