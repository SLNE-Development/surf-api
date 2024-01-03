package dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.minecart.container;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketHopperMinecart extends PacketAbstractMinecartContainer<PacketHopperMinecart>, Spawnable {
}
