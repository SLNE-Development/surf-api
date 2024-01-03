package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal.tameable;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.DyeColor;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.tameable.PacketWolf;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.google.common.base.Preconditions.*;

public final class PacketWolfImpl extends PacketTameableAnimalImpl<PacketWolf> implements PacketWolf {

    public PacketWolfImpl(UUID uuid) {
        super(uuid, EntityTypes.WOLF);
    }

    @Override
    public boolean interested() {
        return get(INTERESTED_INDEX, false);
    }

    @Override
    public void interested(boolean interested) {
        set(INTERESTED_INDEX, interested);
        afterSet();
    }

    @Override
    public DyeColor collarColor() {
        return DyeColor.getById(get(COLLAR_COLOR_INDEX, DyeColor.RED.getWoolData()));
    }

    @Override
    public void collarColor(@NotNull DyeColor color) {
        set(COLLAR_COLOR_INDEX, checkNotNull(color, "color").getWoolData());
        afterSet();
    }

    @Override
    public int remainingAngerTime() {
        return get(ANGER_TIME_INDEX, 0);
    }

    @Override
    public void remainingAngerTime(int remainingAngerTime) {
        set(ANGER_TIME_INDEX, remainingAngerTime);
        afterSet();
    }
}
