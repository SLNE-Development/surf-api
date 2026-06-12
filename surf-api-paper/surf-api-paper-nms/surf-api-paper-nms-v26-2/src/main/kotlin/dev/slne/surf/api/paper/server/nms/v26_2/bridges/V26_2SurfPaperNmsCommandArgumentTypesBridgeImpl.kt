package dev.slne.surf.api.paper.server.nms.v26_2.bridges

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsCommandArgumentTypesBridge
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.AdventureNBT
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.CompoundTagArgument
import net.minecraft.commands.arguments.MessageArgument

@NmsUseWithCaution
@Suppress("ClassName")
class V26_2SurfPaperNmsCommandArgumentTypesBridgeImpl : SurfPaperNmsCommandArgumentTypesBridge {

    override fun compoundTag(): ArgumentType<*> {
        return CompoundTagArgument.compoundTag()
    }

    override fun getCompoundTag(ctx: CommandContext<*>, key: String): CompoundBinaryTag {
        val nms = CompoundTagArgument.getCompoundTag(ctx, key)
        return AdventureNBT.fromNms(nms)
    }

    override fun signedMessage(): ArgumentType<*> {
        return MessageArgument.message()
    }

    @Suppress("UNCHECKED_CAST")
    override fun getSignedMessage(
        ctx: CommandContext<*>,
        key: String
    ): Deferred<SignedMessage> {
        val deferred = CompletableDeferred<SignedMessage>()
        MessageArgument.resolveChatMessage(
            ctx as CommandContext<CommandSourceStack>,
            key
        ) { nmsMessage ->
            deferred.complete(nmsMessage.adventureView())
        }

        return deferred
    }
}
