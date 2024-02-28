package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.flying;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.flying.PacketFlying;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.PacketMobImpl;
import java.util.UUID;

public abstract class PacketFlyingImpl<Impl extends PacketFlying<Impl>> extends
    PacketMobImpl<Impl> implements PacketFlying<Impl> {

  public PacketFlyingImpl(UUID uuid, EntityType type) {
    super(uuid, type);
  }
}
