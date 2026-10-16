package dev.slne.surf.api.minestom.server.impl.command.brigadier

import dev.slne.surf.api.minestom.command.CommandDefinition
import dev.slne.surf.api.minestom.command.CommandPath
import dev.slne.surf.api.minestom.command.argument.ArgumentDefinition
import dev.slne.surf.api.minestom.command.argument.ArgumentKind
import dev.slne.surf.api.minestom.server.impl.command.MinestomConditions
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minestom.server.command.CommandSender
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket

/**
 * Merges the CommandAPI's commands into the [DeclareCommandsPacket] Minestom builds for a player.
 *
 * Paths and arguments the sender fails the permissions or requirements of are omitted, and a
 * command with no usable path is not declared at all. Holds no per-call state; safe to call from
 * any thread.
 */
internal class DeclareCommandsMerger(
    private val tree: BrigadierCommandTree,
    private val conditions: MinestomConditions = MinestomConditions(),
    private val declarations: NodeDeclarations = NodeDeclarations(),
) {
    /**
     * Returns a packet with a root literal for every label of every command [sender] can use,
     * appended to the nodes of [original].
     *
     * [original] is not modified and is returned as is when no command is registered. A root
     * literal of [original] that carries a CommandAPI label is removed so the merged tree is the
     * only declaration of that label.
     */
    fun merge(original: DeclareCommandsPacket, sender: CommandSender): DeclareCommandsPacket {
        val registered = tree.registered()
        if (registered.isEmpty()) return original

        val roots = registered.mapNotNull { (labels, definition) ->
            treeFor(definition, sender)?.let { branches -> labels to branches }
        }

        val nodes = original.nodes().mapTo(ObjectArrayList(), ::copyOf)
        val extraRootChildren = IntArrayList()

        // The removed nodes stay in the array, unreachable from the root.
        val owned = registered.flatMapTo(HashSet()) { (labels, _) -> labels }
        val originalRoot = nodes[original.rootIndex()]
        originalRoot.children = originalRoot.children
            .filterNot { child -> nodes[child].isLiteral() && nodes[child].name in owned }
            .toIntArray()

        roots.forEach { (labels, branches) ->
            labels.forEach { label ->
                val node = DeclareCommandsPacket.Node()
                node.flags = flagsOf(DeclareCommandsPacket.NodeType.LITERAL, branches.executable)
                node.name = label
                nodes += node

                val index = nodes.size - 1
                node.children = branches.children.values.map { child -> emit(child, nodes) }.toIntArray()
                extraRootChildren.add(index)
            }
        }

        val root = nodes[original.rootIndex()]
        root.children += extraRootChildren.toIntArray()

        return DeclareCommandsPacket(nodes, original.rootIndex())
    }

    private fun DeclareCommandsPacket.Node.isLiteral(): Boolean =
        flags.toInt() and NODE_TYPE_MASK == LITERAL_NODE_TYPE

    private fun emit(source: PendingNode, nodes: MutableList<DeclareCommandsPacket.Node>): Int {
        val node = DeclareCommandsPacket.Node()
        val definition = source.definition

        if (definition == null) {
            node.flags = flagsOf(DeclareCommandsPacket.NodeType.LITERAL, source.executable)
            node.name = checkNotNull(source.literal)
        } else {
            val declaration = declarations.of(definition)
            val suggestionsType = declaration.suggestionsType
            node.flags = flagsOf(
                DeclareCommandsPacket.NodeType.ARGUMENT,
                source.executable,
                suggestionsType != null,
            )
            node.name = definition.nodeName
            node.parser = declaration.parser
            node.properties = declaration.properties
            if (suggestionsType != null) node.suggestionsType = suggestionsType
        }

        nodes += node
        val index = nodes.size - 1
        node.children = source.children.values.map { child -> emit(child, nodes) }.toIntArray()
        return index
    }

    /**
     * The flags of a node: its type, its executable bit and whether it carries a suggestions type.
     * The packet writes a suggestions type only when its flag is set.
     */
    private fun flagsOf(
        type: DeclareCommandsPacket.NodeType,
        executable: Boolean,
        suggestionsType: Boolean = false,
    ): Byte = DeclareCommandsPacket.getFlag(type, executable, false, suggestionsType)

    private fun copyOf(source: DeclareCommandsPacket.Node): DeclareCommandsPacket.Node {
        val node = DeclareCommandsPacket.Node()
        node.flags = source.flags
        node.children = source.children.copyOf()
        node.redirectedNode = source.redirectedNode
        node.name = source.name
        node.parser = source.parser
        node.properties = source.properties
        node.suggestionsType = source.suggestionsType
        return node
    }

    /**
     * Folds the paths of [definition] that [sender] can use into one tree.
     *
     * Paths sharing a prefix share nodes, as Brigadier merges same-named children. A path is cut at
     * the first argument the sender may not use. Returns `null` when the sender can execute no path.
     */
    private fun treeFor(definition: CommandDefinition, sender: CommandSender): PendingNode? {
        val root = PendingNode(literal = null, definition = null)
        var any = false

        definition.paths.forEach { path ->
            if (!conditions.canUse(sender, path.permissions, path.requirements)) return@forEach

            val depths = executableDepths(path)
            var current = root
            if (0 in depths) {
                root.executable = true
                any = true
            }

            for ((index, argument) in path.arguments.withIndex()) {
                if (!conditions.canUse(sender, argument.permissions, argument.requirements)) return@forEach

                current = current.child(argument)
                if (index + 1 in depths) {
                    current.executable = true
                    any = true
                }
            }
        }

        return root.takeIf { any }
    }

    private fun executableDepths(path: CommandPath): IntRange {
        val firstOptional = path.arguments.indexOfFirst(ArgumentDefinition<*>::optional)
        return if (firstOptional == -1) {
            path.arguments.size..path.arguments.size
        } else {
            firstOptional..path.arguments.size
        }
    }

    private class PendingNode(val literal: String?, val definition: ArgumentDefinition<*>?) {
        var executable = false
        val children = LinkedHashMap<String, PendingNode>()

        fun child(argument: ArgumentDefinition<*>): PendingNode {
            val kind = argument.kind
            val literal = (kind as? ArgumentKind.Literal)?.literal
            val key = if (literal != null) "literal:$literal" else "argument:${argument.nodeName}"

            return children.getOrPut(key) {
                PendingNode(
                    literal = literal,
                    definition = if (literal == null) argument else null,
                )
            }
        }
    }

    private companion object {
        /**
         * The bits of a node's flags that hold its type: root, literal or argument.
         */
        const val NODE_TYPE_MASK = 0x03
        const val LITERAL_NODE_TYPE = 0x01
    }
}
