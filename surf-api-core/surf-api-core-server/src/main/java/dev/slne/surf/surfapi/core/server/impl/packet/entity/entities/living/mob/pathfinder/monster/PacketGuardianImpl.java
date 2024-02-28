package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketGuardian;
import java.util.UUID;

public sealed class PacketGuardianImpl<Impl extends PacketGuardian<Impl>> extends
    PacketMonsterImpl<Impl> implements PacketGuardian<Impl> permits PacketElderGuardianImpl {

  private int targetId = 0;

  public PacketGuardianImpl(UUID uuid) {
    super(uuid, EntityTypes.GUARDIAN);
  }

  protected PacketGuardianImpl(UUID uuid, EntityType type) {
    super(uuid, type);
  }

  @Override
  public boolean moving() {
    return get(MOVING_INDEX, false);
  }

  @Override
  public void moving(boolean moving) {
    set(MOVING_INDEX, moving);
    afterSet();
  }

  @Override
  public int targetId() {
    return targetId;
  }

  @Override
  public void targetId(int targetId) {
    this.targetId = targetId;
    set(TARGET_ID_INDEX, targetId);
    afterSet();
  }
}
