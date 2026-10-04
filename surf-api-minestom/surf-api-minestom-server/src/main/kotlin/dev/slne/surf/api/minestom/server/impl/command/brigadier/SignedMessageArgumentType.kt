package dev.slne.surf.api.minestom.server.impl.command.brigadier

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import dev.slne.surf.api.minestom.server.impl.command.SignedCommandArguments
import net.kyori.adventure.chat.SignedMessage
import net.minestom.server.command.CommandSender
import net.minestom.server.entity.Player

/**
 * Reads the rest of the command line and pairs it with the signature its sender produced for it.
 */
internal class SignedMessageArgumentType(
    private val nodeName: String,
) : ArgumentType<SignedMessage> {

    override fun parse(reader: StringReader): SignedMessage = read(reader, sender = null)

    override fun <S> parse(reader: StringReader, source: S): SignedMessage =
        read(reader, source as? CommandSender)

    private fun read(reader: StringReader, sender: CommandSender?): SignedMessage {
        val content = reader.remaining
        reader.cursor = reader.totalLength

        return SignedCommandArguments.find(nodeName)
            ?: SignedCommandArguments.unsignedFactory((sender as? Player)?.uuid, content)
    }
}
