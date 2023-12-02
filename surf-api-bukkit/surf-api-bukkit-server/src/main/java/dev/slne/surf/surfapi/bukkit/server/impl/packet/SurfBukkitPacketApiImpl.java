package dev.slne.surf.surfapi.bukkit.server.impl.packet;

import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitInteractListener;
import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitPacketApi;
import dev.slne.surf.surfapi.core.server.impl.packet.SurfCorePacketApiImpl;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SurfBukkitPacketApiImpl extends SurfCorePacketApiImpl implements SurfBukkitPacketApi {
    /**
     * List of SurfBukkitInteractListener objects.
     * The list is synchronized to support concurrent access.
     * Use the registerInteractListener method to add an interact listener to the list.
     * Use the getInteractListeners method to retrieve the list of interact listeners.
     */
    private final List<SurfBukkitInteractListener> interactListeners = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void registerInteractListener(SurfBukkitInteractListener listener) {
        interactListeners.add(listener);
    }

    /**
     * Returns the list of SurfBukkitInteractListener objects.
     *
     * @return the list of SurfBukkitInteractListener objects
     */
    @ApiStatus.Internal
    public List<SurfBukkitInteractListener> getInteractListeners() {
        return interactListeners;
    }
}
