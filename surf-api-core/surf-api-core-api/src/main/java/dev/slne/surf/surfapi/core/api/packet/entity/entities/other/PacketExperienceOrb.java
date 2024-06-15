package dev.slne.surf.surfapi.core.api.packet.entity.entities.other;

import java.util.UUID;
import org.spongepowered.math.vector.Vector3i;

public interface PacketExperienceOrb {

  int entityId();

  Vector3i position();

  short experience();

  boolean addViewer(UUID viewer);

  boolean removeViewer(UUID viewer);

  void despawn();
}
