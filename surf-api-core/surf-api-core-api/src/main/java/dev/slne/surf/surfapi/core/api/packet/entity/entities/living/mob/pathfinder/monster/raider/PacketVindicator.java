package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider.illager.PacketAbstractIllager;

@CanBeSpawned
public interface PacketVindicator extends PacketAbstractIllager<PacketVindicator>, Spawnable {
}
