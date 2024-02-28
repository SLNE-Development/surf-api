package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.wateranimal;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketSquid<Impl extends PacketSquid<Impl>> extends PacketWaterAnimal<Impl>,
    Spawnable {

}
