package dev.slne.surf.surfapi.core.api.packet.entity.entities.basic;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Shootable;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketFishingHook extends PacketEntity<PacketFishingHook>, Shootable, Spawnable {

  int HOOKED_ENTITY_ID_INDEX = 8, IS_CATCHABLE_INDEX = 9;

  int hookedEntityId();

  /**
   * @param hookedEntityId the hooked entity id - set to {@code 0} if there is no hooked entity id
   */
  void hookedEntityId(int hookedEntityId);

  boolean catchable();

  void catchable(boolean catchable);
}
