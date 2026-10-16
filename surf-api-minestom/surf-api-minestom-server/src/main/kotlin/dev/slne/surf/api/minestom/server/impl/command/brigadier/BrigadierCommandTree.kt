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
import dev.slne.surf.api.minestom.command.executor.*
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
 * The dispatcher is rebuilt from the registered [CommandDefinition]s whenever the set of commands
 * changes, because Brigadier cannot detach a node from a dispatcher. [register], [unregister] and
 * [clear] are not thread-safe and must be serialized by the caller; [dispatcher] and [registered]
 * can be read from any thread.
 */
internal class BrigadierCommandTree(
    private val conditions: MinestomConditions = MinestomConditions(),
    private val suggestionScope: () -> CoroutineScope = { minestomAsyncScope },
) {
    private data class Entry(val labels: List<String>, val definition: CommandDefinition)

    private val entries = Object2ObjectLinkedOpenHashMap<String, Entry>()

    /**
     * The current dispatcher, replaced as a whole on every change.
     *
     * A reference obtained earlier keeps the command set it was built from.
     */
    @Volatile
    var dispatcher = CommandDispatcher<CommandSender>()
        private set

    @Volatile
    private var registeredSnapshot: List<Pair<List<String>, CommandDefinition>> = emptyList()

    /**
     * Registers [definition] under [labels], replacing any command previously registered with the
     * same [key].
     */
    fun register(key: String, labels: Collection<String>, definition: CommandDefinition) {
        entries[key] = Entry(ObjectArrayList(labels), definition)
        rebuild()
    }

    /**
     * Removes the command registered under [key]; unknown keys are ignored.
     */
    fun unregister(key: String) {
        if (entries.remove(key) != null) rebuild()
    }

    /**
     * Removes every command.
     */
    fun clear() {
        entries.clear()
        rebuild()
    }

    /**
     * Every registered command, paired with the labels it answers to.
     */
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

        // Conditions shared by all paths gate the root, hiding the command from senders who can
        // use none of them.
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
     * The argument counts at which [path] can execute: its full length, or every length from the
     * first optional argument onwards.
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
     * Builds the node for [definition]: a literal node for a literal kind, otherwise an argument
     * node typed by the definition's raw type.
     *
     * Signed message kinds use [SignedMessageArgumentType]. A custom suggestion provider wraps the
     * type in [SuggestingArgumentType].
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

    /**
     * The command executed when [path] is run with [depth] arguments.
     *
     * Returns `0` when the sender fails the path's conditions, no executor matches its type or the
     * executor throws; the last two cases are reported to the sender. A suspending executor is
     * launched and reported as successful before it completes.
     */
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
     * Collects the non-literal arguments of [path] for a command executed with [depth] arguments.
     *
     * An argument at or beyond [depth] is marked absent and takes its declared default value.
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

    /**
     * The executor for the type of [sender]: its own type first, then [ExecutorType.ANY].
     *
     * Yields `null` when neither matches exactly one executor.
     */
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
