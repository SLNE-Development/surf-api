package dev.slne.surf.surfapi.bukkit.server.impl.packet;

import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitPacketApi;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.SurfBukkitPacketEntityApi;
import dev.slne.surf.surfapi.bukkit.api.packet.lore.SurfBukkitPacketLoreHandler;
import dev.slne.surf.surfapi.bukkit.server.impl.packet.entity.SurfBukkitPacketEntityApiImpl;
import dev.slne.surf.surfapi.bukkit.server.packet.lore.PacketLoreListener;
import dev.slne.surf.surfapi.core.server.impl.packet.SurfCorePacketApiImpl;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import static com.google.common.base.Preconditions.*;

public class SurfBukkitPacketApiImpl extends SurfCorePacketApiImpl implements SurfBukkitPacketApi {

    private final SurfBukkitPacketEntityApiImpl packetEntityApi;

    public SurfBukkitPacketApiImpl() {
        packetEntityApi = new SurfBukkitPacketEntityApiImpl();
    }

    @Override
    public SurfBukkitPacketEntityApi getPacketEntityApi() {
        return packetEntityApi;
    }

    @Override
    public void registerPacketLoreListener(@NotNull NamespacedKey identifier, @NotNull SurfBukkitPacketLoreHandler listener) {
        checkNotNull(identifier, "identifier");
        checkNotNull(listener, "listener");

        PacketLoreListener.INSTANCE.register(identifier, listener);
    }

    @Override
    public void registerPacketLoreListenerGlobal(@NotNull Plugin plugin, @NotNull SurfBukkitPacketLoreHandler listener) {
        checkNotNull(plugin, "plugin");
        checkNotNull(listener, "listener");

        PacketLoreListener.INSTANCE.register(plugin, listener);
    }

    @Override
    public void unregisterPacketLoreListener(@NotNull Plugin plugin) {
        checkNotNull(plugin, "plugin");

        PacketLoreListener.INSTANCE.unregister(plugin);
    }

    @Override
    public void unregisterPacketLoreListener(@NotNull NamespacedKey identifier) {
        checkNotNull(identifier, "identifier");

        PacketLoreListener.INSTANCE.unregister(identifier);
    }
}
