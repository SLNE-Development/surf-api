package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.zombie;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.zombie.PacketZombie;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.PacketMonsterImpl;

import java.util.UUID;

public sealed class PacketZombieImpl<Impl extends PacketZombie<Impl>> extends PacketMonsterImpl<Impl> implements PacketZombie<Impl> permits PacketDrownedImpl, PacketHuskImpl, PacketZombieVillagerImpl, PacketZombifiedPiglinImpl {

    public PacketZombieImpl(UUID uuid) {
        super(uuid, EntityTypes.ZOMBIE);
    }

    public PacketZombieImpl(UUID uuid, EntityType type) {
        super(uuid, type);
    }

    @Override
    public boolean baby() {
        return get(BABY_INDEX, false);
    }

    @Override
    public void baby(boolean baby) {
        set(BABY_INDEX, baby);
        afterSet();
    }

    @Override
    public boolean becomingDrowned() {
        return get(BECOMING_DROWNED_INDEX, false);
    }

    @Override
    public void becomingDrowned(boolean becomingDrowned) {
        set(BECOMING_DROWNED_INDEX, becomingDrowned);
        afterSet();
    }
}
