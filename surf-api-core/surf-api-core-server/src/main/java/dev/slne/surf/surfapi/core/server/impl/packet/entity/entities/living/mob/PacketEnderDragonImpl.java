package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.PacketEnderDragon;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.google.common.base.Preconditions.*;

public final class PacketEnderDragonImpl extends PacketMobImpl<PacketEnderDragon> implements PacketEnderDragon {

    public PacketEnderDragonImpl(UUID uuid) {
        super(uuid, EntityTypes.ENDER_DRAGON);
    }

    @Override
    public @NotNull Phase phase() {
        return Phase.BY_ID.get(get(PHASE_INDEX, Phase.HOVER.id()));
    }

    @Override
    public void phase(@NotNull Phase phase) {
        set(PHASE_INDEX, checkNotNull(phase, "phase").id());
        afterSet();
    }
}
