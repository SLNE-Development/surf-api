package dev.slne.surf.api.minestom.server.impl.command.brigadier

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.ArgumentCommandNode
import dev.slne.surf.api.minestom.command.CommandDefinition
import dev.slne.surf.api.minestom.command.CommandPath
import dev.slne.surf.api.minestom.command.argument.ArgumentDefinition
import dev.slne.surf.api.minestom.command.argument.ArgumentKind
import dev.slne.surf.api.minestom.command.executor.CommandArguments
import dev.slne.surf.api.minestom.command.executor.ExecutionInfo
import dev.slne.surf.api.minestom.command.executor.ExecutorDefinition
import dev.slne.surf.api.minestom.command.executor.ExecutorType
import dev.slne.surf.api.minestom.command.executor.ParsedArgument
import dev.slne.surf.api.minestom.command.executor.handleCommandExecutorFailure
import dev.slne.surf.api.minestom.command.executor.launchCommandExecutor
import dev.slne.surf.api.minestom.coroutine.minestomAsyncScope
import dev.slne.surf.api.minestom.server.impl.command.AccumulatedConditions
import dev.slne.surf.api.minestom.server.impl.command.MinestomConditions
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.CommandSender
import net.minestom.server.command.ConsoleSender
import net.minestom.server.entity.Player

private val NO_COMPATIBLE_EXECUTOR: Component = Component.text(
    "Dieser Befehl kann von diesem Absendertyp nicht ausgeführt werden.",
    NamedTextColor.RED,
)

/**
 * The Brigadier command tree that owns parsing and dispatch for every registered command.
 *
 * Nodes are rebuilt from the registered [CommandDefinition]s whenever the set of commands changes,
 * because Brigadier offers no way to detach a node from a dispatcher.
 */
