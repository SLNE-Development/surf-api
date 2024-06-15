package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.raider;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider.PacketVindicator;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.raider.illager.PacketAbstractIllagerImpl;
import java.util.UUID;

public final class PacketVindicatorImpl extends
    PacketAbstractIllagerImpl<PacketVindicator> implements PacketVindicator {

  public PacketVindicatorImpl(UUID uuid) {
    super(uuid, EntityTypes.VINDICATOR);
  }
}
