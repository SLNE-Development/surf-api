package dev.slne.surf.surfapi.bukkit.server.impl.packet.entity;

import dev.slne.surf.surfapi.bukkit.api.packet.entity.SurfBukkitPacketEntityApi;
import dev.slne.surf.surfapi.core.server.impl.packet.SurfCorePacketEntityApiImpl;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.ApiStatus;


/**
 * Implementation of the {@link SurfBukkitPacketEntityApi} interface.
 * Provides methods for registering and unregistering interact listeners,
 * creating entities, deleting entities, and retrieving entities by ID or UUID.
 */
@ApiStatus.Internal
public class SurfBukkitPacketEntityApiImpl extends SurfCorePacketEntityApiImpl implements SurfBukkitPacketEntityApi {

    public SurfBukkitPacketEntityApiImpl() {
        super(new BukkitEntityIdProvider());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
