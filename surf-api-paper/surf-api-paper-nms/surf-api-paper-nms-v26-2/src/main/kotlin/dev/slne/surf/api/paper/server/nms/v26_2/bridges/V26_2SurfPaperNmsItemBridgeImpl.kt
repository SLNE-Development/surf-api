package dev.slne.surf.api.paper.server.nms.v26_2.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsItemBridge
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.nms
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.toIdentifier
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.unwrap
import dev.slne.surf.api.paper.server.nms.v26_2.bridges.V26_2SurfPaperNmsItemBridgeImpl.CreativeOrderComparator.exactIndex
import dev.slne.surf.api.paper.server.nms.v26_2.bridges.V26_2SurfPaperNmsItemBridgeImpl.CreativeOrderComparator.indexOf
import dev.slne.surf.api.paper.server.nms.v26_2.bridges.V26_2SurfPaperNmsItemBridgeImpl.CreativeOrderComparator.itemTypeIndex
import dev.slne.surf.api.paper.server.nms.v26_2.bridges.V26_2SurfPaperNmsItemBridgeImpl.CreativeOrderComparator.materialIndex
import dev.slne.surf.api.paper.server.nms.v26_2.bridges.V26_2SurfPaperNmsItemBridgeImpl.CreativeOrderComparator.mayHaveComponentVariant
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStackLinkedSet
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType

@NmsUseWithCaution
@Suppress("ClassName")
class V26_2SurfPaperNmsItemBridgeImpl : SurfPaperNmsItemBridge {
    override fun setDefaultMaxStackSize(item: ItemType, maxStackSize: Int) {
        require(maxStackSize in 1..100) { "Max stack size must be between 1 and 100" }

        val nmsItem = item.nms
        val updatedComponents = DataComponentMap.builder()
            .addAll(nmsItem.components())
            .set(DataComponents.MAX_STACK_SIZE, maxStackSize)
            .build()

        @Suppress("DEPRECATION")
        nmsItem.builtInRegistryHolder().bindComponents(updatedComponents)
    }

    override fun getCreativeSearchItemOrderComparator(): Comparator<ItemStack?> {
        return CreativeOrderComparator
    }

    /**
     * A [Comparator] that orders [ItemStack]s according to the order in which they appear in the
     * creative mode search tab.
     *
     * Items with component-based variants (e.g. potions, enchanted books) are resolved using an
     * exact NMS-level lookup so that each variant is placed at its dedicated creative-tab position.
     * All other items fall back to a per-[Material] index derived from the first occurrence of the
     * corresponding [Item] type in the search tab.
     *
     * Items that are not present in the creative search tab at all receive an index of
     * [Int.MAX_VALUE] and are therefore sorted to the end.
     */
    object CreativeOrderComparator : Comparator<ItemStack?> {
        /**
         * A map from NMS [net.minecraft.world.item.ItemStack] to its position in the creative
         * search tab, keyed by both item type **and** data components so that component-variant
         * items (e.g. a specific potion effect) are resolved exactly.
         *
         * Populated lazily on first access from [CreativeModeTabs.searchTab].
         */
        private lateinit var exactIndex: Object2IntOpenCustomHashMap<net.minecraft.world.item.ItemStack>

        /**
         * A map from NMS [Item] to the position of its **first** occurrence in the creative search
         * tab.
         *
         * Used as a fast fallback when an exact component-level match is not required or not found.
         * The index stored here corresponds to the earliest slot at which any stack of that item
         * type appears in [CreativeModeTabs.searchTab].
         *
         * Populated lazily on first access.
         */
        private lateinit var itemTypeIndex: Reference2IntOpenHashMap<Item>

        /**
         * A map from Bukkit [Material] to the creative-tab index of that material, derived from
         * [itemTypeIndex].
         *
         * Only materials for which [Material.isItem] is `true` are included. The index is resolved
         * by converting the material's namespaced key to a NMS `ResourceLocation`
         * and looking up the corresponding [Item] in [BuiltInRegistries.ITEM].
         *
         * Populated lazily on first access.
         */
        private lateinit var materialIndex: Object2IntOpenHashMap<Material>


