@file:Suppress("UnstableApiUsage")

package dev.slne.surf.surfapi.bukkit.api.dialog.builder

import dev.slne.surf.surfapi.bukkit.api.dialog.dialog
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.dialog.DialogLike
import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.nbt.TagStringIO
import net.kyori.adventure.nbt.api.BinaryTagHolder
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.event.ClickEvent
import java.net.URL

class DialogActionBuilder {

    private var action: DialogAction? = null

    fun staticAction(clickEvent: ClickEvent) {
        action = DialogAction.staticAction(clickEvent)
    }

    fun showDialog(dialog: DialogLike) {
        staticAction(ClickEvent.showDialog(dialog))
    }

    fun showDialog(builder: DialogRegistryEntry.Builder.() -> Unit) {
        showDialog(dialog(builder))
    }

    fun copyToClipboard(text: String) {
        staticAction(ClickEvent.copyToClipboard(text))
    }

    fun openUrl(url: String) {
        staticAction(ClickEvent.openUrl(url))
    }

    fun openUrl(url: URL) {
        staticAction(ClickEvent.openUrl(url))
    }

    fun suggestCommand(command: String) {
        staticAction(ClickEvent.suggestCommand(command))
    }

    fun callback(callback: ClickCallback<Audience>) {
        staticAction(ClickEvent.callback(callback))
    }

    fun commandTemplate(template: String) {
        action = DialogAction.commandTemplate(template)
    }

    fun customClick(id: Key, additions: BinaryTagHolder? = null) {
        action = DialogAction.customClick(id, additions)
    }

    fun customClick(
        id: Key,
        additions: CompoundBinaryTag.Builder.() -> Unit,
    ) {
        val additionsTag = CompoundBinaryTag.builder().apply(additions).build()
        val holder =
            BinaryTagHolder.binaryTagHolder(TagStringIO.tagStringIO().asString(additionsTag))

        action = DialogAction.customClick(id, holder)
    }

    fun customClick(options: ClickCallback.Options? = null, callback: DialogActionCallback) {
        val options = options ?: ClickCallback.Options.builder().build()
        action = DialogAction.customClick(callback, options)
    }

    internal fun build(): DialogAction {
        val action = action
        require(action != null) { "DialogAction must be built" }
        return action
    }
}

fun DialogAction(block: DialogActionBuilder.() -> Unit): DialogAction =
    DialogActionBuilder().apply(block).build()

fun dialogAction(block: DialogActionBuilder.() -> Unit): DialogAction =
    DialogActionBuilder().apply(block).build()