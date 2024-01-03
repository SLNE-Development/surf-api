package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketRabbit;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.google.common.base.Preconditions.*;

public final class PacketRabbitImpl extends PacketAnimalImpl<PacketRabbit> implements PacketRabbit {

    public PacketRabbitImpl(UUID uuid) {
        super(uuid, EntityTypes.RABBIT);
    }

    @Override
    public Type rabbitType() {
        return Type.values()[get(RABBIT_TYPE_INDEX, Type.BROWN.ordinal())];
    }

    @Override
    public void setRabbitType(@NotNull Type type) {
        set(RABBIT_TYPE_INDEX, checkNotNull(type, "type").ordinal());
        afterSet();
    }
}
