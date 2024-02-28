package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.piglin;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.piglin.PacketPiglin;
import java.util.UUID;

public final class PacketPiglinImpl extends PacketBasePiglinImpl<PacketPiglin> implements
    PacketPiglin {

  public PacketPiglinImpl(UUID uuid) {
    super(uuid, EntityTypes.PIGLIN);
  }

  @Override
  public boolean baby() {
    return get(BABY_INDEX, false);
  }

  @Override
  public void baby(boolean flag) {
    set(BABY_INDEX, flag);
    afterSet();
  }

  @Override
  public boolean chargingCrossbow() {
    return get(CHARGING_CROSSBOW_INDEX, false);
  }

  @Override
  public void chargingCrossbow(boolean chargingCrossbow) {
    set(CHARGING_CROSSBOW_INDEX, chargingCrossbow);
    afterSet();
  }

  @Override
  public boolean dancing() {
    return get(DANCING_INDEX, false);
  }

  @Override
  public void dancing(boolean dancing) {
    set(DANCING_INDEX, dancing);
    afterSet();
  }
}
