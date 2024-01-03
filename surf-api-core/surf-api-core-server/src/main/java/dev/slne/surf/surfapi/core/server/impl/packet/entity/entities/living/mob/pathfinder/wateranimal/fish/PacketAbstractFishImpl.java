package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.wateranimal.fish;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.wateranimal.fish.PacketAbstractFish;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.wateranimal.PacketWaterAnimalImpl;

import java.util.UUID;

public abstract class PacketAbstractFishImpl<Impl extends PacketAbstractFish<Impl>> extends PacketWaterAnimalImpl<Impl> implements PacketAbstractFish<Impl> {

    public PacketAbstractFishImpl(UUID uuid, EntityType type) {
        super(uuid, type);
    }

    @Override
    public boolean fromBukkit() {
        return get(FROM_BUKKIT_INDEX, false);
    }

    @Override
    public void fromBukkit(boolean fromBukkit) {
        set(FROM_BUKKIT_INDEX, fromBukkit);
        afterSet();
    }
}
