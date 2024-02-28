package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.piglin;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.piglin.PacketBasePiglin;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.PacketMonsterImpl;
import java.util.UUID;

public abstract class PacketBasePiglinImpl<Impl extends PacketBasePiglin<Impl>> extends
    PacketMonsterImpl<Impl> implements PacketBasePiglin<Impl> {

  public PacketBasePiglinImpl(UUID uuid, EntityType type) {
    super(uuid, type);
  }

  @Override
  public boolean immuneToZombification() {
    return get(IMMUNE_TO_ZOMBIFICATION_INDEX, false);
  }

  @Override
  public void immuneToZombification(boolean flag) {
    set(IMMUNE_TO_ZOMBIFICATION_INDEX, flag);
    afterSet();
  }
}
