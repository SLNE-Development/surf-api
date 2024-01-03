package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketSpider;

import java.util.UUID;

public final class PacketSpiderImpl extends PacketMonsterImpl<PacketSpider> implements PacketSpider {

    public PacketSpiderImpl(UUID uuid) {
        super(uuid, EntityTypes.SPIDER);
    }

    @Override
    public boolean climbing() {
        return getMaskBit(SPIDER_FLAGS_INDEX, CLIMBING_FLAG);
    }

    @Override
    public void climbing(boolean climbing) {
        setMaskBit(SPIDER_FLAGS_INDEX, CLIMBING_FLAG, climbing);
        afterSet();
    }
}
