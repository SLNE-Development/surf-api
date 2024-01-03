package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse.PacketHorse;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PacketHorseImpl extends PacketAbstractHorseImpl<PacketHorse> implements PacketHorse {

    public PacketHorseImpl(UUID uuid) {
        super(uuid, EntityTypes.HORSE);
    }

    @Override
    public Color color() {
        return Color.values()[getTypeVariant() & 255];
    }

    @Override
    public void color(@NotNull Color color) {
        setTypeVariant(color.ordinal() & 255 | this.getTypeVariant() & -256);
        afterSet();
    }

    @Override
    public Style style() {
        return Style.values()[(this.getTypeVariant() & '\uff00') >> 8];
    }

    @Override
    public void style(@NotNull Style style) {
        setTypeVariant(getTypeVariant() & style.ordinal() << 8 & '\uff00');
        afterSet();
    }

    private int getTypeVariant() {
        return get(VARIANT_INDEX, 0);
    }

    private void setTypeVariant(int variant) {
        set(VARIANT_INDEX, variant);
    }
}
