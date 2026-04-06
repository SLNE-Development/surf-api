@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.dialog.builder

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.core.util.mutableObjectListOf
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody
import it.unimi.dsi.fastutil.objects.ObjectList
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import org.jetbrains.annotations.Range

class DialogBodyBuilder {
    private val bodies = mutableObjectListOf<DialogBody>()

    fun plain(block: PlainDialogMessageBuilder.() -> Unit) {
        val builder = PlainDialogMessageBuilder().apply(block)
        bodies.add(builder.build())
    }

    fun plainMessage(
        message: Component,
        width: @Range(from = 1, to = 1024) Int? = null,
    ) {
        bodies.add(
            if (width != null) {
                DialogBody.plainMessage(message, width)
            } else {
                DialogBody.plainMessage(message)
            }
        )
    }

    fun plainMessage(plain: PlainMessageDialogBody) {
        bodies.add(plain)
    }

    fun plainMessage(
        width: @Range(from = 1, to = 1024) Int? = null,
        block: SurfComponentBuilder.() -> Unit,
    ) {
        plainMessage(SurfComponentBuilder(block), width)
    }

    fun item(block: ItemDialogMessageBuilder.() -> Unit) {
        val builder = ItemDialogMessageBuilder().apply(block)
        bodies.add(builder.build())
    }

    internal fun build(): ObjectList<DialogBody> = bodies

    class PlainDialogMessageBuilder {
        var message: Component? = null
        var width: @Range(from = 1, to = 1024) Int? = null

        fun message(message: Component) {
            this.message = message
        }

        fun message(block: SurfComponentBuilder.() -> Unit) {
            message(SurfComponentBuilder(block))
        }

        fun width(width: @Range(from = 1, to = 1024) Int) {
            this.width = width
        }

        internal fun build(): DialogBody {
            val message = message
            val width = width
            require(message != null) { "Dialog body message must not be null" }
            return if (width != null) {
                DialogBody.plainMessage(message, width)
            } else {
                DialogBody.plainMessage(message)
            }
        }
    }

    class ItemDialogMessageBuilder {
        var item: ItemStack? = null
        var description: PlainMessageDialogBody? = null
        var showDecorations: Boolean? = null
        var showTooltip: Boolean? = null
        var width: @Range(from = 1, to = 256) Int? = null
        var height: @Range(from = 1, to = 256) Int? = null

        fun item(item: ItemStack) {
            this.item = item
        }

        fun item(type: ItemType, amount: Int = 1, block: ItemStack.() -> Unit = {}) {
            item = type.createItemStack(amount).apply(block)
        }

        fun description(description: PlainMessageDialogBody) {
            this.description = description
        }

        fun description(block: PlainDialogMessageBuilder.() -> Unit) {
            description = PlainDialogMessageBuilder().apply(block).build() as PlainMessageDialogBody
        }

        fun simpleDescription(
            message: Component,
            width: @Range(from = 1, to = 1024) Int? = null,
        ) {
            description = if (width != null) {
                DialogBody.plainMessage(message, width)
            } else {
                DialogBody.plainMessage(message)
            }
        }

        fun simpleDescription(
            width: @Range(from = 1, to = 1024) Int? = null,
            block: SurfComponentBuilder.() -> Unit,
        ) {
            simpleDescription(SurfComponentBuilder(block), width)
        }

        fun showDecorations(show: Boolean) {
            this.showDecorations = show
        }

        fun showTooltip(show: Boolean) {
            this.showTooltip = show
        }

        fun width(width: @Range(from = 1, to = 256) Int) {
            this.width = width
        }

        fun height(height: @Range(from = 1, to = 256) Int) {
            this.height = height
        }

        internal fun build(): DialogBody {
            val item = item
            require(item != null) { "Dialog body item must not be null" }
            val builder = DialogBody.item(item)
            description?.let { builder.description(it) }
            showDecorations?.let { builder.showDecorations(it) }
            showTooltip?.let { builder.showTooltip(it) }
            width?.let { builder.width(it) }
            height?.let { builder.height(it) }
            return builder.build()
        }
    }
}

fun DialogBody(block: DialogBodyBuilder.() -> Unit): ObjectList<DialogBody> =
    DialogBodyBuilder().apply(block).build()

fun dialogBody(block: DialogBodyBuilder.() -> Unit): ObjectList<DialogBody> =
    DialogBodyBuilder().apply(block).build()