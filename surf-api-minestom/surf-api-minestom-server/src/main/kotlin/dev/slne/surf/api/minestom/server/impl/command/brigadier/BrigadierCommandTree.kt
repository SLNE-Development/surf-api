package dev.slne.surf.api.minestom.server.impl.command.brigadier

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.ArgumentCommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
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
 * The argument counts at which this path can execute: its full length, or every length from the
 * first optional argument onwards.
 *
 * The Brigadier tree and the tree declared to the client both derive their executable nodes from
 * it and must agree.
 */
internal fun CommandPath.executableDepths(): IntRange {
    val firstOptional = arguments.indexOfFirst(ArgumentDefinition<*>::optional)
    return if (firstOptional == -1) {
        arguments.size..arguments.size
    } else {
        firstOptional..arguments.size
    }
}

/**
 * The Brigadier command tree that owns parsing and dispatch for every registered command.
 *
 * The dispatcher is rebuilt from the registered commands on the first read after the set of
 * commands changed, because Brigadier cannot detach a node from a dispatcher. The nodes of a
 * command are built at registration, so a definition that cannot be built fails there. [register]
 * and [unregister] are not thread-safe and must be serialized by the caller; [dispatcher] and
 * [registered] can be read from any thread.
 */
internal class BrigadierCommandTree(
    private val conditions: MinestomConditions = MinestomConditions(),
    private val suggestionScope: () -> CoroutineScope = { minestomAsyncScope },
) {
    private class Entry(
        val labels: List<String>,
        val definition: CommandDefinition,
        val nodes: List<LiteralCommandNode<CommandSender>>,
    )

    private class Snapshot(
        val dispatcher: CommandDispatcher<CommandSender>,
        val registered: List<Pair<List<String>, CommandDefinition>>,
    )

    private val entries = Object2ObjectLinkedOpenHashMap<String, Entry>()

    /**
     * The snapshot of the current command set, or `null` when a change has not been built yet.
     */
    @Volatile
    private var snapshot: Snapshot? = null

    /**
     * The current dispatcher, replaced as a whole after every change.
     *
     * A reference obtained earlier keeps the command set it was built from.
     */
    val dispatcher: CommandDispatcher<CommandSender>
        get() = current().dispatcher

    /**
     * Registers [definition] under [labels], replacing any command previously registered with the
     * same [key].
     */
    fun register(key: String, labels: Collection<String>, definition: CommandDefinition) {
        val labelList = ObjectArrayList(labels)
        val nodes = labelList.map { label -> literalFor(label, definition).build() }

        synchronized(entries) {
            entries[key] = Entry(labelList, definition, nodes)
            snapshot = null
        }
    }

    /**
     * Removes the command registered under [key]; unknown keys are ignored.
     */
    fun unregister(key: String) {
        synchronized(entries) {
            if (entries.remove(key) != null) snapshot = null
        }
    }

    /**
     * Every registered command, paired with the labels it answers to.
     */
    fun registered(): List<Pair<List<String>, CommandDefinition>> = current().registered

    private fun current(): Snapshot = snapshot ?: synchronized(entries) {
        snapshot ?: build().also { built -> snapshot = built }
    }

    private fun build(): Snapshot {
        val dispatcher = CommandDispatcher<CommandSender>()
        entries.values.forEach { entry ->
            entry.nodes.forEach { node -> dispatcher.root.addChild(node) }
        }

        return Snapshot(
            dispatcher = dispatcher,
            registered = entries.values.map { entry -> entry.labels to entry.definition },
        )
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
            val executableDepths = path.executableDepths()
            if (0 in executableDepths) {
                root.executes(executor(path, depth = 0))
            }

            val chain = chainFor(path, executableDepths)
            if (chain != null) root.then(chain)
        }

        return root
    }

    private fun sharedConditions(definition: CommandDefinition): AccumulatedConditions =
        conditions.commonForPaths(definition.paths)

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
     * Returns `0` when the sender fails the path's conditions, no executor matches its type, or the
     * arguments cannot be resolved or the executor throws; the last three cases are reported to the
     * sender. A suspending executor is
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

        try {
            val arguments = argumentsOf(context, path, depth)
            val info = ExecutionInfo(sender, arguments, context.input)

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
