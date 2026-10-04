package dev.slne.surf.api.minestom.dialog.builder

import dev.slne.surf.api.minestom.dialog.callback.DialogCallbackContext
import dev.slne.surf.api.minestom.dialog.callback.DialogCallbackOptions
import dev.slne.surf.api.minestom.dialog.callback.DialogCallbacks
import dev.slne.surf.api.minestom.dialog.callback.DialogResponseView
import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.BinaryTag
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.minestom.server.dialog.Dialog
import net.minestom.server.dialog.DialogAction
import net.minestom.server.entity.Player
import java.net.URL

/**
 * Builds what a dialog button does when it is pressed.
 */
class DialogActionBuilder {

    private var action: DialogAction? = null

    fun action(action: DialogAction) {
        this.action = action
    }

    fun openUrl(url: String) {
        action(DialogAction.OpenUrl(url))
    }

    fun openUrl(url: URL) {
        openUrl(url.toString())
    }

    fun runCommand(command: String) {
        action(DialogAction.RunCommand(command))
    }

    fun suggestCommand(command: String) {
        action(DialogAction.SuggestCommand(command))
    }

    fun copyToClipboard(value: String) {
        action(DialogAction.CopyToClipboard(value))
    }

    fun changePage(page: Int) {
        action(DialogAction.ChangePage(page))
    }

    fun showDialog(dialog: Dialog) {
        action(DialogAction.ShowDialog(dialog))
    }

    fun showDialog(block: DialogEntryBuilder.() -> Unit) {
        showDialog(dev.slne.surf.api.minestom.dialog.dialog(block))
    }

    /**
     * Runs the command [template] with the dialog's input values substituted into it.
     */
    fun commandTemplate(template: String) {
        action(DialogAction.DynamicRunCommand(template))
    }

    fun customClick(id: Key, payload: BinaryTag? = null) {
        action(DialogAction.Custom(id, payload))
    }

    fun customClick(id: Key, payload: CompoundBinaryTag.Builder.() -> Unit) {
        customClick(id, CompoundBinaryTag.builder().apply(payload).build())
    }

    /**
     * Reports the dialog's input values back under [id], merged with [additions].
     */
    fun dynamicCustomClick(id: Key, additions: CompoundBinaryTag? = null) {
        action(DialogAction.DynamicCustom(id, additions))
    }

    /**
     * Runs [callback] on the server once the button is pressed.
     */
    fun callback(
        options: DialogCallbackOptions = DialogCallbackOptions.DEFAULT,
        callback: (DialogCallbackContext) -> Unit,
    ) {
        action(DialogCallbacks.action(options, callback))
    }

    /**
     * Runs [callback] with the pressing player once the button is pressed.
     */
    fun playerCallback(
        options: DialogCallbackOptions = DialogCallbackOptions.DEFAULT,
        callback: (Player) -> Unit,
    ) {
        callback(options) { context -> callback(context.player) }
    }

    /**
     * Runs [callback] with the values the dialog collected once the button is pressed.
     *
     * The client reports its input values back, merged with [additions].
     */
    fun customClick(
        options: DialogCallbackOptions = DialogCallbackOptions.DEFAULT,
        additions: CompoundBinaryTag? = null,
        callback: (DialogResponseView, DialogCallbackContext) -> Unit,
    ) {
        action(
            DialogCallbacks.dynamicAction(options, additions) { context ->
                callback(DialogResponseView.of(context.payload), context)
            }
        )
    }

    /**
     * Runs [callback] with the values the dialog collected and the pressing player once the button
     * is pressed.
     *
     * ```
     * action {
     *     customPlayerClick { response, player ->
     *         player.sendMessage(response.getText("message") ?: "")
     *     }
     * }
     * ```
     */
    fun customPlayerClick(
        options: DialogCallbackOptions = DialogCallbackOptions.DEFAULT,
        additions: CompoundBinaryTag? = null,
        callback: (response: DialogResponseView, player: Player) -> Unit,
    ) {
        customClick(options, additions) { response, context ->
            callback(response, context.player)
        }
    }

    internal fun build(): DialogAction {
        val action = action
        require(action != null) { "DialogAction must be built" }
        return action
    }
}

fun dialogAction(block: DialogActionBuilder.() -> Unit): DialogAction =
    DialogActionBuilder().apply(block).build()
