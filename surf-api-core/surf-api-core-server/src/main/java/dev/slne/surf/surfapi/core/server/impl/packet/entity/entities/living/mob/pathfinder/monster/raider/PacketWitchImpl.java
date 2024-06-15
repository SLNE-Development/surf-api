package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.raider;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider.PacketWitch;
import java.util.UUID;

public final class PacketWitchImpl extends PacketRaiderImpl<PacketWitch> implements PacketWitch {

  public PacketWitchImpl(UUID uuid) {
    super(uuid, EntityTypes.WITCH);
  }

  @Override
  public boolean drinkingPotion() {
    return get(DRINKING_POTION_INDEX, false);
  }

  @Override
  public void drinkingPotion(boolean drinkingPotion) {
    set(DRINKING_POTION_INDEX, drinkingPotion);
    afterSet();
  }
}
