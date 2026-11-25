package dev.slne.surf.api.paper.nms.bridges

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.spring.SurfApiBean
import dev.slne.surf.api.spring.surfBean
import kotlinx.coroutines.Deferred
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.nbt.CompoundBinaryTag

@NmsUseWithCaution
@SurfApiBean
interface SurfPaperNmsCommandArgumentTypesBridge {

    fun compoundTag(): ArgumentType<*>
    fun getCompoundTag(ctx: CommandContext<*>, key: String): CompoundBinaryTag

    fun signedMessage(): ArgumentType<*>
    fun getSignedMessage(ctx: CommandContext<*>, key: String): Deferred<SignedMessage>

    companion object {
        internal val instance by surfBean<SurfPaperNmsCommandArgumentTypesBridge>()
    }
}
