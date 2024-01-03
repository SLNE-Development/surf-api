package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.flying;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.flying.PacketPhantom;
import dev.slne.surf.surfapi.core.api.util.pos.Hitbox;
import org.jetbrains.annotations.Range;

import java.util.UUID;

import static com.google.common.base.Preconditions.*;

public final class PacketPhantomImpl extends PacketFlyingImpl<PacketPhantom> implements PacketPhantom {

    public PacketPhantomImpl(UUID uuid) {
        super(uuid, EntityTypes.PHANTOM);
    }

    @Override
    public int size() {
        return get(SIZE_INDEX, 0);
    }

    @Override
    public void size(int size) {
        set(SIZE_INDEX, size);
        afterSet();
    }

    @Override
    public Hitbox hitbox() {
        final int size = size();
        return Hitbox.of(0.9 + 0.2 * size, 0.5 + 0.1 * size);
    }

    @Override
    public void hitbox(@Range(from = 0, to = ((long) Double.MAX_VALUE)) double hitboxSize) {
        checkArgument(hitboxSize >= 0, "hitboxSize cannot be negative");

        final int size = (int) ((hitboxSize - 0.9) / 0.2);
        size(size);
    }
}
