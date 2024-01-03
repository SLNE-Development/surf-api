package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketMooshroomCow;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PacketMooshroomCowImpl extends PacketCowImpl<PacketMooshroomCow> implements PacketMooshroomCow {

    public PacketMooshroomCowImpl(UUID uuid) {
        super(uuid, EntityTypes.MOOSHROOM);
    }

    @Override
    public Variant variant() {
        return Variant.BY_ID.get(get(VARIANT_INDEX, Variant.RED.getId()));
    }

    @Override
    public void variant(@NotNull Variant variant) {
        set(VARIANT_INDEX, variant.getId());
        afterSet();
    }
}
