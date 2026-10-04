package dev.slne.surf.api.minestom.dialog.builder

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.Component
import net.minestom.server.dialog.DialogAction
import net.minestom.server.dialog.DialogActionButton
import org.jetbrains.annotations.Range

/**
 * Builds a single pressable dialog button.
 */
class DialogActionButtonBuilder {
    var label: Component? = null
    var tooltip: Component? = null
    var width: @Range(from = 1, to = 1024) Int = DialogActionButton.DEFAULT_WIDTH
    var action: DialogAction? = null

    fun label(label: Component) {
        this.label = label
    }

    fun label(block: SurfComponentBuilder.() -> Unit) {
        label(SurfComponentBuilder(block))
    }

    fun tooltip(tooltip: Component) {
        this.tooltip = tooltip
    }

    fun tooltip(block: SurfComponentBuilder.() -> Unit) {
        tooltip(SurfComponentBuilder(block))
    }

    fun width(width: @Range(from = 1, to = 1024) Int) {
        this.width = width
    }

    fun action(action: DialogAction) {
        this.action = action
    }

    fun action(block: DialogActionBuilder.() -> Unit) {
        action(dialogAction(block))
    }

    internal fun build(): DialogActionButton {
        val label = label
        require(label != null) { "ActionButton label must not be null" }

        return DialogActionButton(label, tooltip, width, action)
    }
}

fun actionButton(block: DialogActionButtonBuilder.() -> Unit): DialogActionButton =
    DialogActionButtonBuilder().apply(block).build()
