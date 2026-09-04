package dev.slne.surf.api.minestom.command.commandapi.argument

import com.mojang.brigadier.arguments.ArgumentType
import net.kyori.adventure.chat.SignedMessage

/**
 * Reads the rest of the command line as a chat message the sender signed.
 */
class SignedMessageArgument(nodeName: String) : Argument<SignedMessage>(nodeName) {
    override val kind = ArgumentKind.SignedMessage
    override val rawType: ArgumentType<SignedMessage> = UnsupportedArgumentType(nodeName)
    override val greedy = true

    override fun stringify(value: SignedMessage): String = value.message()
}
