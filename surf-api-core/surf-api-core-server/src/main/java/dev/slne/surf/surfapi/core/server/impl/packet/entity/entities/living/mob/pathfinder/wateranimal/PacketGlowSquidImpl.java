package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.wateranimal;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.wateranimal.PacketGlowSquid;
import java.util.UUID;

public final class PacketGlowSquidImpl extends PacketSquidImpl<PacketGlowSquid> implements
    PacketGlowSquid {

  public PacketGlowSquidImpl(UUID uuid) {
    super(uuid, EntityTypes.GLOW_SQUID);
  }
}
