package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal.tameable;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.tameable.PacketParrot;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PacketParrotImpl extends PacketTameableAnimalImpl<PacketParrot> implements PacketParrot {

    public PacketParrotImpl(UUID uuid) {
        super(uuid, EntityTypes.PARROT);
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
