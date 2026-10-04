package dev.slne.surf.api.minestom.server.impl.command

import com.mojang.brigadier.ParseResults
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContextBuilder
import com.mojang.brigadier.tree.ArgumentCommandNode
import dev.slne.surf.api.minestom.server.impl.command.brigadier.SignedMessageArgumentType
import dev.slne.surf.api.minestom.server.impl.command.brigadier.SuggestingArgumentType
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import net.kyori.adventure.chat.SignedMessage
import net.minestom.server.command.CommandSender
import net.minestom.server.crypto.ArgumentSignatures
import java.util.*

/**
 * The messages the sender of the command being dispatched signed, keyed by argument node name.
 *
 * Signed chat is an optional feature, so the messages are plain adventure [SignedMessage]s and
 * the feature installs the [unsignedFactory] that creates its own messages for unsigned input.
 */
@InternalSurfApi
object SignedCommandArguments {
    private val current = ThreadLocal<Map<String, SignedMessage>>()

    private val SYSTEM_FACTORY: (UUID?, String) -> SignedMessage = { _, content ->
        SignedMessage.system(content, null)
    }

    /**
     * Creates the message a signed message argument reads when its sender signed nothing.
     */
    @Volatile
    var unsignedFactory: (UUID?, String) -> SignedMessage = SYSTEM_FACTORY

    /** Runs [block] with [messages] visible to every signed message argument parsed within it. */
    fun <T> withMessages(messages: Map<String, SignedMessage>, block: () -> T): T {
        val previous = current.get()
        current.set(messages)

        try {
            return block()
        } finally {
            if (previous == null) current.remove() else current.set(previous)
        }
    }

    /** The message signed for the argument named [nodeName], or `null` when none was. */
    fun find(nodeName: String): SignedMessage? = current.get()?.get(nodeName)

    fun resetUnsignedFactory() {
        unsignedFactory = SYSTEM_FACTORY
    }
}

/**
 * The text a client signs for [input], keyed by the node name of the argument it is read from.
 */
@InternalSurfApi
fun signableCommandArguments(
    sender: CommandSender,
    input: String,
): Map<String, String> {
    val ownership = MinestomCommandAPIPlatform.activeOwnership() ?: return emptyMap()
    val dispatcher = MinestomCommandAPIPlatform.activeDispatcher() ?: return emptyMap()
    if (ownership.findInput(input) == null) return emptyMap()

    return signableArgumentsOf(dispatcher.parse(input.removePrefix("/"), sender))
}

/**
 * The signable arguments of [parse], keyed by node name, in the order they were read.
 */
internal fun signableArgumentsOf(
    parse: ParseResults<CommandSender>,
): Map<String, String> {
    val input = parse.reader.string
    val values = Object2ObjectLinkedOpenHashMap<String, String>()

    var context: CommandContextBuilder<CommandSender>? = parse.context
    while (context != null) {
        context.nodes.forEach { parsed ->
            val node = parsed.node
            if (node is ArgumentCommandNode<*, *> && node.type.isSignable()) {
                values[node.name] = input.substring(parsed.range.start, parsed.range.end)
            }
        }

        context = context.child
    }

    return values
}

/**
 * Whether [signatures] covers exactly the signable arguments in [values].
 */
@InternalSurfApi
fun signaturesCoverArguments(
    signatures: ArgumentSignatures,
    values: Map<String, String>,
): Boolean {
    val entries = signatures.entries()
    return entries.size == values.size && entries.all { entry -> entry.name() in values }
}

/** Whether a node of this type is one the client signs. */
private fun ArgumentType<*>.isSignable(): Boolean = when (this) {
    is SuggestingArgumentType -> delegate is SignedMessageArgumentType
    else -> this is SignedMessageArgumentType
}
