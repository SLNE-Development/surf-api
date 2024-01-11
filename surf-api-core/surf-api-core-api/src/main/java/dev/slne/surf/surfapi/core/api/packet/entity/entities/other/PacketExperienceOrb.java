package dev.slne.surf.surfapi.core.api.packet.entity.entities.other;

import org.spongepowered.math.vector.Vector3i;

import java.util.UUID;

public interface PacketExperienceOrb {

    int entityId();

    Vector3i position();

    short experience();

    boolean addViewer(UUID viewer);

    boolean removeViewer(UUID viewer);

    void despawn();
}
