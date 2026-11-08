package dev.slne.surf.api.minestom.dialog

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.minestom.dialog.builder.DialogEntryBuilder
import net.kyori.adventure.text.Component
import org.jetbrains.annotations.Range

/**
 * Builds a dialog that only acknowledges what it says.
 */
fun noticeDialog(block: DialogEntryBuilder.() -> Unit) = dialog {
    block()
    type { notice() }
}

/**
 * Builds a dialog showing [notice] under [title], acknowledged by a single button.
 */
fun noticeDialog(
    title: Component,
    notice: Component,
    width: @Range(from = 1, to = 1024) Int? = null
) = noticeDialog {
    base {
        title(title)
        body {
            plainMessage(notice, width)
        }
    }
}

/**
 * Builds a dialog showing the built notice under [title], acknowledged by a single button.
 */
fun noticeDialogWithBuilder(
    title: Component,
    width: @Range(from = 1, to = 1024) Int? = null,
    notice: SurfComponentBuilder.() -> Unit,
) = noticeDialog(title, SurfComponentBuilder(notice), width)
