package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.villager;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketWanderingTrader extends PacketAbstractVillager<PacketWanderingTrader>,
    Spawnable {

}
