package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.basic.PacketLlamaSpit;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;

import java.util.UUID;

public final class PacketLlamaSpitImpl extends PacketEntityImpl<PacketLlamaSpit> implements PacketLlamaSpit {

    public PacketLlamaSpitImpl(UUID uuid) {
        super(uuid, EntityTypes.LLAMA_SPIT);
    }
}
