package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import static com.google.common.base.Preconditions.checkArgument;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketPig;
import java.util.UUID;

public final class PacketPigImpl extends PacketAnimalImpl<PacketPig> implements PacketPig {

  public PacketPigImpl(UUID uuid) {
    super(uuid, EntityTypes.PIG);
  }

  @Override
  public boolean hasSaddle() {
    return get(SADDLE_INDEX, false);
  }

  @Override
  public void hasSaddle(boolean saddled) {
    set(SADDLE_INDEX, saddled);
    afterSet();
  }

  @Override
  public int boostTicks() {
    return get(BOOST_TICKS_INDEX, 0);
  }

  @Override
  public void boostTicks(int ticks) {
    checkArgument(ticks >= 0, "Boost ticks cannot be negative");

    set(BOOST_TICKS_INDEX, ticks);
    afterSet();
  }
}
