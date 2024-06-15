package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.PacketPathfinderMob;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.PacketMobImpl;
import java.util.UUID;

public abstract class PacketPathfinderMobImpl<Impl extends PacketPathfinderMob<Impl>> extends
    PacketMobImpl<Impl> implements PacketPathfinderMob<Impl> {

  public PacketPathfinderMobImpl(UUID uuid, EntityType type) {
    super(uuid, type);
  }
}
