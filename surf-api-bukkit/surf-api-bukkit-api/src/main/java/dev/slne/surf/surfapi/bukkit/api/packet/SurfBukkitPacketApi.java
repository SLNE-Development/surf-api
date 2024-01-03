package dev.slne.surf.surfapi.bukkit.api.packet;

import dev.slne.surf.surfapi.bukkit.api.SurfBukkitApi;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.SurfBukkitPacketEntityApi;
import dev.slne.surf.surfapi.bukkit.api.packet.lore.SurfBukkitPacketLoreHandler;
import dev.slne.surf.surfapi.bukkit.api.packet.lore.SurfBukkitPacketLoreHandlerSimple;
import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketApi;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;


/**
 * SurfBukkitPacketApi interface extends SurfCorePacketApi interface
 * and provides additional methods for packet handling in Bukkit.
 * It allows registering and unregistering packet lore listeners, interacting with entities, and creating entities.
 */
@ApiStatus.NonExtendable
public interface SurfBukkitPacketApi extends SurfCorePacketApi {

    /**
     * Retrieves the SurfBukkitPacketEntityApi instance.
     *
     * @return surfBukkitPacketEntityApi instance
     */
    @Override
    SurfBukkitPacketEntityApi getPacketEntityApi();

    /**
     * Registers a listener for modifying the lore of an item stack.
     *
     * @param identifier The identifier for the item to listen for. Must not be null.
     * @param listener   The listener for modifying the lore of an item stack.
     */
    void registerPacketLoreListener(@NotNull NamespacedKey identifier, @NotNull SurfBukkitPacketLoreHandler listener);

    /**
     * Registers a packet lore listener with the specified identifier.
     *
     * @param identifier The identifier for the item to listen for. Must not be null.
     * @param listener   The packet lore listener to register. Must not be null.
     */
    default void registerPacketLoreListener(@NotNull NamespacedKey identifier,
                                            @NotNull SurfBukkitPacketLoreHandlerSimple listener) {
        registerPacketLoreListener(identifier, (SurfBukkitPacketLoreHandler) listener);
    }

    /**
     * Unregisters a packet lore listener identified by the given identifier.
     *
     * @param identifier the identifier of the packet lore listener to unregister
     */
    void unregisterPacketLoreListener(@NotNull NamespacedKey identifier);

    /**
     * Retrieves the SurfBukkitPacketApi instance.
     *
     * @return the SurfBukkitPacketApi instance
     */
    static SurfBukkitPacketApi get() {
        return SurfBukkitApi.get().getPacketApi();
    }
}
