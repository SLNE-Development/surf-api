package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.basic.PacketDragonFireball;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;

import java.util.UUID;

public final class PacketDragonFireballImpl extends PacketEntityImpl<PacketDragonFireball> implements PacketDragonFireball {

    public PacketDragonFireballImpl(UUID uuid) {
        super(uuid, EntityTypes.DRAGON_FIREBALL);
    }
}
