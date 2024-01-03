package dev.slne.surf.surfapi.core.api.packet.entity.entities;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.NeedsRespawn;

public interface Shootable {

    int shooterEntityId();

    @NeedsRespawn
    void shooterEntityId(int shooterEntityId);
}
