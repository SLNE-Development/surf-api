package dev.slne.surf.surfapi.bukkit.server.packet.lore

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListener
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.annotation.ClientboundListener
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.annotation.ServerboundListener
import dev.slne.surf.surfapi.bukkit.api.packet.lore.SurfBukkitPacketLoreHandler
import dev.slne.surf.surfapi.bukkit.api.util.key
import dev.slne.surf.surfapi.bukkit.server.nms.toBukkit
import dev.slne.surf.surfapi.bukkit.server.nms.toNms
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemLore
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin
import net.minecraft.network.chat.Component as MinecraftComponent

/**
 * PacketLoreListener is a class that implements PacketListenerAbstract and is responsible for
 * handling the modification of lore on item stacks in packet events.
 */
@OptIn(NmsUseWithCaution::class)
object PacketLoreListener : PacketListener {
    private val globalHandlersByPlugin =
        Object2ObjectLinkedOpenHashMap<Plugin, ObjectLinkedOpenHashSet<SurfBukkitPacketLoreHandler>>()

    private val keyedHandlersByPlugin =
        Object2ObjectLinkedOpenHashMap<Plugin, Object2ObjectLinkedOpenHashMap<NamespacedKey, SurfBukkitPacketLoreHandler>>()

    @Volatile
    private var keyedHandlersSnapshot: Map<NamespacedKey, SurfBukkitPacketLoreHandler> = emptyMap()

    @Volatile
    private var globalHandlersSnapshot: Array<SurfBukkitPacketLoreHandler> = emptyArray()

    private val ORIGINAL_LORE_KEY = key("original_lore")
    private val ORIGINAL_LORE_KEY_STRING = ORIGINAL_LORE_KEY.asString()

    private fun hasAnyHandlers(): Boolean = keyedHandlersSnapshot.isNotEmpty() || globalHandlersSnapshot.isNotEmpty()

    @ServerboundListener
    fun onPacketReceive(event: ServerboundSetCreativeModeSlotPacket) {
        makeCleanItemStack(event.itemStack())
    }

    @ClientboundListener
    fun onWindowItem(event: ClientboundContainerSetContentPacket): ClientboundContainerSetContentPacket {
        if (!hasAnyHandlers()) return event

        val sourceItems = event.items
        val updatedItems = ObjectArrayList<ItemStack>(sourceItems.size)

        var changed = false

        for (i in sourceItems.indices) {
            val original = sourceItems[i]
            val updated = makeUpdatedItemStack(original)

            if (updated !== original) {
                changed = true
            }

            updatedItems.add(updated)
        }

        val originalCarried = event.carriedItem()
        val updatedCarried = makeUpdatedItemStack(originalCarried)

        if (updatedCarried !== originalCarried) {
            changed = true
        }

        if (!changed) {
            return event
        }

        return ClientboundContainerSetContentPacket(
            event.containerId(),
            event.stateId(),
            updatedItems,
            updatedCarried
        )
    }

    @ClientboundListener
    fun onSetSlotPacket(event: ClientboundContainerSetSlotPacket): ClientboundContainerSetSlotPacket {
        val original = event.item
        val updated = makeUpdatedItemStack(original)

        if (updated === original) {
            return event
        }

        return ClientboundContainerSetSlotPacket(
            event.containerId,
            event.stateId,
            event.slot,
            updated
        )
    }

    @ClientboundListener
    fun onSetPlayerInventoryPacket(event: ClientboundSetPlayerInventoryPacket): ClientboundSetPlayerInventoryPacket {
        val original = event.contents
        val updated = makeUpdatedItemStack(original)

        if (updated === original) {
            return event
        }

        return ClientboundSetPlayerInventoryPacket(
            event.slot(),
            updated
        )
    }

    @ClientboundListener
    fun onSetCursorItemPacket(event: ClientboundSetCursorItemPacket): ClientboundSetCursorItemPacket {
        val original = event.contents
        val updated = makeUpdatedItemStack(original)

        if (updated === original) {
            return event
        }

        return ClientboundSetCursorItemPacket(updated)
    }

    /**
     * Returns the original item if:
     * - item is empty
     * - no handlers exist
     * - no keyed handler matches and no global handler exists
     * - handlers run but lore result is identical
     *
     * Only copies the stack if at least one handler will actually run.
     */
    private fun makeUpdatedItemStack(
        original: ItemStack,
    ): ItemStack {
        if (original.isEmpty) return original

        // One volatile read
        val keyedSnapshot = keyedHandlersSnapshot
        val globalSnapshot = globalHandlersSnapshot

        if (keyedSnapshot.isEmpty() && globalSnapshot.isEmpty()) {
            return original
        }

        /*
         * We need Bukkit PDC for the current handler API.
         * But we only use the original mirror to cheaply determine
         * whether any keyed handlers actually match.
         */
        val originalBukkitStack = original.asBukkitMirror()
        val originalPdc = originalBukkitStack.persistentDataContainer

        val matchingKeyedHandlers = resolveMatchingKeyedHandlers(
            originalPdc.keys,
            keyedSnapshot
        )

        if (matchingKeyedHandlers.isEmpty() && globalSnapshot.isEmpty()) {
            return original
        }

        /*
        * From here on, we know that at least one handler will run.
        * Only now create a copy.
        */
        val item = original.copy()
        val bukkitStack = item.asBukkitMirror()
        val pdc = bukkitStack.persistentDataContainer

        val originalLore = item.getOrDefault(DataComponents.LORE, ItemLore.EMPTY)
        val originalLines = originalLore.lines

        val mutableLore = originalLines.mapTo(
            ObjectArrayList(originalLines.size)
        ) { it.toBukkit() }

        for (i in matchingKeyedHandlers.indices) {
            matchingKeyedHandlers[i].handleLore(mutableLore, pdc, bukkitStack)
        }

        for (i in globalSnapshot.indices) {
            globalSnapshot[i].handleLore(mutableLore, pdc, bukkitStack)
        }

        val updatedLines = ObjectArrayList<MinecraftComponent>(mutableLore.size)
        for (i in mutableLore.indices) {
            val line = mutableLore[i].decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
            updatedLines.add(line.toNms())
        }

        val updatedLore = ItemLore(updatedLines)

        if (updatedLore == originalLore) {
            return original
        }

        item.set(DataComponents.LORE, updatedLore)
        CustomData.update(DataComponents.CUSTOM_DATA, item) { tag ->
            if (!tag.contains(ORIGINAL_LORE_KEY_STRING)) {
                tag.store(ORIGINAL_LORE_KEY_STRING, ItemLore.CODEC, originalLore)
            }
        }

        return item
    }

