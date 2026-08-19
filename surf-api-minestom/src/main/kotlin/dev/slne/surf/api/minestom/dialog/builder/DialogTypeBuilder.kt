package dev.slne.surf.api.minestom.dialog.builder

import dev.slne.surf.api.core.util.mutableObjectListOf
import dev.slne.surf.api.core.util.mutableObjectSetOf
import net.minestom.server.dialog.Dialog
import net.minestom.server.dialog.DialogActionButton
import net.minestom.server.dialog.DialogMetadata
import net.minestom.server.registry.HolderSet
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

    /**
     * Offers one entry per dialog, each opening the dialog it stands for.
     *
     * ```
     * type {
     *     dialogList(homes) {
     *         columns(3)
     *         buttonWidth(200)
     *         exitAction { label { spacer("Back") } }
     *     }
     * }
     * ```
     */
    fun dialogList(block: DialogListTypeBuilder.() -> Unit) {
        val builder = DialogListTypeBuilder().apply(block)
        factory = { metadata -> builder.build(metadata) }
    }

    fun dialogList(
        dialogs: Collection<Dialog>,
        block: DialogListTypeBuilder.() -> Unit = {},
    ) {
        dialogList {
            addAll(dialogs)
            block()
        }
    }

    fun dialogList(
        vararg dialogs: Dialog,
        block: DialogListTypeBuilder.() -> Unit = {},
    ) {
        dialogList(dialogs.toList(), block)
    }

    /**
     * Offers one entry per link this server announced to the client.
     */
    fun serverLinks(block: ServerLinksTypeBuilder.() -> Unit = {}) {
        val builder = ServerLinksTypeBuilder().apply(block)
        factory = { metadata -> builder.build(metadata) }
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

    /**
     * Collects the dialogs a dialog list offers and how they are laid out.
     */
    class DialogListTypeBuilder {
        private val dialogs = mutableObjectSetOf<Dialog>()
        var exitAction: DialogActionButton? = null
        var columns: @Range(from = 1, to = Int.MAX_VALUE.toLong()) Int = DEFAULT_COLUMNS
        var buttonWidth: @Range(from = 1, to = 1024) Int = DEFAULT_BUTTON_WIDTH

        fun dialog(dialog: Dialog) {
            dialogs.add(dialog)
        }

        fun addAll(vararg dialogs: Dialog) {
            this.dialogs.addAll(dialogs)
        }

        fun addAll(dialogs: Iterable<Dialog>) {
            this.dialogs.addAll(dialogs)
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

        fun buttonWidth(buttonWidth: @Range(from = 1, to = 1024) Int) {
            this.buttonWidth = buttonWidth
        }

        internal fun build(metadata: DialogMetadata): Dialog = Dialog.DialogList(
            metadata,
            HolderSet.Direct(dialogs.toList()),
            exitAction,
            columns,
            buttonWidth
        )
    }

    /**
     * Collects how the links a server links dialog offers are laid out.
     */
    class ServerLinksTypeBuilder {
        var exitAction: DialogActionButton? = null
        var columns: @Range(from = 1, to = Int.MAX_VALUE.toLong()) Int = DEFAULT_COLUMNS
        var buttonWidth: @Range(from = 1, to = 1024) Int = DEFAULT_BUTTON_WIDTH

        fun exitAction(exitAction: DialogActionButton) {
            this.exitAction = exitAction
        }

        fun exitAction(block: DialogActionButtonBuilder.() -> Unit) {
            exitAction = actionButton(block)
        }

        fun columns(columns: @Range(from = 1, to = Int.MAX_VALUE.toLong()) Int) {
            this.columns = columns
        }

        fun buttonWidth(buttonWidth: @Range(from = 1, to = 1024) Int) {
            this.buttonWidth = buttonWidth
        }

        internal fun build(metadata: DialogMetadata): Dialog =
            Dialog.ServerLinks(metadata, exitAction, columns, buttonWidth)
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
    }

    private companion object {
        const val DEFAULT_COLUMNS = 2
        const val DEFAULT_BUTTON_WIDTH = 150
    }
}
