package dev.slne.surf.surfapi.bukkit.api.packet;

import dev.slne.surf.surfapi.bukkit.api.SurfBukkitApi;
import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketApi;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface SurfBukkitPacketApi extends SurfCorePacketApi {

    void registerInteractListener(SurfBukkitInteractListener listener);

    static SurfBukkitPacketApi get() {
        return SurfBukkitApi.get().getPacketApi();
    }
}
