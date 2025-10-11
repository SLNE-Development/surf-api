package dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory

import dev.slne.surf.surfapi.bukkit.api.builder.buildItem
import dev.slne.surf.surfapi.bukkit.api.builder.displayName
import dev.slne.surf.surfapi.bukkit.api.dialog.noticeDialog
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.titleBuilder
import dev.slne.surf.surfapi.core.api.messages.adventure.text
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.ViewType
import me.devnatan.inventoryframework.context.CloseContext
import me.devnatan.inventoryframework.context.RenderContext
import org.bukkit.inventory.ItemType


object TestInventoryView : View() {
    private val counterState = mutableState(0)

    override fun onInit(config: ViewConfigBuilder) {
        config.titleBuilder { primary("Test Inventory View") }
        config.type(ViewType.CHEST)
        config.size(5)
        config.cancelInteractions()
    }

    override fun onFirstRender(render: RenderContext) {
        render.slot(1, buildItem(ItemType.DIAMOND) {
            displayName {
                text("Diamond")
            }
        }).onClick(counterState::increment)
    }

    override fun onClose(close: CloseContext) {
        close.player.showDialog(
            noticeDialog(
                text("Du hast das Inventar geschlossen!"),
                text("Du hast den Diamanten ${counterState.get(close)} mal geklickt!")
            )
        )
    }
}