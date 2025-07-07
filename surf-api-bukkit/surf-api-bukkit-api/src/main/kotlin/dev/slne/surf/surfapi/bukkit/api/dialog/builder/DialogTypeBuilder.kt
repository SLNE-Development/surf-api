@file:Suppress("UnstableApiUsage")

package dev.slne.surf.surfapi.bukkit.api.dialog.builder

import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import dev.slne.surf.surfapi.core.api.util.mutableObjectSetOf
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.type.DialogType
import io.papermc.paper.registry.set.RegistrySet
import org.jetbrains.annotations.Range

class DialogTypeBuilder {

    private var type: DialogType? = null

    fun notice() {
        type = DialogType.notice()
    }

    fun notice(button: ActionButton) {
        type = DialogType.notice(button)
    }

    fun notice(block: DialogActionButtonBuilder.() -> Unit) {
        type = DialogType.notice(ActionButton(block))
    }

    fun dialogList(vararg keys: TypedKey<Dialog>, block: DialogListTypeBuilder.() -> Unit = {}) {
        dialogList {
            dialog(*keys)
            block()
        }
    }

    fun dialogList(vararg dialogs: Dialog, block: DialogListTypeBuilder.() -> Unit = {}) {
        dialogList {
            addAll(*dialogs)
            block()
        }
    }

    fun dialogList(block: DialogListTypeBuilder.() -> Unit) {
        type = DialogListTypeBuilder().apply(block).build()
    }

    fun confirmation(block: DialogConfirmationTypeBuilder.() -> Unit = {}) {
        type = DialogConfirmationTypeBuilder().apply(block).build()
    }

    fun confirmation(yes: ActionButton, no: ActionButton) {
        type = DialogType.confirmation(yes, no)
    }

    fun multiAction(block: DialogMultiActionTypeBuilder.() -> Unit) {
        type = DialogMultiActionTypeBuilder().apply(block).build()
    }

    fun multiAction(
        actions: List<ActionButton>,
        block: DialogMultiActionTypeBuilder.() -> Unit = {},
    ) {
        type = DialogMultiActionTypeBuilder().apply {
            actions.forEach { action(it) }
            block()
        }.build()
    }

    fun multiAction(
        vararg actions: ActionButton,
        block: DialogMultiActionTypeBuilder.() -> Unit = {},
    ) {
        multiAction(actions.toList(), block)
    }

    fun serverLinks(block: ServerLinksTypeBuilder.() -> Unit) {
        type = ServerLinksTypeBuilder().apply(block).build()
    }

    internal fun build(): DialogType {
        val type = type
        require(type != null) { "Dialog type must be built" }
        return type
    }

    class DialogListTypeBuilder {
        var exitAction: ActionButton? = null
        var columns: @Range(from = 1, to = Int.MAX_VALUE.toLong()) Int? = null
        var buttonWidth: @Range(from = 1, to = 1024) Int? = null
        private val dialogs = mutableObjectSetOf<Dialog>()

        fun exitAction(exitAction: ActionButton) {
            this.exitAction = exitAction
        }

        fun exitAction(block: DialogActionButtonBuilder.() -> Unit) {
            exitAction = ActionButton(block)
        }

        fun columns(columns: @Range(from = 1, to = Int.MAX_VALUE.toLong()) Int) {
            this.columns = columns
        }

        fun buttonWidth(buttonWidth: @Range(from = 1, to = 1024) Int) {
            this.buttonWidth = buttonWidth
        }

        fun dialog(dialog: Dialog) {
            dialogs.add(dialog)
        }

        fun addAll(vararg dialogs: Dialog) {
            this.dialogs.addAll(dialogs)
        }

        fun addAll(dialogs: Iterable<Dialog>) {
            this.dialogs.addAll(dialogs)
        }

        fun dialog(key: TypedKey<Dialog>) {
            dialogs.add(
                RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG).getOrThrow(key)
            )
        }

        fun dialog(vararg keys: TypedKey<Dialog>) {
            dialogs.addAll(
                keys.map { RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG).getOrThrow(it) }
            )
        }

        internal fun build(): DialogType {
            val builder = DialogType.dialogList(RegistrySet.valueSet(RegistryKey.DIALOG, dialogs))
            with(builder) {
                exitAction?.let { exitAction(it) }
                columns?.let { columns(it) }
                buttonWidth?.let { buttonWidth(it) }
            }
            return builder.build()
        }
    }

    class DialogConfirmationTypeBuilder {
        var yes: ActionButton? = null
        var no: ActionButton? = null

        fun yes(yes: ActionButton) {
            this.yes = yes
        }

        fun yes(block: DialogActionButtonBuilder.() -> Unit) {
            yes = ActionButton(block)
        }

        fun no(no: ActionButton) {
            this.no = no
        }

        fun no(block: DialogActionButtonBuilder.() -> Unit) {
            no = ActionButton(block)
        }

        internal fun build(): DialogType {
            val yes = yes
            val no = no
            require(yes != null) { "Yes action must not be null" }
            require(no != null) { "No action must not be null" }
            return DialogType.confirmation(yes, no)
        }
    }

    class DialogMultiActionTypeBuilder {
        private val actions = mutableObjectListOf<ActionButton>()
        var exitAction: ActionButton? = null
        var columns: @Range(from = 1, to = Int.MAX_VALUE.toLong()) Int? = null

        fun action(action: ActionButton) {
            actions.add(action)
        }

        fun action(block: DialogActionButtonBuilder.() -> Unit) {
            actions.add(ActionButton(block))
        }

        fun exitAction(exitAction: ActionButton) {
            this.exitAction = exitAction
        }

        fun exitAction(block: DialogActionButtonBuilder.() -> Unit) {
            exitAction = ActionButton(block)
        }

        fun columns(columns: @Range(from = 1, to = Int.MAX_VALUE.toLong()) Int) {
            this.columns = columns
        }

        internal fun build(): DialogType {
            val builder = DialogType.multiAction(actions)
            with(builder) {
                exitAction?.let { exitAction(it) }
                columns?.let { columns(it) }
            }
            return builder.build()
        }
    }

    class ServerLinksTypeBuilder {
        var exitAction: ActionButton? = null
        var columns: @Range(from = 1, to = Int.MAX_VALUE.toLong()) Int = 2
        var buttonWidth: @Range(from = 1, to = 1024) Int = 150

        fun exitAction(exitAction: ActionButton) {
            this.exitAction = exitAction
        }

        fun exitAction(block: DialogActionButtonBuilder.() -> Unit) {
            exitAction = ActionButton(block)
        }

        fun columns(columns: @Range(from = 1, to = Int.MAX_VALUE.toLong()) Int) {
            this.columns = columns
        }

        fun buttonWidth(buttonWidth: @Range(from = 1, to = 1024) Int) {
            this.buttonWidth = buttonWidth
        }

        internal fun build(): DialogType {
            return DialogType.serverLinks(exitAction, columns, buttonWidth)
        }
    }
}

fun DialogType(block: DialogTypeBuilder.() -> Unit): DialogType =
    DialogTypeBuilder().apply(block).build()

fun dialogType(block: DialogTypeBuilder.() -> Unit): DialogType =
    DialogTypeBuilder().apply(block).build()