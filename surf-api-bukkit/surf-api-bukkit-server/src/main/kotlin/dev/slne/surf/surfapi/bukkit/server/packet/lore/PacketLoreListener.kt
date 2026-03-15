package dev.slne.surf.surfapi.bukkit.server.packet.lore

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListener
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.annotation.ClientboundListener
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.annotation.ServerboundListener
import dev.slne.surf.surfapi.bukkit.api.packet.lore.SurfBukkitPacketLoreHandler
import dev.slne.surf.surfapi.bukkit.api.util.key
import dev.slne.surf.surfapi.bukkit.server.nms.toBukkit
import dev.slne.surf.surfapi.bukkit.server.nms.toNms
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemLore
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap
import net.kyori.adventure.text.Component as AdventureComponent
import net.minecraft.network.chat.Component as MinecraftComponent

/**
 * PacketLoreListener is a class that implements PacketListenerAbstract and is responsible for
 * handling the modification of lore on item stacks in packet events.
 */
@OptIn(NmsUseWithCaution::class)
object PacketLoreListener : PacketListener {
    private val loreHandlers = ConcurrentHashMap<NamespacedKey, SurfBukkitPacketLoreHandler>()
    private val loreHandlersGlobal = ConcurrentHashMap<Plugin, MutableSet<SurfBukkitPacketLoreHandler>>()

    @Volatile
    private var loreHandlerSnapshot: Array<Map.Entry<NamespacedKey, SurfBukkitPacketLoreHandler>> = emptyArray()

    @Volatile
    private var loreHandlerGlobalSnapshot: Array<Map.Entry<Plugin, MutableSet<SurfBukkitPacketLoreHandler>>> =
        emptyArray()

    private val ORIGINAL_LORE_KEY = key("original_lore")
    private val ORIGINAL_LORE_KEY_STRING = ORIGINAL_LORE_KEY.asString()

    private val ITALIC_DECORATION = TextDecoration.ITALIC
    private val ITALIC_STATE_FALSE = TextDecoration.State.FALSE

    private fun hasAnyHandlers(): Boolean = loreHandlerSnapshot.isNotEmpty() || loreHandlerGlobalSnapshot.isNotEmpty()

    @ServerboundListener
    fun onPacketReceive(event: ServerboundSetCreativeModeSlotPacket) {
        makeCleanItemStack(event.itemStack())
    }

    @ClientboundListener
    fun onWindowItem(event: ClientboundContainerSetContentPacket): ClientboundContainerSetContentPacket {
        if (!hasAnyHandlers()) return event

        val items = event.items
        val updatedItems = ObjectArrayList<ItemStack>(items.size)
        for (i in items.indices) {
            updatedItems.add(makeUpdatedItemStack(items[i].copy()))
        }

        return ClientboundContainerSetContentPacket(
            event.containerId(),
            event.stateId(),
            items,
            makeUpdatedItemStack(event.carriedItem().copy())
        )
    }

    @ClientboundListener
    fun onSetSlotPacket(event: ClientboundContainerSetSlotPacket): ClientboundContainerSetSlotPacket {
        if (!hasAnyHandlers()) return event

        return ClientboundContainerSetSlotPacket(
            event.containerId,
            event.stateId,
            event.slot,
            makeUpdatedItemStack(event.item.copy())
        )
    }

    @ClientboundListener
    fun onSetPlayerInventoryPacket(event: ClientboundSetPlayerInventoryPacket): ClientboundSetPlayerInventoryPacket {
        if (!hasAnyHandlers()) return event

        return ClientboundSetPlayerInventoryPacket(
            event.slot(),
            makeUpdatedItemStack(event.contents.copy())
        )
    }

    @ClientboundListener
    fun onSetCursorItemPacket(event: ClientboundSetCursorItemPacket): ClientboundSetCursorItemPacket {
        if (!hasAnyHandlers()) return event

        return ClientboundSetCursorItemPacket(makeUpdatedItemStack(event.contents.copy()))
    }

    private fun makeUpdatedItemStack(
        item: ItemStack,
    ): ItemStack {
        if (item.isEmpty) return item

        // One volatile read
        val handlerEntries = loreHandlerSnapshot
        val globalEntries = loreHandlerGlobalSnapshot

        val nmsLore = item.getOrDefault(DataComponents.LORE, ItemLore.EMPTY)
        val lines = nmsLore.lines

        val mutableLore = mutableObjectListOf<AdventureComponent>(lines.size)
        for (i in lines.indices) {
            mutableLore.add(lines[i].toBukkit())
        }

        val bukkitStack = item.asBukkitMirror()
        val pdc = bukkitStack.persistentDataContainer

        var anyHandlerRan = false
        if (handlerEntries.isNotEmpty()) {
            for ((key, handler) in handlerEntries) {
                if (pdc.has(key)) {
                    handler.handleLore(mutableLore, pdc, bukkitStack)
                    anyHandlerRan = true
                }
            }
        }

        if (globalEntries.isNotEmpty()) {
            for ((plugin, handlers) in globalEntries) {
                if (plugin.isEnabled) {
                    for (handler in handlers) {
                        handler.handleLore(mutableLore, pdc, bukkitStack)
                        anyHandlerRan = true
                    }
                }
            }
        }

        if (!anyHandlerRan) return item

        val updatedLines = ObjectArrayList<MinecraftComponent>(mutableLore.size)
        for (i in mutableLore.indices) {
            val component = mutableLore[i]
            updatedLines.add(
                component.decorationIfAbsent(ITALIC_DECORATION, ITALIC_STATE_FALSE).toNms()
            )
        }

        val updatedNmsLore = ItemLore(updatedLines)

        if (updatedNmsLore == nmsLore) {
            return item
        }

        item.set(DataComponents.LORE, updatedNmsLore)
        CustomData.update(DataComponents.CUSTOM_DATA, item) { tag ->
            tag.store(ORIGINAL_LORE_KEY_STRING, ItemLore.CODEC, nmsLore)
        }

        return item
    }

    private fun makeCleanItemStack(
        stack: ItemStack,
    ): ItemStack {
        CustomData.update(DataComponents.CUSTOM_DATA, stack) { tag ->
            val originalLore = tag.read(ORIGINAL_LORE_KEY_STRING, ItemLore.CODEC)
            originalLore.ifPresent { lore ->
                stack.set(DataComponents.LORE, lore)
                tag.remove(ORIGINAL_LORE_KEY_STRING)
            }
        }

        return stack
    }

    private fun rebuildSnapshots() {
        loreHandlerSnapshot = loreHandlers.entries.toTypedArray()
        loreHandlerGlobalSnapshot = loreHandlersGlobal.entries.toTypedArray()
    }

    fun register(identifier: NamespacedKey, listener: SurfBukkitPacketLoreHandler) {
        loreHandlers[identifier] = listener
        rebuildSnapshots()
    }

    fun register(plugin: Plugin, listener: SurfBukkitPacketLoreHandler) {
        loreHandlersGlobal.computeIfAbsent(plugin) { ConcurrentHashMap.newKeySet() }.add(listener)
        rebuildSnapshots()
    }

    fun unregister(identifier: NamespacedKey) {
        loreHandlers.remove(identifier)
        rebuildSnapshots()
    }

    fun unregister(plugin: Plugin) {
        loreHandlersGlobal.remove(plugin)
        rebuildSnapshots()
    }
}
