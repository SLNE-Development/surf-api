package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketStrider;
import java.util.UUID;

public class PacketStriderImpl extends PacketAnimalImpl<PacketStrider> implements PacketStrider {

  public PacketStriderImpl(UUID uuid) {
    super(uuid, EntityTypes.STRIDER);
  }

  @Override
  public int boostTicks() {
    return get(BOOST_TICKS_INDEX, 0);
  }

  @Override
  public void boostTicks(int boostTicks) {
    set(BOOST_TICKS_INDEX, boostTicks);
    afterSet();
  }

  @Override
  public boolean shivering() {
    return get(SHIVERING_INDEX, false);
  }

  @Override
  public void shivering(boolean shivering) {
    set(SHIVERING_INDEX, shivering);
    afterSet();
  }

  @Override
  public boolean hasSaddle() {
    return get(SADDLED_INDEX, false);
  }

  @Override
  public void hasSaddle(boolean hasSaddle) {
    set(SADDLED_INDEX, hasSaddle);
    afterSet();
  }
}