internal class BrigadierCommandTree(
    private val conditions: MinestomConditions = MinestomConditions(),
    private val suggestionScope: () -> CoroutineScope = { minestomAsyncScope },
) {
    private data class Entry(val labels: List<String>, val definition: CommandDefinition)

    private val entries = Object2ObjectLinkedOpenHashMap<String, Entry>()

    @Volatile
    var dispatcher = CommandDispatcher<CommandSender>()
        private set

    @Volatile
    private var registeredSnapshot: List<Pair<List<String>, CommandDefinition>> = emptyList()

    fun register(key: String, labels: Collection<String>, definition: CommandDefinition) {
        entries[key] = Entry(ObjectArrayList(labels), definition)
        rebuild()
    }

    fun unregister(key: String) {
        if (entries.remove(key) != null) rebuild()
    }

    fun clear() {
        entries.clear()
        rebuild()
    }

    /** Every registered command, paired with the labels it answers to. */
    fun registered(): List<Pair<List<String>, CommandDefinition>> = registeredSnapshot

    private fun rebuild() {
        val rebuilt = CommandDispatcher<CommandSender>()
        entries.values.forEach { entry ->
            entry.labels.forEach { label ->
                rebuilt.register(literalFor(label, entry.definition))
            }
        }

        registeredSnapshot = entries.values.map { entry -> entry.labels to entry.definition }
        dispatcher = rebuilt
    }

    private fun literalFor(
        label: String,
        definition: CommandDefinition,
    ): LiteralArgumentBuilder<CommandSender> {
        val root = LiteralArgumentBuilder.literal<CommandSender>(label)

        // Requirements every path agrees on gate the root, so a sender who can use none of them
        // never sees the command at all rather than reaching it and being turned away
        val shared = sharedConditions(definition)
        if (shared.permissions.isNotEmpty() || shared.requirements.isNotEmpty()) {
            root.requires { sender ->
                conditions.canUse(sender, shared.permissions, shared.requirements)
            }
        }

        definition.paths.forEach { path ->
            val executableDepths = executableDepths(path)
            if (0 in executableDepths) {
                root.executes(executor(path, depth = 0))
            }

            val chain = chainFor(path, executableDepths)
            if (chain != null) root.then(chain)
        }

        return root
    }

    private fun sharedConditions(definition: CommandDefinition): AccumulatedConditions =
        conditions.commonForPaths(definition.paths, AccumulatedConditions())

    /**
     * Returns the argument counts at which [path] is executable.
     *
     * An optional suffix makes every length from the first optional argument onwards executable,
     * which is how a single declared path yields several usable syntaxes.
     */
    private fun executableDepths(path: CommandPath): IntRange {
        val firstOptional = path.arguments.indexOfFirst(ArgumentDefinition<*>::optional)
        return if (firstOptional == -1) {
            path.arguments.size..path.arguments.size
        } else {
            firstOptional..path.arguments.size
        }
    }

    private fun chainFor(
        path: CommandPath,
        executableDepths: IntRange,
    ): ArgumentBuilder<CommandSender, *>? {
        var current: ArgumentBuilder<CommandSender, *>? = null

        for (index in path.arguments.indices.reversed()) {
            val definition = path.arguments[index]
            val builder = nodeFor(definition)

            if (index + 1 in executableDepths) {
                builder.executes(executor(path, depth = index + 1))
            }
            current?.let(builder::then)

            val permissions = definition.permissions
            val requirements = definition.requirements
            if (permissions.isNotEmpty() || requirements.isNotEmpty()) {
                builder.requires { sender -> conditions.canUse(sender, permissions, requirements) }
            }

            current = builder
        }

        return current
    }

    /**
     * Builds the node for [definition].
     *
     * A literal becomes a Brigadier literal node rather than an argument, which is why a literal and
     * an argument may share a name on the same path. Everything else becomes an argument node driven
     * by the definition's own raw type.
     */
    private fun nodeFor(definition: ArgumentDefinition<*>): ArgumentBuilder<CommandSender, *> {
        val kind = definition.kind
        if (kind is ArgumentKind.Literal) return LiteralArgumentBuilder.literal(kind.literal)

        @Suppress("UNCHECKED_CAST")
        val rawType = if (kind is ArgumentKind.SignedMessage) {
            SignedMessageArgumentType(definition.nodeName) as ArgumentType<Any>
        } else {
            definition.rawType as ArgumentType<Any>
        }
        val type = if (definition.hasCustomSuggestions()) {
            SuggestingArgumentType(rawType, definition, suggestionScope)
        } else {
            rawType
        }

        return RequiredArgumentBuilder.argument(definition.nodeName, type)
    }

    private fun executor(path: CommandPath, depth: Int) = Command { context ->
        val sender = context.source
        if (!conditions.canUse(sender, path.permissions, path.requirements)) {
            return@Command 0
        }

        val selected = selectExecutor(sender, path.executors)
            ?: run {
                sender.sendMessage(NO_COMPATIBLE_EXECUTOR)
                return@Command 0
            }

        val arguments = argumentsOf(context, path, depth)
        val info = ExecutionInfo(sender, arguments, context.input)

        try {
            when (selected) {
                is ExecutorDefinition.Normal -> {
                    selected.executor(info)
                    Command.SINGLE_SUCCESS
                }

                is ExecutorDefinition.Resulting -> selected.executor(info)

                is ExecutorDefinition.Suspending -> {
                    launchCommandExecutor(selected, sender, arguments, context.input)
                    Command.SINGLE_SUCCESS
                }
            }
        } catch (failure: Throwable) {
            handleCommandExecutorFailure(sender, context.input, failure)
            0
        }
    }

    /**
     * Snapshots the value-carrying arguments of [path] that a command at [depth] can see.
     *
     * A literal occupies no argument name and is left out. Arguments beyond [depth] were not typed,
     * so they fall back to their declared default.
     */
    private fun argumentsOf(
        context: CommandContext<CommandSender>,
        path: CommandPath,
        depth: Int,
    ): CommandArguments {
        val parsedNames = ObjectArrayList<String>(context.nodes.size)
        context.nodes.forEach { parsed ->
            val node = parsed.node
            if (node is ArgumentCommandNode<*, *>) parsedNames += node.name
        }

        val valued = path.arguments.filter { argument -> argument.kind !is ArgumentKind.Literal }
        return CommandArguments.of(
            valued.mapTo(ObjectArrayList(valued.size)) { definition ->
                val name = definition.nodeName
                val index = path.arguments.indexOf(definition)
                val present = index < depth && name in parsedNames

                ParsedArgument(
                    name = name,
                    value = if (present) {
                        context.getArgument(name, Any::class.java)
                    } else {
                        definition.defaultValue?.invoke(context.source)
                    },
                    raw = if (present) rawOf(context, name) else null,
                    present = present,
                )
            },
        )
    }

    private fun rawOf(context: CommandContext<CommandSender>, name: String): String? {
        context.nodes.forEach { parsed ->
            val node = parsed.node
            if (node is ArgumentCommandNode<*, *> && node.name == name) {
                return context.input.substring(parsed.range.start, parsed.range.end)
            }
        }
        return null
    }

    private fun selectExecutor(
        sender: CommandSender,
        executors: List<ExecutorDefinition>,
    ): ExecutorDefinition? {
        val preferred = when (sender) {
            is Player -> PreferredExecutors.player
            is ConsoleSender -> PreferredExecutors.console
            else -> PreferredExecutors.any
        }
        return preferred.firstNotNullOfOrNull { type ->
            executors.singleOrNull { executor -> executor.type == type }
        }
    }

    private object PreferredExecutors {
        val player = listOf(ExecutorType.PLAYER, ExecutorType.ANY)
        val console = listOf(ExecutorType.CONSOLE, ExecutorType.ANY)
        val any = listOf(ExecutorType.ANY)
    }
}
