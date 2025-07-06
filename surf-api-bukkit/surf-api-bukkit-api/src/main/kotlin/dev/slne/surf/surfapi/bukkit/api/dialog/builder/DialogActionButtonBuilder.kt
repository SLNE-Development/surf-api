@file:Suppress("UnstableApiUsage")

package dev.slne.surf.surfapi.bukkit.api.dialog.builder

import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.action.DialogAction
import net.kyori.adventure.text.Component
import org.jetbrains.annotations.Range

class DialogActionButtonBuilder { // permission
    var label: Component? = null
    var tooltip: Component? = null
    var width: @Range(from = 1, to = 1024) Int? = null
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
        action(DialogAction(block))
    }

    internal fun build(): ActionButton {
        val label = label
        require(label != null) { "ActionButton label must not be null" }
        val builder = ActionButton.builder(label)
        with(builder) {
            tooltip?.let { tooltip(it) }
            width?.let { width(it) }
            action?.let { action(it) }
        }
        return builder.build()
    }
}

fun ActionButton(block: DialogActionButtonBuilder.() -> Unit): ActionButton =
    DialogActionButtonBuilder().apply(block).build()

fun actionButton(block: DialogActionButtonBuilder.() -> Unit): ActionButton =
    DialogActionButtonBuilder().apply(block).build()