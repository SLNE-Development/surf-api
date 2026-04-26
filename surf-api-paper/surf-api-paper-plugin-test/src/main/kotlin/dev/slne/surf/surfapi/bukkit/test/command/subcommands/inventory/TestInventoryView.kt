package dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory

import dev.slne.surf.api.core.messages.adventure.text
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.dialog.noticeDialog
import dev.slne.surf.api.paper.inventory.framework.dsl.slot
import dev.slne.surf.api.paper.inventory.framework.dsl.withItem
import dev.slne.surf.api.paper.inventory.framework.titleBuilder
import dev.slne.surf.api.paper.inventory.framework.view.onClose
import dev.slne.surf.api.paper.inventory.framework.view.onFirstRender
import dev.slne.surf.api.paper.inventory.framework.view.state.get
import dev.slne.surf.api.paper.inventory.framework.view.state.increment
import dev.slne.surf.api.paper.inventory.framework.view.state.mutableState
import dev.slne.surf.api.paper.inventory.framework.view.surfView
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.ViewType
import me.devnatan.inventoryframework.context.CloseContext
import me.devnatan.inventoryframework.context.RenderContext
import org.bukkit.inventory.ItemType

val testInventoryViewDsl = surfView("TestInventoryView") {
    val counterState = mutableState(0)

    onFirstRender {
        slot(1) {
            onClick(counterState::increment)
            withItem(ItemType.DIAMOND) {
                displayName {
                    text("Diamond")
                }
            }
        }
    }

    onClose {
        player.showDialog(
            noticeDialog(
                text("Du hast das Inventar geschlossen!"),
                text("Du hast den Diamanten ${counterState[this]} mal geklickt!")
            )
        )
    }
}

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