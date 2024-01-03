package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketChestedHorse<Impl extends PacketChestedHorse<Impl>> extends PacketAbstractHorse<Impl>, Spawnable {

    int HAS_CHEST_INDEX = 18;

    boolean hasChest();

    void hasChest(boolean hasChest);
}