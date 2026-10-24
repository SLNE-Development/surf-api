/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command

import dev.slne.surf.api.minestom.command.argument.Argument
import dev.slne.surf.api.minestom.command.exception.CommandValidationException
import dev.slne.surf.api.minestom.command.executor.CommandExecutable
import dev.slne.surf.api.minestom.command.executor.ExecutorDefinition
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import net.minestom.server.command.CommandSender

/**
 * Builder for a command whose argument structure branches.
 *
 * Each argument added through [then] starts a branch that can carry its own executors and nested
 * arguments. Permissions and requirements declared here apply to every branch. Invalid
 * configuration surfaces as [CommandValidationException] from [register].
 *
 * @property name label of the command; letters, digits, `.`, `_` and `-` only
 */
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

    /**
     * Requires [permission] to use any branch of the command.
     *
     * Multiple calls accumulate and every permission is required. The console always passes;
     * players need all permissions; other senders pass only while no permission is declared.
     *
     * @throws CommandValidationException when [permission] is blank
     */
    fun withPermission(permission: String): CommandTree = apply {
        val normalized = permission.trim()
        if (normalized.isBlank()) {
            throw CommandValidationException("Command permission must not be blank")
        }
        permissions += normalized
    }

    /**
     * Requires [requirement] to return `true` for the sender to use any branch of the command.
     * Multiple requirements accumulate and all must pass.
     */
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

    /**
     * Adds [argument], together with the branches nested below it, as a branch of the command.
     */
    fun then(argument: Argument<*>): CommandTree = apply {
        arguments += argument
    }

    /**
     * Adds an executor that runs when the command is invoked without arguments.
     */
    override fun addExecutor(definition: ExecutorDefinition): CommandTree = apply {
        executors += definition
    }

    @InternalSurfApi
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

    /**
     * Validates the command and registers it with the server.
     *
     * With a [namespace], the name and every alias are additionally registered as `namespace:label`.
     *
     * @throws CommandValidationException when the command or [namespace] is invalid
     * @throws IllegalStateException when a label is already taken or the platform is not installed
     */
    fun register(namespace: String? = null): RegisteredCommand = CommandAPI.register(toDefinition(), namespace)
}
