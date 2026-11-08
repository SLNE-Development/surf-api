package dev.slne.surf.api.paper.server.nms.v26_2.packet.lore

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.packet.listener.listener.PacketListener
import dev.slne.surf.api.paper.packet.listener.listener.annotation.ClientboundListener
import dev.slne.surf.api.paper.packet.listener.listener.annotation.ServerboundListener
import dev.slne.surf.api.paper.packet.lore.SurfPaperPacketLoreHandler
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.toBukkit
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.toNms
import dev.slne.surf.api.paper.util.namespacedKey
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectLists
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
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
@Suppress("ClassName")
object V26_2PacketLoreListener : PacketListener {
    private data class PriorityHandler(
        val handler: SurfPaperPacketLoreHandler,
        val priority: Short,
    )

    private val priorityComparator =
        Comparator<PriorityHandler> { a, b -> a.priority.compareTo(b.priority) }

    private val globalHandlersByPlugin =
        Object2ObjectLinkedOpenHashMap<Plugin, ObjectLinkedOpenHashSet<PriorityHandler>>()

    private val keyedHandlersByPlugin =
        Object2ObjectLinkedOpenHashMap<Plugin, Object2ObjectLinkedOpenHashMap<NamespacedKey, PriorityHandler>>()

    @Volatile
    private var keyedHandlersSnapshot: Map<NamespacedKey, PriorityHandler> = emptyMap()

    @Volatile
    private var globalHandlersSnapshot: Array<PriorityHandler> = emptyArray()

    private val ORIGINAL_LORE_KEY = namespacedKey("original_lore")
    private val ORIGINAL_LORE_KEY_STRING = ORIGINAL_LORE_KEY.asString()

    private fun hasAnyHandlers(): Boolean =
        keyedHandlersSnapshot.isNotEmpty() || globalHandlersSnapshot.isNotEmpty()

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

