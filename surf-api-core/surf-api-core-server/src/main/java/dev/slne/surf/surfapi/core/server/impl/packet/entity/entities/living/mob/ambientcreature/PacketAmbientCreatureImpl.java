package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.ambientcreature;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.ambientcreature.PacketAmbientCreature;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.PacketMobImpl;
import java.util.UUID;

public abstract class PacketAmbientCreatureImpl<Impl extends PacketAmbientCreature<Impl>> extends
    PacketMobImpl<Impl> implements PacketAmbientCreature<Impl> {

  public PacketAmbientCreatureImpl(UUID uuid, EntityType type) {
    super(uuid, type);
  }
}
