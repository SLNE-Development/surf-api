package dev.slne.surf.api.minestom.command.argument

import com.mojang.brigadier.arguments.ArgumentType
import net.kyori.adventure.chat.SignedMessage

/**
 * The remainder of the command line as a [SignedMessage].
 *
 * The message carries the client's signature when one was supplied for this argument; otherwise an
 * unsigned message is created from the text. The argument is greedy and must be the last argument
 * of its path.
 */
class SignedMessageArgument(nodeName: String) : Argument<SignedMessage>(nodeName) {
    override val kind = ArgumentKind.SignedMessage
    override val rawType: ArgumentType<SignedMessage> = UnsupportedArgumentType(nodeName)
    override val greedy = true

    override fun stringify(value: SignedMessage): String = value.message()
}
