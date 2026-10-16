/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command

import dev.slne.surf.api.minestom.command.argument.*
import dev.slne.surf.api.minestom.command.exception.CommandValidationException
import dev.slne.surf.api.minestom.command.executor.CommandExecutable
import dev.slne.surf.api.minestom.command.executor.ExecutorDefinition
import dev.slne.surf.api.minestom.command.executor.ExecutorType
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import it.unimi.dsi.fastutil.objects.*
import net.minestom.server.command.CommandSender
import java.util.*

@InternalSurfApi
data class CommandDefinition(
    val name: String,
    val aliases: Set<String>,
    val metadata: CommandMetadata,
    val paths: List<CommandPath>,
)

@InternalSurfApi
data class CommandPath(
    val arguments: List<ArgumentDefinition<*>>,
    val executors: List<ExecutorDefinition>,
    val permissions: Set<String>,
    val requirements: List<(CommandSender) -> Boolean>,
)

/**
 * Builder for a command made of a linear argument chain and optional subcommands.
 *
 * Executors added through [CommandExecutable] run after the last argument of the chain, or on the
 * command itself when no arguments are declared. Use [CommandTree] for branching argument structures.
 *
 * Invalid configuration, such as a malformed name, a required argument after an optional one or a
 * non-terminal greedy argument, surfaces as [CommandValidationException] from [register].
 *
 * @property name label of the command; letters, digits, `.`, `_` and `-` only
 */
class CommandAPICommand(val name: String) : CommandExecutable<CommandAPICommand> {
    private val aliases = ObjectArrayList<String>()
    private val permissions = ObjectLinkedOpenHashSet<String>()
    private val requirements = ObjectArrayList<(CommandSender) -> Boolean>()
    private val arguments = ObjectArrayList<Argument<*>>()
    private val subcommands = ObjectArrayList<CommandAPICommand>()
    private val executors = ObjectArrayList<ExecutorDefinition>()

    private var shortDescription: String? = null
    private val fullDescription = ObjectArrayList<String>()
    private val usage = ObjectArrayList<String>()
    private var help: String? = null

    fun withAliases(vararg aliases: String): CommandAPICommand = apply {
        this.aliases += aliases
    }

    /**
     * Requires [permission] to use the command and all of its subcommands.
     *
     * Multiple calls accumulate and every permission is required. The console always passes;
     * players need all permissions; other senders pass only while no permission is declared.
     *
     * @throws CommandValidationException when [permission] is blank
     */
    fun withPermission(permission: String): CommandAPICommand = apply {
        val normalized = permission.trim()
        if (normalized.isBlank()) {
            throw CommandValidationException("Command permission must not be blank")
        }
        permissions += normalized
    }

    /**
     * Requires [requirement] to return `true` for the sender to use the command and its subcommands.
     * Multiple requirements accumulate and all must pass.
     */
    fun withRequirement(requirement: (CommandSender) -> Boolean): CommandAPICommand = apply {
        requirements += requirement
    }

    fun withArguments(vararg arguments: Argument<*>): CommandAPICommand = apply {
        this.arguments += arguments
    }

    /**
     * Appends [arguments] to the chain and marks each of them optional.
     */
    fun withOptionalArguments(vararg arguments: Argument<*>): CommandAPICommand = apply {
        arguments.forEach { argument -> argument.setOptional(true) }
        this.arguments += arguments
    }

    /**
     * Adds [subcommand], reachable by its name or aliases after this command's own arguments.
     *
     * @throws CommandValidationException when [subcommand] is this command; longer cycles are
     * rejected by [register]
     */
    fun withSubcommand(subcommand: CommandAPICommand): CommandAPICommand = apply {
        if (subcommand === this) {
            throw CommandValidationException("A command cannot be its own subcommand")
        }
        subcommands += subcommand
    }

    fun withSubcommands(vararg subcommands: CommandAPICommand): CommandAPICommand = apply {
        subcommands.forEach(::withSubcommand)
    }

    fun withShortDescription(description: String): CommandAPICommand = apply {
        shortDescription = description
    }

    fun withFullDescription(vararg description: String): CommandAPICommand = apply {
        fullDescription.clear()
        fullDescription += description
    }

    fun withUsage(vararg usage: String): CommandAPICommand = apply {
        this.usage.clear()
        this.usage += usage
    }

    fun withHelp(help: String): CommandAPICommand = apply {
        this.help = help
    }

