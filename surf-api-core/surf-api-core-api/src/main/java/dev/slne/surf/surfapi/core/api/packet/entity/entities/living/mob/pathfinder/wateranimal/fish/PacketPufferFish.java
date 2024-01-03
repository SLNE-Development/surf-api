package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.wateranimal.fish;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketPufferFish extends PacketAbstractFish<PacketPufferFish>, Spawnable {

    int PUFF_STATE_INDEX = 17;

    PuffState puffState();

    void puffState(@NotNull PuffState puffState);

    enum PuffState {
        NONE,
        MIDDLE,
        FULL
    }
}
