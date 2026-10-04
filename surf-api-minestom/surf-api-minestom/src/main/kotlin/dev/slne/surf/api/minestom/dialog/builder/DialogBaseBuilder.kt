package dev.slne.surf.api.minestom.dialog.builder

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import it.unimi.dsi.fastutil.objects.ObjectList
import net.kyori.adventure.text.Component
import net.minestom.server.dialog.DialogAfterAction
import net.minestom.server.dialog.DialogBody
import net.minestom.server.dialog.DialogInput
import net.minestom.server.dialog.DialogMetadata

/**
 * Builds everything a dialog shows regardless of its type: its title, body and inputs.
 */
class DialogBaseBuilder {
    var title: Component? = null
    var externalTitle: Component? = null
    var canCloseWithEscape: Boolean = true
    var pause: Boolean = false
    var afterAction: DialogAfterAction = DialogAfterAction.CLOSE
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

    fun canCloseWithEscape(canClose: Boolean) {
        this.canCloseWithEscape = canClose
    }

    fun preventClosingWithEscape(prevent: Boolean = true) {
        canCloseWithEscape = !prevent
    }

    /**
     * Whether a single-player world pauses while this dialog is open.
     */
    fun pause(pause: Boolean) {
        this.pause = pause
    }

    fun afterAction(afterAction: DialogAfterAction) {
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
        val built = dialogInput(block)
        if (built.isEmpty()) return

        val inputs = this.inputs
        if (inputs == null) {
            this.inputs = built
            return
        }
        inputs.addAll(built)
    }

    internal fun build(): DialogMetadata {
        val title = title
        require(title != null) { "Dialog base title must not be null" }

        return DialogMetadata(
            title,
            externalTitle,
            canCloseWithEscape,
            pause,
            afterAction,
            body ?: ObjectList.of(),
            inputs ?: ObjectList.of()
        )
    }
}

fun dialogBase(block: DialogBaseBuilder.() -> Unit): DialogMetadata =
    DialogBaseBuilder().apply(block).build()
