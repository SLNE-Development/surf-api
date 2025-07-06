@file:Suppress("UnstableApiUsage")

package dev.slne.surf.surfapi.bukkit.api.dialog

import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry
import net.kyori.adventure.text.Component
import org.jetbrains.annotations.Range

fun noticeDialog(builder: DialogRegistryEntry.Builder.() -> Unit) = dialog {
    builder()
    type { notice() }
}

fun noticeDialog(notice: Component, width: @Range(from = 1, to = 1024) Int? = null) = noticeDialog {
    base {
        body {
            plainMessage(notice, width)
        }
    }
}

fun noticeDialogWithBuilder(
    width: @Range(from = 1, to = 1024) Int? = null,
    notice: SurfComponentBuilder.() -> Unit,
) = noticeDialog(SurfComponentBuilder(notice), width)