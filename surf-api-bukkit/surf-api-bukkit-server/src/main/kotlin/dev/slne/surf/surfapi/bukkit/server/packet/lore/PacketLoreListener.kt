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
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemLore
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap

/**
 * PacketLoreListener is a class that implements PacketListenerAbstract and is responsible for
 * handling the modification of lore on item stacks in packet events.
 */
@OptIn(NmsUseWithCaution::class)
object PacketLoreListener : PacketListener {
    private val loreHandlers = ConcurrentHashMap<NamespacedKey, SurfBukkitPacketLoreHandler>()
    private val loreHandlersGlobal = ConcurrentHashMap<Plugin, MutableSet<SurfBukkitPacketLoreHandler>>()

    private val ORIGINAL_LORE_KEY = key("original_lore")

    @ServerboundListener
    fun onPacketReceive(event: ServerboundSetCreativeModeSlotPacket) {
        makeCleanItemStack(event.itemStack())
    }

    @ClientboundListener
    fun onWindowItem(event: ClientboundContainerSetContentPacket): ClientboundContainerSetContentPacket {
        return ClientboundContainerSetContentPacket(
            event.containerId(),
            event.stateId(),
            event.items.map { makeUpdatedItemStack(it.copy()) },
            makeUpdatedItemStack(event.carriedItem().copy())
        )
    }

    @ClientboundListener
    fun onContainerData(event: ClientboundContainerSetSlotPacket): ClientboundContainerSetSlotPacket {
        return ClientboundContainerSetSlotPacket(
            event.containerId,
            event.stateId,
            event.slot,
            makeUpdatedItemStack(event.item.copy())
        )
    }

    @ClientboundListener
    fun onSetPlayerInventoryPacket(event: ClientboundSetPlayerInventoryPacket): ClientboundSetPlayerInventoryPacket {
        return ClientboundSetPlayerInventoryPacket(
            event.slot(),
            makeUpdatedItemStack(event.contents.copy())
        )
    }

    @ClientboundListener
    fun onSetCursorItemPacket(event: ClientboundSetCursorItemPacket): ClientboundSetCursorItemPacket {
        return ClientboundSetCursorItemPacket(makeUpdatedItemStack(event.contents.copy()))
    }

    private fun makeUpdatedItemStack(
        item: ItemStack,
    ): ItemStack {
        if (item.isEmpty) return item
        if (loreHandlers.isEmpty() && loreHandlersGlobal.isEmpty()) return item

        val bukkitStack = item.asBukkitMirror()
        val pdc = bukkitStack.persistentDataContainer
        val nmsLore = item.getOrDefault(DataComponents.LORE, ItemLore.EMPTY)
        val lines = nmsLore.lines
        val mutableLore = lines.mapTo(mutableObjectListOf(lines.size)) { it.toBukkit() }

        loreHandlers.forEach { (identifier, handler) ->
            if (pdc.has(identifier)) {
                handler.handleLore(mutableLore, pdc, bukkitStack)
            }
        }

        loreHandlersGlobal.forEach { (plugin, handlers) ->
            if (plugin.isEnabled) {
                handlers.forEach { it.handleLore(mutableLore, pdc, bukkitStack) }
            }
        }

        val updatedNmsLore = ItemLore(
            mutableLore.asSequence()
                .map { it.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE) }
                .map { it.toNms() }
                .toList()
        )

        item.set(DataComponents.LORE, updatedNmsLore)
        CustomData.update(DataComponents.CUSTOM_DATA, item) { tag ->
            tag.store(ORIGINAL_LORE_KEY.asString(), ItemLore.CODEC, nmsLore)
        }

        return item
    }

    private fun makeCleanItemStack(
        stack: ItemStack,
    ): ItemStack {
        CustomData.update(DataComponents.CUSTOM_DATA, stack) { tag ->
            val originalLore = tag.read(ORIGINAL_LORE_KEY.asString(), ItemLore.CODEC)
            originalLore.ifPresent { lore ->
                stack.set(DataComponents.LORE, lore)
                tag.remove(ORIGINAL_LORE_KEY.asString())
            }
        }

        return stack
    }

    fun register(identifier: NamespacedKey, listener: SurfBukkitPacketLoreHandler) {
        loreHandlers[identifier] = listener
    }

    fun register(plugin: Plugin, listener: SurfBukkitPacketLoreHandler) {
        loreHandlersGlobal.computeIfAbsent(plugin) { ConcurrentHashMap.newKeySet() }.add(listener)
    }

    fun unregister(identifier: NamespacedKey) {
        loreHandlers.remove(identifier)
    }

    fun unregister(plugin: Plugin) {
        loreHandlersGlobal.remove(plugin)
    }
}
