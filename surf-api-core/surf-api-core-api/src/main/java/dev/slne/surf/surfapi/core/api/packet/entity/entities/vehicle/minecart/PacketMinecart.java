package dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.minecart;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketMinecart extends PacketAbstractMinecart<PacketMinecart>, Spawnable {

}
