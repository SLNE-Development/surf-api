@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.dialog.builder

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import it.unimi.dsi.fastutil.objects.ObjectList
import net.kyori.adventure.text.Component

class DialogBaseBuilder {
    var title: Component? = null
    var externalTitle: Component? = null
    var canCloseWithEscape: Boolean? = null
    var afterAction: DialogBase.DialogAfterAction? = null
    private var body: ObjectList<DialogBody>? = null
    private var inputs: ObjectList<DialogInput>? = null

    fun title(title: Component) {
        this.title = title
    }

    fun title(block: SurfComponentBuilder.() -> Unit) {
        title(SurfComponentBuilder(block))
    }

    fun externalTitle(externalTitle: Component) {
        this.externalTitle = externalTitle
    }

    fun externalTitle(block: SurfComponentBuilder.() -> Unit) {
        externalTitle(SurfComponentBuilder(block))
    }

    fun preventClosingWithEscape(prevent: Boolean = true) {
        this.canCloseWithEscape = !prevent
    }

    fun afterAction(afterAction: DialogBase.DialogAfterAction) {
        this.afterAction = afterAction
    }

    fun body(block: DialogBodyBuilder.() -> Unit) {
        val bodies = dialogBody(block)
        if (bodies.isEmpty()) return
        val body = body
        if (body == null) {
            this.body = bodies
            return
        }
        body.addAll(bodies)
    }

    fun input(block: DialogInputBuilder.() -> Unit) {
        val inputs = dialogInput(block)
        if (inputs.isEmpty()) return
        val input = this.inputs
        if (input == null) {
            this.inputs = inputs
            return
        }
        input.addAll(inputs)
    }

    internal fun build(): DialogBase {
        val title = title
        require(title != null) { "Dialog base title must not be null" }
        val builder = DialogBase.builder(title)
            .pause(false)
        with(builder) {
            body?.let { body(it) }
            inputs?.let { inputs(it) }
            externalTitle?.let { externalTitle(it) }
            canCloseWithEscape?.let { canCloseWithEscape(it) }
            afterAction?.let { afterAction(it) }
        }

        return builder.build()
    }
}

fun dialogBase(block: DialogBaseBuilder.() -> Unit): DialogBase =
    DialogBaseBuilder().apply(block).build()

fun DialogBase(block: DialogBaseBuilder.() -> Unit): DialogBase =
    DialogBaseBuilder().apply(block).build()