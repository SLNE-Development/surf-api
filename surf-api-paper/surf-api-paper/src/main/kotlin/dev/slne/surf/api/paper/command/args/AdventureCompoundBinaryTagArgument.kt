package dev.slne.surf.api.paper.command.args

import com.mojang.brigadier.context.CommandContext
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.CommandAPIArgumentType
import dev.jorel.commandapi.arguments.SafeOverrideableArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsCommandArgumentTypesBridge
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.nbt.TagStringIO

@OptIn(NmsUseWithCaution::class)
class AdventureCompoundBinaryTagArgument(nodeName: String) :
    SafeOverrideableArgument<CompoundBinaryTag, CompoundBinaryTag>(
        nodeName,
        SurfPaperNmsCommandArgumentTypesBridge.Companion::compoundTag,
        { TagStringIO.tagStringIO().asString(it) }
    ) {
    override fun getPrimitiveType(): Class<CompoundBinaryTag> {
        return CompoundBinaryTag::class.java
    }

    override fun getArgumentType(): CommandAPIArgumentType {
        return CommandAPIArgumentType.NBT_COMPOUND
    }

    override fun <Source> parseArgument(
        ctx: CommandContext<Source>,
        key: String,
        previousArgs: CommandArguments
    ): CompoundBinaryTag {
        return SurfPaperNmsCommandArgumentTypesBridge.Companion.getCompoundTag(ctx, key)
    }
}

inline fun CommandTree.adventureCompoundBinaryTagArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): CommandTree =
    then(AdventureCompoundBinaryTagArgument(nodeName).setOptional(optional).apply(block))

inline fun Argument<*>.adventureCompoundBinaryTagArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): Argument<*> =
    then(AdventureCompoundBinaryTagArgument(nodeName).setOptional(optional).apply(block))


inline fun CommandAPICommand.adventureCompoundBinaryTagArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): CommandAPICommand =
    withArguments(AdventureCompoundBinaryTagArgument(nodeName).setOptional(optional).apply(block))