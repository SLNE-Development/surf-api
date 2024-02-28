package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.wateranimal;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.wateranimal.PacketWaterAnimal;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.PacketPathfinderMobImpl;
import java.util.UUID;

public abstract class PacketWaterAnimalImpl<Impl extends PacketWaterAnimal<Impl>> extends
    PacketPathfinderMobImpl<Impl> implements PacketWaterAnimal<Impl> {

  public PacketWaterAnimalImpl(UUID uuid, EntityType type) {
    super(uuid, type);
  }
}
