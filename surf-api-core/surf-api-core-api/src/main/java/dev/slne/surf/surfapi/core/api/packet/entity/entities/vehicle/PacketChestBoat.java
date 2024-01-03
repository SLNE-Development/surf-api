package dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketChestBoat extends PacketBoat<PacketChestBoat>, Spawnable {
}
