package dev.slne.surf.api.minestom.dialog.builder

import dev.slne.surf.api.core.util.mutableObjectListOf
import net.minestom.server.dialog.Dialog
import net.minestom.server.dialog.DialogActionButton
import net.minestom.server.dialog.DialogMetadata
import org.jetbrains.annotations.Range

/**
 * Picks which kind of dialog is built and which buttons it offers.
 *
 * Every kind is described as a factory, because the type and the shared metadata only come together
 * once the surrounding dialog is built.
 */
class DialogTypeBuilder {

    private var factory: ((DialogMetadata) -> Dialog)? = null

    fun notice() {
        factory = { metadata -> Dialog.Notice(metadata, Dialog.Notice.DEFAULT_ACTION) }
    }

    fun notice(button: DialogActionButton) {
        factory = { metadata -> Dialog.Notice(metadata, button) }
    }

    fun notice(block: DialogActionButtonBuilder.() -> Unit) {
        notice(actionButton(block))
    }

    fun confirmation(yes: DialogActionButton, no: DialogActionButton) {
        factory = { metadata -> Dialog.Confirmation(metadata, yes, no) }
    }

    fun confirmation(block: DialogConfirmationTypeBuilder.() -> Unit) {
        val builder = DialogConfirmationTypeBuilder().apply(block)
        factory = { metadata -> builder.build(metadata) }
    }

    fun multiAction(block: DialogMultiActionTypeBuilder.() -> Unit) {
        val builder = DialogMultiActionTypeBuilder().apply(block)
        factory = { metadata -> builder.build(metadata) }
    }

    fun multiAction(
        actions: List<DialogActionButton>,
        block: DialogMultiActionTypeBuilder.() -> Unit = {},
    ) {
        multiAction {
            actions.forEach { action(it) }
            block()
        }
    }

    fun multiAction(
        vararg actions: DialogActionButton,
        block: DialogMultiActionTypeBuilder.() -> Unit = {},
    ) {
        multiAction(actions.toList(), block)
    }

    internal fun build(metadata: DialogMetadata): Dialog {
        val factory = factory
        require(factory != null) { "Dialog type must be built" }
        return factory(metadata)
    }

    class DialogConfirmationTypeBuilder {
        var yes: DialogActionButton? = null
        var no: DialogActionButton? = null

        fun yes(yes: DialogActionButton) {
            this.yes = yes
        }

        fun yes(block: DialogActionButtonBuilder.() -> Unit) {
            yes = actionButton(block)
        }

        fun no(no: DialogActionButton) {
            this.no = no
        }

        fun no(block: DialogActionButtonBuilder.() -> Unit) {
            no = actionButton(block)
        }

        internal fun build(metadata: DialogMetadata): Dialog {
            val yes = yes
            val no = no
            require(yes != null) { "Yes action must not be null" }
            require(no != null) { "No action must not be null" }

            return Dialog.Confirmation(metadata, yes, no)
        }
    }

    class DialogMultiActionTypeBuilder {
        private val actions = mutableObjectListOf<DialogActionButton>()
        var exitAction: DialogActionButton? = null
        var columns: @Range(from = 1, to = Int.MAX_VALUE.toLong()) Int = DEFAULT_COLUMNS

        fun action(action: DialogActionButton) {
            actions.add(action)
        }

        fun action(block: DialogActionButtonBuilder.() -> Unit) {
            action(actionButton(block))
        }

        fun exitAction(exitAction: DialogActionButton) {
            this.exitAction = exitAction
        }

        fun exitAction(block: DialogActionButtonBuilder.() -> Unit) {
            exitAction = actionButton(block)
        }

        fun columns(columns: @Range(from = 1, to = Int.MAX_VALUE.toLong()) Int) {
            this.columns = columns
        }

        internal fun build(metadata: DialogMetadata): Dialog {
            require(actions.isNotEmpty()) { "A multi action dialog must offer at least one action" }

            return Dialog.MultiAction(metadata, actions, exitAction, columns)
        }

        private companion object {
            const val DEFAULT_COLUMNS = 2
        }
    }
}