    @ServerboundListener
    fun onContainerClickPacket(
        event: ServerboundContainerClickPacket,
        player: ServerPlayer
    ): ServerboundContainerClickPacket {
        if (!hasAnyHandlers()) return event

        val container = player.containerMenu
        val currentStateId = container.stateId

        val brokenStateId = if (event.stateId() == currentStateId) {
            currentStateId - 1
        } else {
            event.stateId()
        }

        if (brokenStateId == event.stateId()) return event

        return ServerboundContainerClickPacket(
            event.containerId(),
            brokenStateId,
            event.slotNum(),
            event.buttonNum(),
            event.containerInput(),
            event.changedSlots(),
            event.carriedItem()
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

    @ClientboundListener
    fun onSystemChatPacket(event: ClientboundSystemChatPacket): ClientboundSystemChatPacket {
        if (!hasAnyHandlers()) return event

        val original = event.content()
        val transformed = transformChatComponent(original)

        if (transformed === original) return event

        return ClientboundSystemChatPacket(transformed, event.overlay())
    }

    @ClientboundListener
    fun onClientboundPlayerChatPacket(event: ClientboundPlayerChatPacket): ClientboundPlayerChatPacket {
        if (!hasAnyHandlers()) return event
        val unsignedContent = event.unsignedContent() ?: return event
        val transformed =transformChatComponent(unsignedContent)

        if (transformed === unsignedContent) return event

        return ClientboundPlayerChatPacket(
            event.globalIndex(),
            event.sender(),
            event.index(),
            event.signature(),
            event.body(),
            transformed,
            event.filterMask(),
            event.chatType()
        )
    }

    @ClientboundListener
    fun onClientboundDisguisedChatPacket(event: ClientboundDisguisedChatPacket): ClientboundDisguisedChatPacket {
        if (!hasAnyHandlers()) return event
        val message = event.message()
        val transformed = transformChatComponent(message)

        if (transformed === message) return event

        return ClientboundDisguisedChatPacket(transformed, event.chatType())
    }

    /**
     * Recursively walks the component tree and rebuilds only the branches that actually
     * contain a [HoverEvent.ShowItem] whose item lore is mutated by the registered handlers.
     *
     * Returns the same [component] reference if nothing changed, so the caller can short-circuit.
     */
    private fun transformChatComponent(component: MinecraftComponent): MinecraftComponent {
        val style = component.style
        val newStyle = transformStyle(style)

        val siblings = component.siblings
        var newSiblings: ObjectArrayList<MinecraftComponent>? = null

        for (i in siblings.indices) {
            val sibling = siblings[i]
            val transformedSibling = transformChatComponent(sibling)

            if (transformedSibling !== sibling && newSiblings == null) {
                newSiblings = ObjectArrayList(siblings.size)
                for (j in 0 until i) {
                    newSiblings.add(siblings[j])
                }
            }

            newSiblings?.add(transformedSibling)
        }

        if (newStyle === style && newSiblings == null) {
            return component
        }

        val copy = component.copy()
        if (newStyle !== style) {
            copy.style = newStyle
        }

        if (newSiblings != null) {
            val target = copy.siblings
            target.clear()
            target.addAll(newSiblings)
        }

        return copy
    }

    /**
     * Applies the lore handlers to the item carried by a [HoverEvent.ShowItem], if any.
     * Returns the original [style] reference when nothing changed.
     */
    private fun transformStyle(style: Style): Style {
        val hoverEvent = style.hoverEvent ?: return style
        if (hoverEvent !is HoverEvent.ShowItem) return style

        val template = hoverEvent.item
        val stack = ItemStack(template.item, template.count, template.components)
        val updated = makeUpdatedItemStack(stack)

        if (updated === stack) return style

        val newTemplate = ItemStackTemplate(
            updated.typeHolder(),
            updated.count,
            updated.componentsPatch
        )

        return style.withHoverEvent(HoverEvent.ShowItem(newTemplate))
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
         * We need Bukkit PDC for the current handler API, but only when there
         * are keyed handlers to consider. The original mirror is used to cheaply
         * determine whether any keyed handlers actually match.
         */
        val matchingKeyedHandlers = if (keyedSnapshot.isNotEmpty()) {
            val originalBukkitStack = original.asBukkitMirror()
            val originalPdc = originalBukkitStack.persistentDataContainer
            resolveMatchingKeyedHandlers(
                originalPdc.keys,
                keyedSnapshot
            )
        } else {
            ObjectLists.emptyList()
        }

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

        /*
         * Combine matching keyed handlers and global handlers, then sort by priority so that
         * smaller priority values run earlier and larger values run later — across both
         * keyed and global handlers.
         */
        val combined = ObjectArrayList<PriorityHandler>(matchingKeyedHandlers.size + globalSnapshot.size)
        combined.addAll(matchingKeyedHandlers)
        for (i in globalSnapshot.indices) {
            combined.add(globalSnapshot[i])
        }
        if (combined.size > 1) {
            combined.sortWith(priorityComparator)
        }

        for (i in combined.indices) {
            combined[i].handler.handleLore(mutableLore, pdc, bukkitStack)
        }

        val updatedLines = ObjectArrayList<MinecraftComponent>(mutableLore.size)
        for (i in mutableLore.indices) {
            val line =
                mutableLore[i].decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
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
        keyedSnapshot: Map<NamespacedKey, PriorityHandler>,
    ): List<PriorityHandler> {
        if (itemKeys.isEmpty() || keyedSnapshot.isEmpty()) {
            return emptyList()
        }

        var result: ObjectArrayList<PriorityHandler>? = null
        for (key in itemKeys) {
            val handler = keyedSnapshot[key] ?: continue

            if (result == null) {
                result = ObjectArrayList(2)
            }

            result.add(handler)
        }

        return result ?: emptyList()
    }

    fun register(plugin: Plugin, identifier: NamespacedKey, listener: SurfPaperPacketLoreHandler, priority: Short) {
        synchronized(this) {
            check(!keyedHandlersSnapshot.containsKey(identifier)) {
                "A PacketLore handler for $identifier is already registered!"
            }

            val handlers =
                keyedHandlersByPlugin.computeIfAbsent(plugin) { Object2ObjectLinkedOpenHashMap() }

            val priorityHandler = PriorityHandler(listener, priority)
            val previous = handlers.putIfAbsent(identifier, priorityHandler)
            check(previous == null) {
                "A PacketLore handler for $identifier is already registered for plugin ${plugin.name}!"
            }

            val newSnapshot = Object2ObjectLinkedOpenHashMap(keyedHandlersSnapshot)
            newSnapshot[identifier] = priorityHandler
            keyedHandlersSnapshot = newSnapshot
        }
    }

    fun register(plugin: Plugin, listener: SurfPaperPacketLoreHandler, priority: Short) {
        synchronized(this) {
            val handlers =
                globalHandlersByPlugin.computeIfAbsent(plugin) { ObjectLinkedOpenHashSet() }

            // duplicate-detection by handler identity, regardless of priority
            for (existing in handlers) {
                if (existing.handler === listener) {
                    error("A PacketLore handler identical to the provided one (${listener.javaClass.name}) is already registered for plugin ${plugin.name}!")
                }
            }

            handlers.add(PriorityHandler(listener, priority))
            rebuildGlobalHandlersSnapshot()
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
        val snapshot = ObjectArrayList<PriorityHandler>()

        globalHandlersByPlugin.object2ObjectEntrySet().fastForEach { (plugin, handlers) ->
            if (plugin.isEnabled) {
                snapshot.addAll(handlers)
            }
        }

        val array = snapshot.toTypedArray()
        if (array.size > 1) {
            java.util.Arrays.sort(array, priorityComparator)
        }
        globalHandlersSnapshot = array
    }
}
