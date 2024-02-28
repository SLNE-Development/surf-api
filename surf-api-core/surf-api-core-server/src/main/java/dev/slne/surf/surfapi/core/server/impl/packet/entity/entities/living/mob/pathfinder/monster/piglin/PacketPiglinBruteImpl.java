package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.piglin;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.piglin.PacketPiglinBrute;
import java.util.UUID;

public final class PacketPiglinBruteImpl extends PacketBasePiglinImpl<PacketPiglinBrute> implements
    PacketPiglinBrute {

  public PacketPiglinBruteImpl(UUID uuid) {
    super(uuid, EntityTypes.PIGLIN_BRUTE);
  }
}
