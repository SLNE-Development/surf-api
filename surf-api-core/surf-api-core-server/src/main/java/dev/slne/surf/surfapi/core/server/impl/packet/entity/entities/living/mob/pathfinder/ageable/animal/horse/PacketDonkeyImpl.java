package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse;

import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse.PacketDonkey;
import java.util.UUID;

public final class PacketDonkeyImpl extends PacketChestedHorseImpl<PacketDonkey> implements
    PacketDonkey {

  public PacketDonkeyImpl(UUID uuid) {
    super(uuid);
  }
}
