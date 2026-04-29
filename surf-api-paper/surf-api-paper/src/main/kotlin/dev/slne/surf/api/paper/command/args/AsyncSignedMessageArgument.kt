package dev.slne.surf.api.paper.command.args

import com.mojang.brigadier.context.CommandContext
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.CommandAPIArgumentType
import dev.jorel.commandapi.executors.CommandArguments
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsCommandArgumentTypesBridge
import kotlinx.coroutines.Deferred
import net.kyori.adventure.chat.SignedMessage

/**
 * A CommandAPI argument that resolves a [SignedMessage] asynchronously via a [Deferred].
 *
 * This argument wraps the NMS-backed signed-message argument type. The resolved value should be
 * retrieved inside a suspend executor using `args.awaiting<SignedMessage>(nodeName)`, which
 * suspends the coroutine until the message is available without blocking the server thread.
 *
 * ### Usage
 * ```kotlin
 * asyncSignedMessageArgument("message")
 * playerExecutorSuspend { sender, args ->
 *     val message = args.awaiting<SignedMessage>("message")
 *     server.sendMessage(message, ChatType.CHAT.bind(sender.displayName()))
 * }
 * ```
 *
 * @param nodeName The name of the argument node used in the command tree and for retrieval via
 *   `args.awaiting<SignedMessage>(nodeName)`.
 */
@OptIn(NmsUseWithCaution::class)
class AsyncSignedMessageArgument(nodeName: String) : Argument<Deferred<SignedMessage>>(
    nodeName,
    SurfPaperNmsCommandArgumentTypesBridge::signedMessage,
) {
    override fun getPrimitiveType(): Class<Deferred<SignedMessage>> {
        return Deferred::class.java as Class<Deferred<SignedMessage>>
    }

    override fun getArgumentType(): CommandAPIArgumentType {
        return CommandAPIArgumentType.CHAT
    }

    override fun <Source> parseArgument(
        p0: CommandContext<Source>,
        p1: String,
        p2: CommandArguments
    ): Deferred<SignedMessage> {
        return SurfPaperNmsCommandArgumentTypesBridge.getSignedMessage(p0, p1)
    }
}


/**
 * Appends an [AsyncSignedMessageArgument] as a child of this [CommandTree].
 *
 * @param nodeName The name of the argument node.
 * @param optional Whether the argument is optional. Defaults to `false`.
 * @param block Optional configuration block applied to the newly created argument.
 * @return This [CommandTree] for chaining.
 */
inline fun CommandTree.asyncSignedMessageArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): CommandTree =
    then(AsyncSignedMessageArgument(nodeName).setOptional(optional).apply(block))

/**
 * Appends an [AsyncSignedMessageArgument] as a child of this [Argument].
 *
 * @param nodeName The name of the argument node.
 * @param optional Whether the argument is optional. Defaults to `false`.
 * @param block Optional configuration block applied to the newly created argument.
 * @return This [Argument] for chaining.
 */
inline fun Argument<*>.asyncSignedMessageArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): Argument<*> =
    then(AsyncSignedMessageArgument(nodeName).setOptional(optional).apply(block))


/**
 * Registers an [AsyncSignedMessageArgument] on this [CommandAPICommand].
 *
 * @param nodeName The name of the argument node.
 * @param optional Whether the argument is optional. Defaults to `false`.
 * @param block Optional configuration block applied to the newly created argument.
 * @return This [CommandAPICommand] for chaining.
 */
inline fun CommandAPICommand.asyncSignedMessageArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): CommandAPICommand =
    withArguments(AsyncSignedMessageArgument(nodeName).setOptional(optional).apply(block))