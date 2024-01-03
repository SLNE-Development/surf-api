package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.basic.PacketEndCrystal;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityCommon;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class PacketEndCrystalImpl extends PacketEntityImpl<PacketEndCrystal> implements PacketEndCrystal {
    public PacketEndCrystalImpl(UUID uuid) {
        super(uuid, EntityTypes.END_CRYSTAL);
    }

    @Override
    public Optional<org.spongepowered.math.vector.Vector3i> beamTarget() {
        return get(BEAM_TARGET_INDEX, Optional.<Vector3i>empty()).map(PacketEntityCommon::fromPacketEvents);
    }

    @Override
    public void beamTarget(@Nullable org.spongepowered.math.vector.Vector3i beamTarget) {
        set(BEAM_TARGET_INDEX, EntityDataTypes.OPTIONAL_BLOCK_POSITION, Optional.ofNullable(beamTarget).map(PacketEntityCommon::toPacketEvents));
        afterSet();
    }

    @Override
    public boolean showBottom() {
        return get(SHOW_BOTTOM_INDEX, true);
    }

    @Override
    public void showBottom(boolean showBottom) {
        set(SHOW_BOTTOM_INDEX, showBottom);
        afterSet();
    }
}
