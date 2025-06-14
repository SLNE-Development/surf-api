package dev.slne.surf.surfapi.bukkit.server.impl.inventory

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.inventory.InventoryBridge
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.Gui
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.types.ChestGui
import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.item.UpdatableGuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes.OutlinePane
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes.PaginatedPane
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes.StaticPane
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Mask
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Pattern
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Slot
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui.AbstractGui
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui.types.ChestGuiImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.item.GuiItemImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.item.UpdatableGuiItemImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane.panes.OutlinePaneImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane.panes.PaginatedPaneImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane.panes.StaticPaneImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane.utils.MaskImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane.utils.PatternImpl
import org.bukkit.NamespacedKey
import org.bukkit.inventory.Inventory
import java.util.*

@AutoService(InventoryBridge::class)
class InventoryBridgeImpl : InventoryBridge {
    override fun getGuiByInventory(inventory: Inventory): Gui? {
        return AbstractGui.getGui(inventory)
    }

    override fun createGuiItem(
        key: NamespacedKey?,
        uuid: UUID?,
    ): GuiItem {
        return GuiItemImpl(key ?: GuiItemImpl.DEFAULT_KEY, uuid ?: UUID.randomUUID())
    }

    override fun createUpdatableGuiItem(
        key: NamespacedKey?,
        uuid: UUID?,
    ): UpdatableGuiItem {
        return UpdatableGuiItemImpl(key ?: GuiItemImpl.DEFAULT_KEY, uuid ?: UUID.randomUUID())
    }

    override fun createMask(vararg mask: String): Mask {
        return MaskImpl(*mask)
    }

    override fun createPattern(vararg pattern: String): Pattern {
        return PatternImpl(*pattern)
    }

    override fun createChestGui(
        size: ChestGui.ChestGuiSize,
        parent: Gui?,
    ): ChestGui {
        return ChestGuiImpl(size, parent as? AbstractGui)
    }

    override fun createOutlinePane(
        slot: Slot,
        length: Int,
        height: Int,
        uuid: UUID?,
    ): OutlinePane {
        return OutlinePaneImpl(slot, length, height, uuid = uuid ?: UUID.randomUUID())
    }

    override fun createPaginatedPane(
        slot: Slot,
        length: Int,
        height: Int,
        uuid: UUID?,
    ): PaginatedPane {
        return PaginatedPaneImpl(slot, length, height, uuid = uuid ?: UUID.randomUUID())
    }

    override fun createStaticPane(
        slot: Slot,
        length: Int,
        height: Int,
        uuid: UUID?,
    ): StaticPane {
        return StaticPaneImpl(slot, length, height, uuid = uuid ?: UUID.randomUUID())
    }
}