        fun init() {
            val params = CreativeModeTab.ItemDisplayParameters(
                FeatureFlags.DEFAULT_FLAGS,
                true,
                MinecraftServer.getServer().registryAccess()
            )

            for (tab in BuiltInRegistries.CREATIVE_MODE_TAB) {
                if (tab.type != CreativeModeTab.Type.CATEGORY) continue
                tab.buildContents(params)
            }
            for (tab in BuiltInRegistries.CREATIVE_MODE_TAB) {
                if (tab.type == CreativeModeTab.Type.CATEGORY) continue
                tab.buildContents(params)
            }

            val orderedItems = CreativeModeTabs.searchTab().displayItems

            this.exactIndex = Object2IntOpenCustomHashMap(
                orderedItems.size,
                ItemStackLinkedSet.TYPE_AND_TAG
            ).apply {
                defaultReturnValue(Int.MAX_VALUE)
                var i = 0
                for (item in orderedItems) put(item, i++)
            }

            this.itemTypeIndex = Reference2IntOpenHashMap<Item>(Material.entries.size).apply {
                defaultReturnValue(Int.MAX_VALUE)

                for ((i, stack) in orderedItems.withIndex()) {
                    val item = stack.item
                    putIfAbsent(item, i)
                }
            }

            this.materialIndex = Object2IntOpenHashMap<Material>(Registry.ITEM.size()).apply {
                defaultReturnValue(Int.MAX_VALUE)

                for (itemType in Registry.ITEM) {
                    val key = itemType.key.toIdentifier()
                    val item = BuiltInRegistries.ITEM.getValue(key)

                    @Suppress("DEPRECATION")
                    put(itemType.asMaterial(), itemTypeIndex.getInt(item))
                }
            }
        }

        /**
         * Compares two [ItemStack]s by their position in the creative search tab.
         *
         * @param a the first item stack to compare.
         * @param b the second item stack to compare.
         * @return a negative integer if [a] appears before [b] in the creative search tab,
         *   zero if their positions are equal, or a positive integer if [a] appears after [b].
         */
        override fun compare(a: ItemStack?, b: ItemStack?): Int {
            val aIndex = indexOf(a)
            val bIndex = indexOf(b)

            return aIndex.compareTo(bIndex)
        }

        /**
         * Returns the creative search tab index for the given [stack].
         *
         * If the stack's [Material] may have component-specific variants (see
         * [mayHaveComponentVariant]), an exact NMS-level lookup against [exactIndex] is attempted
         * first. If no exact match is found the result falls back to the material-level index from
         * [materialIndex].
         *
         * @param stack the item stack whose index should be determined.
         * @return the zero-based position in the creative search tab, or [Int.MAX_VALUE] if the
         *   stack is not present in the tab.
         */
        private fun indexOf(stack: ItemStack?): Int {
            if (stack == null) return Int.MAX_VALUE

            val type = stack.type
            val byMaterial = materialIndex.getInt(type)

            if (mayHaveComponentVariant(type)) {
                val nms = stack.unwrap()
                val exact = exactIndex.getInt(nms)
                if (exact != Int.MAX_VALUE) return exact
            }

            return byMaterial
        }

        /**
         * Returns `true` if [material] can have component-based variants that each receive a
         * distinct position in the creative search tab.
         *
         * Items that return `true` here are subject to an exact NMS-level comparison in
         * [indexOf] so that, for example, individual potion effects or enchantment combinations
         * are ordered correctly rather than being grouped together at the material's generic
         * position.
         *
         * @param material the material to check.
         * @return `true` if the material may have component variants, `false` otherwise.
         */
        private fun mayHaveComponentVariant(material: Material) = when (material) {
            Material.POTION,
            Material.SPLASH_POTION,
            Material.LINGERING_POTION,
            Material.TIPPED_ARROW,
            Material.ENCHANTED_BOOK,
            Material.FIREWORK_ROCKET,
            Material.PAINTING,
            Material.SUSPICIOUS_STEW,
            Material.OMINOUS_BOTTLE,
            Material.LIGHT,
            Material.TEST_BLOCK,
            Material.GOAT_HORN -> true

            else -> false
        }
    }
}
