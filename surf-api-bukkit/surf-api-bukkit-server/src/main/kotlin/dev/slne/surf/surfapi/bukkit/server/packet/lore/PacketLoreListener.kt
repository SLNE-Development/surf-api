package dev.slne.surf.surfapi.bukkit.server.packet.lore

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListener
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.annotation.ClientboundListener
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.annotation.ServerboundListener
import dev.slne.surf.surfapi.bukkit.api.packet.lore.SurfBukkitPacketLoreHandler
import dev.slne.surf.surfapi.bukkit.server.nms.toBukkit
import dev.slne.surf.surfapi.bukkit.server.nms.toNms
import dev.slne.surf.surfapi.core.api.util.*
import it.unimi.dsi.fastutil.objects.ObjectSet
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemLore
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin

/**
 * PacketLoreListener is a class that implements PacketListenerAbstract and is responsible for
 * handling the modification of lore on item stacks in packet events.
 */
@OptIn(NmsUseWithCaution::class)
object PacketLoreListener : PacketListener {
    private val loreHandlers =
        mutableObject2ObjectMapOf<NamespacedKey, SurfBukkitPacketLoreHandler>().synchronize()
    private val loreHandlersGlobal =
        mutableObject2ObjectMapOf<Plugin, ObjectSet<SurfBukkitPacketLoreHandler>>().synchronize()


    private const val LORE_PREFIX_STRING = "§q"
    private val lorePrefix = Component.literal(LORE_PREFIX_STRING)

    @ServerboundListener
    fun onPacketReceive(event: ServerboundSetCreativeModeSlotPacket) {
        makeCleanItemStack(event.itemStack())
    }

    @ClientboundListener
    fun onWindowItem(event: ClientboundContainerSetContentPacket) {
        for (item in event.items) {
            makeUpdatedItemStack(item)
        }
    }

    @ClientboundListener
    fun onContainerData(event: ClientboundContainerSetSlotPacket) {
        makeUpdatedItemStack(event.item)
    }

    @ClientboundListener
    fun onSetPlayerInventoryPacket(event: ClientboundSetPlayerInventoryPacket) {
        makeUpdatedItemStack(event.contents)
    }

    @ClientboundListener
    fun onSetCursorItemPacket(event: ClientboundSetCursorItemPacket) {
        makeUpdatedItemStack(event.contents)
    }

    private fun makeUpdatedItemStack(
        item: ItemStack,
    ): ItemStack {
        if (item.isEmpty) return item
        if (loreHandlers.isEmpty() && loreHandlersGlobal.isEmpty()) return item

        val bukkitStack = item.asBukkitMirror()
        val pdc = bukkitStack.persistentDataContainer
        val nmsLore = item.get(DataComponents.LORE)
        val lines = nmsLore?.lines
        val lore =
            lines?.mapTo(mutableObjectListOf(lines.size)) { it.toBukkit() } ?: emptyObjectList()

        loreHandlers.forEach { (identifier, handler) ->
            if (pdc.has(identifier)) {
                handler.handleLore(lore, pdc, bukkitStack)
            }
        }

        loreHandlersGlobal.forEach { (plugin, handlers) ->
            if (plugin.isEnabled) {
                handlers.forEach { it.handleLore(lore, pdc, bukkitStack) }
            }
        }

        val updatedNmsLore = ItemLore(
            lore.asSequence()
                .map { it.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE) }
                .map { it.toNms() }
                .map { lorePrefix.copy().append(it) }
                .toList()
        )

        item.set(DataComponents.LORE, updatedNmsLore)
        return item
    }

    private fun makeCleanItemStack(
        stack: ItemStack?,
    ): ItemStack? {
        if (stack == null) return null

        val updatedLore = stack.components.get(DataComponents.LORE)
            ?.lines?.filter { !it.string.startsWith(LORE_PREFIX_STRING) }
            ?: return stack

        stack.set(DataComponents.LORE, ItemLore(updatedLore))
        return stack
    }

    fun register(identifier: NamespacedKey, listener: SurfBukkitPacketLoreHandler) {
        loreHandlers.put(identifier, listener)
    }

    fun register(plugin: Plugin, listener: SurfBukkitPacketLoreHandler) {
        loreHandlersGlobal.computeIfAbsent(plugin) { mutableObjectSetOf() }.add(listener)
    }

    fun unregister(identifier: NamespacedKey) {
        loreHandlers.remove(identifier)
    }

    fun unregister(plugin: Plugin) {
        loreHandlersGlobal.remove(plugin)
    }
}
