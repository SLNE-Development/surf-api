package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketGoat;
import java.util.UUID;

public final class PacketGoatImpl extends PacketAnimalImpl<PacketGoat> implements PacketGoat {

  public PacketGoatImpl(UUID uuid) {
    super(uuid, EntityTypes.GOAT);
  }

  @Override
  public boolean screaming() {
    return get(SCREAMING_INDEX, false);
  }

  @Override
  public void screaming(boolean screaming) {
    set(SCREAMING_INDEX, screaming);
    afterSet();
  }

  @Override
  public boolean leftHorn() {
    return get(LEFT_HORN_INDEX, true);
  }

  @Override
  public void leftHorn(boolean hasHorn) {
    set(LEFT_HORN_INDEX, hasHorn);
    afterSet();
  }

  @Override
  public boolean rightHorn() {
    return get(RIGHT_HORN_INDEX, true);
  }

  @Override
  public void rightHorn(boolean hasHorn) {
    set(RIGHT_HORN_INDEX, hasHorn);
    afterSet();
  }
}
