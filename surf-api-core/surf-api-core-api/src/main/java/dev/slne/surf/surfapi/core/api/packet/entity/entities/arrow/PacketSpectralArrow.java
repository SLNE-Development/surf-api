package dev.slne.surf.surfapi.core.api.packet.entity.entities.arrow;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Shootable;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.SpawnVelocity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketSpectralArrow extends PacketAbstractArrow<PacketSpectralArrow>,
    SpawnVelocity, Shootable, Spawnable {

}