    private fun makeCleanItemStack(
        stack: ItemStack,
    ): ItemStack {
        if (stack.isEmpty) {
            return stack
        }

        val customData = stack.get(DataComponents.CUSTOM_DATA) ?: return stack
        if (!customData.contains(ORIGINAL_LORE_KEY_STRING)) {
            return stack
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack) { tag ->
            val originalLore = tag.read(ORIGINAL_LORE_KEY_STRING, ItemLore.CODEC)
            originalLore.ifPresent { lore ->
                stack.set(DataComponents.LORE, lore)
                tag.remove(ORIGINAL_LORE_KEY_STRING)
            }
        }

        return stack
    }

    private fun resolveMatchingKeyedHandlers(
        itemKeys: Set<NamespacedKey>,
        keyedSnapshot: Map<NamespacedKey, SurfBukkitPacketLoreHandler>,
    ): List<SurfBukkitPacketLoreHandler> {
        if (itemKeys.isEmpty() || keyedSnapshot.isEmpty()) {
            return emptyList()
        }

        var result: ObjectArrayList<SurfBukkitPacketLoreHandler>? = null
        for (key in itemKeys) {
            val handler = keyedSnapshot[key] ?: continue

            if (result == null) {
                result = ObjectArrayList(2)
            }

            result.add(handler)
        }

        return result ?: emptyList()
    }

    fun register(plugin: Plugin, identifier: NamespacedKey, listener: SurfBukkitPacketLoreHandler) {
        synchronized(this) {
            check(!keyedHandlersSnapshot.containsKey(identifier)) {
                "A PacketLore handler for $identifier is already registered!"
            }

            val handlers = keyedHandlersByPlugin.computeIfAbsent(plugin) { Object2ObjectLinkedOpenHashMap() }

            val previous = handlers.putIfAbsent(identifier, listener)
            check(previous == null) {
                "A PacketLore handler for $identifier is already registered for plugin ${plugin.name}!"
            }

            val newSnapshot = Object2ObjectLinkedOpenHashMap(keyedHandlersSnapshot)
            newSnapshot[identifier] = listener
            keyedHandlersSnapshot = newSnapshot
        }
    }

    fun register(plugin: Plugin, listener: SurfBukkitPacketLoreHandler) {
        synchronized(this) {
            val handlers = globalHandlersByPlugin.computeIfAbsent(plugin) { ObjectLinkedOpenHashSet() }
            if (handlers.add(listener)) {
                rebuildGlobalHandlersSnapshot()
            } else {
                error("A PacketLore handler identical to the provided one (${listener.javaClass.name}) is already registered for plugin ${plugin.name}!")
            }
        }
    }

    fun unregister(identifier: NamespacedKey) {
        synchronized(this) {
            if (!keyedHandlersSnapshot.containsKey(identifier)) {
                return
            }

            var emptyPlugin: Plugin? = null

            for ((plugin, handlers) in keyedHandlersByPlugin) {
                if (handlers.remove(identifier) != null) {
                    if (handlers.isEmpty()) {
                        emptyPlugin = plugin
                    }
                    break
                }
            }

            if (emptyPlugin != null) {
                keyedHandlersByPlugin.remove(emptyPlugin)
            }

            val newSnapshot = Object2ObjectLinkedOpenHashMap(keyedHandlersSnapshot)
            newSnapshot.remove(identifier)
            keyedHandlersSnapshot = newSnapshot
        }
    }

    fun unregister(plugin: Plugin) {
        synchronized(this) {
            val removedGlobal = globalHandlersByPlugin.remove(plugin) != null
            if (removedGlobal) {
                rebuildGlobalHandlersSnapshot()
            }

            val removedKeyed = keyedHandlersByPlugin.remove(plugin)
            if (removedKeyed != null) {
                val newSnapshot = Object2ObjectLinkedOpenHashMap(keyedHandlersSnapshot)
                for (identifier in removedKeyed.keys) {
                    newSnapshot.remove(identifier)
                }
                keyedHandlersSnapshot = newSnapshot
            }
        }
    }

    private fun rebuildGlobalHandlersSnapshot() {
        val snapshot = ObjectArrayList<SurfBukkitPacketLoreHandler>()

        globalHandlersByPlugin.object2ObjectEntrySet().fastForEach { (plugin, handlers) ->
            if (plugin.isEnabled) {
                snapshot.addAll(handlers)
            }
        }

        globalHandlersSnapshot = snapshot.toTypedArray()
    }
}
