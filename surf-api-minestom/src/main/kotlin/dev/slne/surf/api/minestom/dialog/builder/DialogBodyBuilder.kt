package dev.slne.surf.api.minestom.dialog.builder

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.core.util.mutableObjectListOf
import it.unimi.dsi.fastutil.objects.ObjectList
import net.kyori.adventure.text.Component
import net.minestom.server.dialog.DialogBody
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.jetbrains.annotations.Range

/**
 * Collects the body elements of a dialog.
 */
class DialogBodyBuilder {
    private val bodies = mutableObjectListOf<DialogBody>()

    fun plain(block: PlainDialogMessageBuilder.() -> Unit) {
        bodies.add(PlainDialogMessageBuilder().apply(block).build())
    }

    fun plainMessage(
        message: Component,
        width: @Range(from = 1, to = 1024) Int? = null,
    ) {
        bodies.add(DialogBody.PlainMessage(message, width ?: DialogBody.PlainMessage.DEFAULT_WIDTH))
    }

    fun plainMessage(plain: DialogBody.PlainMessage) {
        bodies.add(plain)
    }

    fun plainMessage(
        width: @Range(from = 1, to = 1024) Int? = null,
        block: SurfComponentBuilder.() -> Unit,
    ) {
        plainMessage(SurfComponentBuilder(block), width)
    }

    fun item(block: ItemDialogMessageBuilder.() -> Unit) {
        bodies.add(ItemDialogMessageBuilder().apply(block).build())
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

        internal fun build(): DialogBody.PlainMessage {
            val message = message
            require(message != null) { "Dialog body message must not be null" }

            return DialogBody.PlainMessage(
                message,
                width ?: DialogBody.PlainMessage.DEFAULT_WIDTH
            )
        }
    }

    class ItemDialogMessageBuilder {
        var item: ItemStack? = null
        var description: DialogBody.PlainMessage? = null
        var showDecorations: Boolean = true
        var showTooltip: Boolean = true
        var width: @Range(from = 1, to = 256) Int = DEFAULT_ITEM_SIZE
        var height: @Range(from = 1, to = 256) Int = DEFAULT_ITEM_SIZE

        fun item(item: ItemStack) {
            this.item = item
        }

        fun item(material: Material, amount: Int = 1) {
            item = ItemStack.of(material, amount)
        }

        fun description(description: DialogBody.PlainMessage) {
            this.description = description
        }

        fun description(block: PlainDialogMessageBuilder.() -> Unit) {
            description = PlainDialogMessageBuilder().apply(block).build()
        }

        fun simpleDescription(
            message: Component,
            width: @Range(from = 1, to = 1024) Int? = null,
        ) {
            description = DialogBody.PlainMessage(
                message,
                width ?: DialogBody.PlainMessage.DEFAULT_WIDTH
            )
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

        internal fun build(): DialogBody.Item {
            val item = item
            require(item != null) { "Dialog body item must not be null" }

            return DialogBody.Item(item, description, showDecorations, showTooltip, width, height)
        }

        private companion object {
            const val DEFAULT_ITEM_SIZE = 16
        }
    }
}

fun dialogBody(block: DialogBodyBuilder.() -> Unit): ObjectList<DialogBody> =
    DialogBodyBuilder().apply(block).build()
