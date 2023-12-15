package dev.slne.surf.surfapi.bukkit.api.packet.lore;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.List;

/**
 * Represents a handler for modifying the lore of an item stack.
 */
@FunctionalInterface
public interface SurfBukkitPacketLoreHandlerSimple extends SurfBukkitPacketLoreHandler {

    /**
     * {@inheritDoc}
     *
     * @param loreToDisplay {@inheritDoc}
     * @param dataContainer {@inheritDoc}
     * @param itemStack     {@inheritDoc}
     */
    @Override
    default void handleLore(List<Component> loreToDisplay, PersistentDataContainer dataContainer, ItemStack itemStack) {
        handleLore(loreToDisplay);
    }

    /**
     * Handles the modification of the lore of an item stack.
     *
     * @param loreToDisplay The list of lore components to display on the item stack. Normally, you just add your own
     *                      lore to this list but as this list also contains the lore of other lore handlers and the
     *                      "real" lore of the item stack, you can also remove lore from this list or add your lore
     *                      at the beginning of the list.
     */
    void handleLore(List<Component> loreToDisplay);
}
