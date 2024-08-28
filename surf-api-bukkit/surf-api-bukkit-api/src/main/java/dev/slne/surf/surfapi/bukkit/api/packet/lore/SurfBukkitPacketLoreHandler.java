package dev.slne.surf.surfapi.bukkit.api.packet.lore;

import io.papermc.paper.persistence.PersistentDataContainerView;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

/**
 * Represents a handler for modifying the lore of an item stack.
 */
@FunctionalInterface
public interface SurfBukkitPacketLoreHandler {

  /**
   * Handles the modification of the lore of an item stack.
   *
   * @param loreToDisplay The list of lore components to display on the item stack. Normally, you
   *                      just add your own lore to this list but as this list also contains the
   *                      lore of other lore handlers and the "real" lore of the item stack, you can
   *                      also remove lore from this list or add your lore at the beginning of the
   *                      list.
   * @param dataContainer The persistent data container associated with the item stack.
   * @param itemStack     The item stack to modify.
   */
  void handleLore(List<Component> loreToDisplay, PersistentDataContainerView dataContainer,
      ItemStack itemStack);
}
