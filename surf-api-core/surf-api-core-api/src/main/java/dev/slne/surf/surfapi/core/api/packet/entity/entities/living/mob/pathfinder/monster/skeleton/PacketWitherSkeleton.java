package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.skeleton;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketWitherSkeleton extends PacketAbstractSkeleton<PacketWitherSkeleton>, Spawnable {
}