    override fun addExecutor(definition: ExecutorDefinition): CommandAPICommand = apply {
        executors += definition
    }

    @InternalSurfApi
    fun toDefinition(): CommandDefinition {
        val stack = ReferenceOpenHashSet<CommandAPICommand>()

        validateCommandLabels(name, aliases)

        val output = ObjectArrayList<CommandPath>()
        val rootPermissions = immutableSet(permissions)
        val rootRequirements = immutableList(requirements)

        if (arguments.isEmpty && executors.isNotEmpty()) {
            output += commandPath(emptyList(), executors, rootPermissions, rootRequirements)
        }

        val subcommandNodes = ObjectArrayList<ArgumentTreeNode>()
        subcommands.forEach { subcommand ->
            subcommandNodes.addAll(subcommand.toSubcommandNodes(stack))
        }

        val rootNodes = if (arguments.isEmpty) {
            subcommandNodes
        } else {
            val argumentNodes = ObjectArrayList<ArgumentTreeNode>(arguments.size)
            arguments.forEach { argument ->
                argumentNodes += argument.toTreeNode()
            }

            linearNodes(argumentNodes, executors, subcommandNodes)
        }
        rootNodes.forEach { node ->
            flatten(
                emptyList(),
                rootPermissions,
                rootRequirements,
                node,
                output
            )
        }

        val paths = validateAndFreezePaths(output, name)
        return CommandDefinition(
            name = name,
            aliases = immutableSet(aliases),
            metadata = CommandMetadata.snapshot(shortDescription, fullDescription, usage, help),
            paths = paths,
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
    fun register(namespace: String? = null): RegisteredCommand =
        CommandAPI.register(toDefinition(), namespace)

    private fun toSubcommandNodes(
        stack: MutableSet<CommandAPICommand>,
    ): List<ArgumentTreeNode> {
        if (!stack.add(this)) {
            throw CommandValidationException("Command '$name' contains a subcommand cycle")
        }

        try {
            validateCommandLabels(name, aliases)

            val nestedSubcommands = ObjectArrayList<ArgumentTreeNode>()
            subcommands.forEach { subcommand ->
                nestedSubcommands.addAll(subcommand.toSubcommandNodes(stack))
            }

            val body = if (arguments.isEmpty) {
                emptyList()
            } else {
                val argumentNodes = ObjectArrayList<ArgumentTreeNode>(arguments.size)
                arguments.forEach { argument ->
                    argumentNodes += argument.toTreeNode()
                }

                linearNodes(argumentNodes, executors, nestedSubcommands)
            }

            val result = ObjectArrayList<ArgumentTreeNode>(aliases.size + 1)
            result += createSubcommandNode(
                label = name,
                nestedSubcommands = nestedSubcommands,
                body = body,
            )

            aliases.forEach { alias ->
                result += createSubcommandNode(
                    label = alias,
                    nestedSubcommands = nestedSubcommands,
                    body = body,
                )
            }

            return ObjectLists.unmodifiable(result)
        } finally {
            stack.remove(this)
        }
    }

    private fun createSubcommandNode(
        label: String,
        nestedSubcommands: List<ArgumentTreeNode>,
        body: List<ArgumentTreeNode>,
    ): ArgumentTreeNode = ArgumentTreeNode(
        argument = LiteralArgument(label).toDefinition(),
        executors = if (arguments.isEmpty) immutableList(executors) else emptyList(),
        children = if (arguments.isEmpty) nestedSubcommands else body,
        permissions = immutableSet(permissions),
        requirements = immutableList(requirements),
    )
}

internal fun validateCommandLabels(name: String, aliases: Collection<String>) {
    validateCommandLabel(name, "name")

    val normalized = ObjectOpenHashSet<String>(aliases.size + 1)
    normalized += name.lowercase(Locale.ROOT)

    aliases.forEach { alias ->
        validateCommandLabel(alias, "alias")

        if (!normalized.add(alias.lowercase(Locale.ROOT))) {
            throw CommandValidationException("Command name and aliases must be unique")
        }
    }
}

private fun validateCommandLabel(label: String, kind: String) {
    if (!COMMAND_NAME_PATTERN.matches(label)) {
        throw CommandValidationException(
            "Command $kind '$label' must contain only letters, digits, '.', '_' or '-'",
        )
    }
}

internal fun linearNodes(
    arguments: List<ArgumentTreeNode>,
    terminalExecutors: Collection<ExecutorDefinition>,
    terminalChildren: List<ArgumentTreeNode>,
): List<ArgumentTreeNode> {
    var suffix = terminalChildren
    for (index in arguments.indices.reversed()) {
        val node = arguments[index]
        val executors = if (index == arguments.lastIndex) {
            val combined = ObjectArrayList<ExecutorDefinition>(
                node.executors.size + terminalExecutors.size,
            )

            combined.addAll(node.executors)
            combined.addAll(terminalExecutors)

            immutableList(combined)
        } else {
            node.executors
        }

        val children = ObjectArrayList<ArgumentTreeNode>(
            node.children.size + suffix.size,
        )

        children.addAll(node.children)
        children.addAll(suffix)

        suffix = ObjectLists.singleton(
            node.copy(
                executors = executors,
                children = immutableList(children),
            ),
        )
    }
    return suffix
}

internal fun flatten(
    prefix: List<ArgumentDefinition<*>>,
    inheritedPermissions: Set<String>,
    inheritedRequirements: List<(CommandSender) -> Boolean>,
    node: ArgumentTreeNode,
    output: MutableList<CommandPath>,
) {
    val path = ObjectArrayList<ArgumentDefinition<*>>(prefix.size + 1)
    path.addAll(prefix)
    path += node.argument

    val permissions = ObjectLinkedOpenHashSet<String>(
        inheritedPermissions.size + node.permissions.size,
    )
    permissions.addAll(inheritedPermissions)
    permissions.addAll(node.permissions)

    val requirements = ObjectArrayList<(CommandSender) -> Boolean>(
        inheritedRequirements.size + node.requirements.size,
    )
    requirements.addAll(inheritedRequirements)
    requirements.addAll(node.requirements)

    val immutablePath = immutableList(path)
    val immutablePermissions = immutableSet(permissions)
    val immutableRequirements = immutableList(requirements)

    if (node.executors.isNotEmpty()) {
        output += commandPath(
            immutablePath,
            node.executors,
            immutablePermissions,
            immutableRequirements
        )
    }
    node.children.forEach { child ->
        flatten(
            immutablePath,
            immutablePermissions,
            immutableRequirements,
            child,
            output
        )
    }
}

internal fun validateAndFreezePaths(
    paths: List<CommandPath>,
    commandName: String
): List<CommandPath> {
    if (paths.isEmpty()) {
        throw CommandValidationException("Command '$commandName' has no executable paths")
    }

    paths.forEach { path ->
        val names = ObjectOpenHashSet<String>(path.arguments.size)
        path.arguments.forEach { argument ->
            if (argument.kind !is ArgumentKind.Literal && !names.add(argument.nodeName)) {
                throw CommandValidationException(
                    "Executable path contains duplicate argument node names",
                )
            }
        }

        val firstOptional = path.arguments.indexOfFirst { argument -> argument.optional }
        if (
            firstOptional >= 0 &&
            path.arguments
                .subList(firstOptional, path.arguments.size)
                .any { argument -> !argument.optional }
        ) {
            throw CommandValidationException("Required arguments cannot follow optional arguments")
        }
        val greedyIndex = path.arguments.indexOfFirst { argument -> argument.greedy }
        if (greedyIndex >= 0 && greedyIndex != path.arguments.lastIndex) {
            throw CommandValidationException("Greedy arguments must be terminal")
        }

        val executorTypes = ObjectOpenHashSet<ExecutorType>(path.executors.size)
        path.executors.forEach { executor ->
            if (!executorTypes.add(executor.type)) {
                throw CommandValidationException(
                    "Executable path contains equal-specificity executor conflicts",
                )
            }
        }
    }

    return immutableList(paths)
}

private fun commandPath(
    arguments: Collection<ArgumentDefinition<*>>,
    executors: Collection<ExecutorDefinition>,
    permissions: Collection<String>,
    requirements: Collection<(CommandSender) -> Boolean>,
): CommandPath = CommandPath(
    arguments = immutableList(arguments),
    executors = immutableList(executors),
    permissions = immutableSet(permissions),
    requirements = immutableList(requirements),
)

internal fun <T> immutableList(
    values: Collection<T>,
): List<T> = ObjectImmutableList(values)

internal fun <T> immutableSet(
    values: Collection<T>,
): Set<T> = ObjectSets.unmodifiable(
    ObjectLinkedOpenHashSet(values),
)

private val COMMAND_NAME_PATTERN = Regex("""[\p{L}\p{N}_.-]+""")
