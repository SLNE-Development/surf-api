package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.basic.PacketEvokerFangs;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;
import java.util.UUID;

public final class PacketEvokerFangsImpl extends PacketEntityImpl<PacketEvokerFangs> implements
    PacketEvokerFangs {

  public PacketEvokerFangsImpl(UUID uuid) {
    super(uuid, EntityTypes.EVOKER_FANGS);
  }
}
