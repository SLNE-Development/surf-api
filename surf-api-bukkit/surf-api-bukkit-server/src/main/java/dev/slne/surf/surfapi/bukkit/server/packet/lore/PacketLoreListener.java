package dev.slne.surf.surfapi.bukkit.server.packet.lore;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import dev.slne.surf.surfapi.bukkit.api.packet.lore.SurfBukkitPacketLoreHandler;
import dev.slne.surf.surfapi.core.api.util.Util;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * PacketLoreListener is a class that implements PacketListenerAbstract and is responsible for
 * handling the modification of lore on item stacks in packet events.
 */
@ApiStatus.Internal
public final class PacketLoreListener extends PacketListenerAbstract {

  /**
   * Represents an instance of the PacketLoreListener class. This variable is used to access the
   * PacketLoreListener class functionality. It is a static final variable, meaning it cannot be
   * changed or reassigned. This variable is initialized with a new instance of the
   * PacketLoreListener class.
   */
  public static final PacketLoreListener INSTANCE = new PacketLoreListener();
  /**
   * Serializer used to convert components to plain text.
   */
  private static final PlainTextComponentSerializer PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.plainText();

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
  private final Component lorePrefix;
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
    super(PacketListenerPriority.LOWEST);

    // We need to do this ugly hack to disable the warning message that is printed when
    // legacy formatting is detected.
    try {
      Class<?> textComponentImpl = Class.forName("net.kyori.adventure.text.TextComponentImpl");
      Field field = textComponentImpl.getDeclaredField("WARN_WHEN_LEGACY_FORMATTING_DETECTED");
      Util.setStaticFinalField(field, false);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }

    lorePrefix = Component.text(lorePrefixString);
  }

  /**
   * This method is called when a packet is received. It checks if the packet type is
   * CREATIVE_INVENTORY_ACTION and then retrieves the item stack from the packet. The received item
   * stack is then passed to the getCleanItemStack() method, which removes any lore that starts with
   * the specified prefix. The modified item stack is then set back to the packet.
   *
   * @param event The PacketReceiveEvent object containing information about the received packet.
   */
  @Override
  public void onPacketReceive(@NotNull PacketReceiveEvent event) {
    if (event.getPacketType().equals(PacketType.Play.Client.CREATIVE_INVENTORY_ACTION)) {
      final WrapperPlayClientCreativeInventoryAction packet = new WrapperPlayClientCreativeInventoryAction(
          event);

      packet.setItemStack(getCleanItemStack(packet.getItemStack()));
    }
  }

  /**
   * Called when a packet is about to be sent. This method handles the modification of the packet
   * before sending it.
   *
   * @param event The PacketSendEvent containing information about the packet being sent.
   */
  @Override
  public void onPacketSend(@NotNull PacketSendEvent event) {
    PacketTypeCommon packetType = event.getPacketType();

    if (packetType.equals(PacketType.Play.Server.WINDOW_ITEMS)) {
      final WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event);
      final List<ItemStack> updatedItems = new ArrayList<>();

      for (ItemStack item : packet.getItems()) {
        if (item != null) {
          updatedItems.add(getUpdatedItemStack(item));
        }
      }

      packet.setItems(updatedItems);
    } else if (packetType.equals(PacketType.Play.Server.SET_SLOT)) {
      final WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);
      ItemStack item = packet.getItem();

      if (item != null) {
        packet.setItem(getUpdatedItemStack(item));
      }
    }
  }

  /**
   * Returns an updated version of the given ItemStack by modifying its lore components.
   *
   * @param item The ItemStack to be updated.
   * @return An updated version of the ItemStack.
   */
  private ItemStack getUpdatedItemStack(@NotNull ItemStack item) {
    final org.bukkit.inventory.ItemStack bukkitStack = SpigotConversionUtil.toBukkitItemStack(item)
        .clone();

    bukkitStack.editMeta(meta -> {
      final PersistentDataContainer pdc = meta.getPersistentDataContainer();
      final List<Component> lore = Optional.ofNullable(meta.lore()).map(ArrayList::new)
          .orElse(new ArrayList<>());

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

      final List<Component> parsed = lore.stream().map(lorePrefix::append).toList();
      meta.lore(parsed.isEmpty() ? null : parsed);
    });

    return SpigotConversionUtil.fromBukkitItemStack(bukkitStack);
  }

  /**
   * Returns a clean version of the given ItemStack by removing specific lore components. If the
   * input stack is null, null is returned.
   *
   * @param stack The ItemStack to be cleaned.
   * @return A clean version of the ItemStack.
   */
  @Contract("null -> null")
  private ItemStack getCleanItemStack(ItemStack stack) {
    if (stack == null) {
      return null;
    }

    org.bukkit.inventory.ItemStack bukkitItemStack = SpigotConversionUtil.toBukkitItemStack(stack);

    bukkitItemStack.editMeta(meta -> {
      final List<Component> lore = meta.lore();

      if (lore != null) {
        lore.removeIf(
            component -> PLAIN_TEXT_SERIALIZER.serialize(component).startsWith(lorePrefixString));

        if (lore.isEmpty()) {
          meta.lore(null);
        } else {
          meta.lore(lore);
        }
      }
    });

    return SpigotConversionUtil.fromBukkitItemStack(bukkitItemStack);
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
