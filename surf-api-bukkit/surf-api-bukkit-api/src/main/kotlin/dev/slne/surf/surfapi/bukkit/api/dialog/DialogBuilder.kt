@file:Suppress("UnstableApiUsage")

package dev.slne.surf.surfapi.bukkit.api.dialog

import dev.slne.surf.surfapi.bukkit.api.dialog.builder.DialogBase
import dev.slne.surf.surfapi.bukkit.api.dialog.builder.DialogBaseBuilder
import dev.slne.surf.surfapi.bukkit.api.dialog.builder.DialogType
import dev.slne.surf.surfapi.bukkit.api.dialog.builder.DialogTypeBuilder
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry

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