package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.basic.PacketPrimedTnt;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;

import java.util.UUID;

public final class PacketPrimedTntImpl extends PacketEntityImpl<PacketPrimedTnt> implements PacketPrimedTnt {

    public PacketPrimedTntImpl(UUID uuid) {
        super(uuid, EntityTypes.PRIMED_TNT);
    }

    @Override
    public void fuseTicks(int fuseTicks) {
        set(FUSE_TICKS_INDEX, fuseTicks);
        afterSet();
    }

    @Override
    public int fuseTicks() {
        return get(FUSE_TICKS_INDEX, 80);
    }
}
