package dev.slne.surf.surfapi.core.api.packet.entity.entities;

import com.github.retrooper.packetevents.util.Vector3d;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.NeedsRespawn;
import org.jetbrains.annotations.NotNull;

public interface SpawnVelocity {

    @NotNull
    Vector3d velocityAtSpawn();

    @NeedsRespawn
    void velocityAtSpawn(@NotNull Vector3d velocityAtSpawn);
}
