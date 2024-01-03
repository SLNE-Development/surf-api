package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.DyeColor;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketSheep;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PacketSheepImpl extends PacketAnimalImpl<PacketSheep> implements PacketSheep {

    public PacketSheepImpl(UUID uuid) {
        super(uuid, EntityTypes.SHEEP);
    }

    @Override
    public DyeColor sheepColor() {
        return DyeColor.getById(getMaskBitRaw(SHEEP_FLAG_INDEX, COLOR_FLAG));
    }

    @Override
    public void sheepColor(@NotNull DyeColor color) {
        setMaskBit(SHEEP_FLAG_INDEX, COLOR_FLAG, color.getWoolData());
        afterSet();
    }

    @Override
    public boolean sheared() {
        return getMaskBit(SHEEP_FLAG_INDEX, SHEARED_FLAG);
    }

    @Override
    public void sheared(boolean sheared) {
        setMaskBit(SHEEP_FLAG_INDEX, SHEARED_FLAG, sheared);
        afterSet();
    }
}
