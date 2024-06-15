package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.wateranimal.fish;

import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.wateranimal.PacketWaterAnimal;

public interface PacketAbstractFish<Impl extends PacketAbstractFish<Impl>> extends
    PacketWaterAnimal<Impl> {

  int FROM_BUKKIT_INDEX = 16;

  boolean fromBukkit();

  void fromBukkit(boolean fromBukkit);
}
