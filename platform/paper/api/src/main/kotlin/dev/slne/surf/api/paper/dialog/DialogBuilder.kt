@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.dialog

import dev.slne.surf.api.paper.dialog.builder.DialogBase
import dev.slne.surf.api.paper.dialog.builder.DialogBaseBuilder
import dev.slne.surf.api.paper.dialog.builder.DialogType
import dev.slne.surf.api.paper.dialog.builder.DialogTypeBuilder
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsCommonBridge
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry
import org.bukkit.entity.Player

fun dialog(builder: DialogRegistryEntry.Builder.() -> Unit): Dialog =
    Dialog.create { it.empty().apply(builder) }

fun Dialog(builder: DialogRegistryEntry.Builder.() -> Unit): Dialog =
    Dialog.create { it.empty().apply(builder) }

fun DialogRegistryEntry.Builder.base(block: DialogBaseBuilder.() -> Unit) {
    base(DialogBase(block))
}

fun DialogRegistryEntry.Builder.type(block: DialogTypeBuilder.() -> Unit) {
    type(DialogType(block))
}

@NmsUseWithCaution
fun Player.clearDialogs(showEmptyDialogBefore: Boolean = false) {
    SurfPaperNmsCommonBridge.clearDialogs(this, showEmptyDialogBefore)
}