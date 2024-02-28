package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.zombie;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketDrowned extends PacketZombie<PacketDrowned>, Spawnable {

}
