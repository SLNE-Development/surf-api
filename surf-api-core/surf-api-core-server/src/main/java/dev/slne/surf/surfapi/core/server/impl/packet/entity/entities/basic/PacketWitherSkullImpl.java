package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.basic.PacketWitherSkull;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;

import java.util.UUID;

public final class PacketWitherSkullImpl extends PacketEntityImpl<PacketWitherSkull> implements PacketWitherSkull {
    public PacketWitherSkullImpl(UUID uuid) {
        super(uuid, EntityTypes.WITHER_SKULL);
    }

    @Override
    public boolean invulnerable() {
        return get(INVULNERABLE_INDEX, false);
    }

    @Override
    public void invulnerable(boolean invulnerable) {
        set(INVULNERABLE_INDEX, invulnerable);
        afterSet();
    }
}
