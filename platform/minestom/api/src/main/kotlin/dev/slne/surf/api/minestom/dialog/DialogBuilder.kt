package dev.slne.surf.api.minestom.dialog

import dev.slne.surf.api.minestom.dialog.builder.DialogBaseBuilder
import dev.slne.surf.api.minestom.dialog.builder.DialogEntryBuilder
import dev.slne.surf.api.minestom.dialog.builder.DialogTypeBuilder
import net.minestom.server.dialog.Dialog

/**
 * Builds a dialog from its shared [base] and its [type].
 *
 * ```
 * dialog {
 *     base {
 *         title { primary("Are you sure?") }
 *         body { plainMessage { info("This cannot be undone.") } }
 *     }
 *     type {
 *         confirmation {
 *             yes { label { success("Yes") }; action { playerCallback { it.closeDialog() } } }
 *             no { label { error("No") } }
 *         }
 *     }
 * }
 * ```
 */
fun dialog(block: DialogEntryBuilder.() -> Unit): Dialog =
    DialogEntryBuilder().apply(block).build()

fun DialogEntryBuilder.base(block: DialogBaseBuilder.() -> Unit) {
    base(DialogBaseBuilder().apply(block))
}

fun DialogEntryBuilder.type(block: DialogTypeBuilder.() -> Unit) {
    type(DialogTypeBuilder().apply(block))
}
