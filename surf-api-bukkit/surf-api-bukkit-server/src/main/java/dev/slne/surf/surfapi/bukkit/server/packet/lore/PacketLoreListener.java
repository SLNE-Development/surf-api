package dev.slne.surf.surfapi.bukkit.server.packet.lore;

import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListener;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.annotation.ClientboundListener;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.annotation.ServerboundListener;
import dev.slne.surf.surfapi.bukkit.api.packet.lore.SurfBukkitPacketLoreHandler;
import io.papermc.paper.adventure.PaperAdventure;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextDecoration.State;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.item.component.ItemLore;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * PacketLoreListener is a class that implements PacketListenerAbstract and is responsible for
 * handling the modification of lore on item stacks in packet events.
 */
@ApiStatus.Internal
public final class PacketLoreListener implements PacketListener {

  /**
   * Represents an instance of the PacketLoreListener class. This variable is used to access the
   * PacketLoreListener class functionality. It is a static final variable, meaning it cannot be
   * changed or reassigned. This variable is initialized with a new instance of the
   * PacketLoreListener class.
   */
  public static final PacketLoreListener INSTANCE = new PacketLoreListener();

  /**
   * A map of lore handlers for modifying the lore of an item stack.
   */
  private final Object2ObjectMap<NamespacedKey, SurfBukkitPacketLoreHandler> loreHandlers = Object2ObjectMaps.synchronize(
      new Object2ObjectOpenHashMap<>());
  /**
   * A map of lore handlers for modifying the lore of an item stack.
   */
  private final Object2ObjectMap<Plugin, List<SurfBukkitPacketLoreHandler>> loreHandlersGlobal = Object2ObjectMaps.synchronize(
      new Object2ObjectOpenHashMap<>());
  /**
   * Private final variable representing the lore prefix for modifying the lore of an item stack.
   * <p>
   * The lore prefix is a {@link Component} object with the value "§q". It is used in conjunction
   * with other {@link SurfBukkitPacketLoreHandler} instances to modify the lore of an item stack.
   *
   * @see SurfBukkitPacketLoreHandler
   */
  private final net.minecraft.network.chat.MutableComponent lorePrefix;
  /**
   * Represents the prefix string used for lore modifications.
   * <p>
   * Lore modifications are handled by implementing the {@link SurfBukkitPacketLoreHandler}
   * interface. The lore handlers can modify the lore of an item stack by adding, removing , or
   * modifying existing lore components.
   * <p>
   * The lore prefix string is used to identify and manage the specific lore modifications made by
   * the lore handlers. It is appended to the beginning of each lore component added by the
   * handlers.
   */
  private final String lorePrefixString = "§q";

  /**
   * Represents a listener for packet events that modifies the lore of an item stack.
   */
  private PacketLoreListener() {
//    // We need to do this ugly hack to disable the warning message that is printed when
//    // legacy formatting is detected.
//    try {
//      Class<?> textComponentImpl = Class.forName("net.kyori.adventure.text.TextComponentImpl");
//      Field field = textComponentImpl.getDeclaredField("WARN_WHEN_LEGACY_FORMATTING_DETECTED");
//      Util.setStaticFinalField(field, false);
//    } catch (ReflectiveOperationException e) {
//      throw new RuntimeException(e);
//    }

    lorePrefix = net.minecraft.network.chat.Component.literal(lorePrefixString);
  }

  /**
   * This method is called when a packet is received. It checks if the packet type is
   * CREATIVE_INVENTORY_ACTION and then retrieves the item stack from the packet. The received item
   * stack is then passed to the getCleanItemStack() method, which removes any lore that starts with
   * the specified prefix. The modified item stack is then set back to the packet.
   *
   * @param event The PacketReceiveEvent object containing information about the received packet.
   */
  @ServerboundListener
  public void onPacketReceive(ServerboundSetCreativeModeSlotPacket event) {
    makeCleanItemStack(event.itemStack());
  }

  /**
   * Called when a packet is about to be sent. This method handles the modification of the packet
   * before sending it.
   *
   * @param event The PacketSendEvent containing information about the packet being sent.
   */
  @ClientboundListener
  public void onWindowItem(ClientboundContainerSetContentPacket event) {
    for (final net.minecraft.world.item.ItemStack item : event.getItems()) {
      makeUpdatedItemStack(item);
    }
  }

  @ClientboundListener
  public void onContainerData(ClientboundContainerSetSlotPacket event) {
    makeUpdatedItemStack(event.getItem());
  }

  private net.minecraft.world.item.ItemStack makeUpdatedItemStack(
      @NotNull net.minecraft.world.item.ItemStack item) {
    if (item.isEmpty()) {
      return item;
    }

    final org.bukkit.inventory.ItemStack bukkitStack = item.asBukkitMirror();
    final PersistentDataContainer pdc = bukkitStack.getItemMeta().getPersistentDataContainer();
    final ItemLore nmsLore = item.get(DataComponents.LORE);
    final List<Component> lore = nmsLore != null ? nmsLore.lines()
        .stream()
        .map(PaperAdventure::asAdventure)
        .collect(Collectors.toList())
        : new ArrayList<>();

    loreHandlers.forEach((identifier, handler) -> {
      if (pdc.has(identifier)) {
        handler.handleLore(lore, pdc, bukkitStack);
      }
    });

    loreHandlersGlobal.forEach((plugin, handlers) -> {
      if (plugin.isEnabled()) {
        handlers.forEach(handler -> handler.handleLore(lore, pdc, bukkitStack));
      }
    });

    final ItemLore updatedNmsLore = new ItemLore(lore.stream()
        .map(component -> component.decorationIfAbsent(TextDecoration.ITALIC, State.FALSE))
        .map(PaperAdventure::asVanilla)
        .map(loreComponent -> lorePrefix.copy().append(loreComponent))
        .collect(Collectors.toList()));

    item.set(DataComponents.LORE, updatedNmsLore);

    return item;
  }

  private net.minecraft.world.item.ItemStack makeCleanItemStack(
      net.minecraft.world.item.ItemStack stack) {
    if (stack == null) {
      return null;
    }

    final ItemLore lore = stack.getComponents().get(DataComponents.LORE);

    if (lore == null) {
      return stack;
    }

    final List<net.minecraft.network.chat.Component> filteredLores = lore.lines().stream()
        .filter(component -> !component.getString().startsWith(lorePrefixString))
        .toList();

    stack.set(DataComponents.LORE, new ItemLore(filteredLores));

    return stack;
  }

  /**
   * Registers a SurfBukkitPacketLoreHandler for a given NamespacedKey identifier.
   *
   * @param identifier The NamespacedKey identifier for the registered handler.
   * @param listener   The SurfBukkitPacketLoreHandler to register.
   */
  public void register(NamespacedKey identifier, SurfBukkitPacketLoreHandler listener) {
    loreHandlers.put(identifier, listener);
  }

  public void register(Plugin plugin, SurfBukkitPacketLoreHandler listener) {
    loreHandlersGlobal.computeIfAbsent(plugin, __ -> new ArrayList<>()).add(listener);
  }

  /**
   * Removes a registered lore handler with the specified identifier.
   *
   * @param identifier The identifier of the registered lore handler to be unregistered.
   */
  public void unregister(NamespacedKey identifier) {
    loreHandlers.remove(identifier);
  }

  public void unregister(Plugin plugin) {
    loreHandlersGlobal.remove(plugin);
  }
}
