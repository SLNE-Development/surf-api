package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.DyeColor;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse.PacketLlama;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

import static com.google.common.base.Preconditions.*;

public sealed class PacketLlamaImpl<Impl extends PacketLlama<Impl>> extends PacketChestedHorseImpl<Impl> implements PacketLlama<Impl> permits PacketTraderLlamaImpl {
    public PacketLlamaImpl(UUID uuid) {
        super(uuid, EntityTypes.LLAMA);
    }

    protected PacketLlamaImpl(UUID uuid, EntityType type) {
        super(uuid, type);
    }

    @Override
    public int strength() {
        return get(STRENGTH_INDEX, 0);
    }

    @Override
    public void strength(int strength) {
        checkArgument(strength >= 1 && strength <= 5, "strength must be in range [1..5]");

        set(STRENGTH_INDEX, strength);
        afterSet();
    }

    @Override
    public Optional<DyeColor> carpetColor() {
        return Optional.ofNullable(DyeColor.getById(get(CARPET_COLOR_INDEX, -1)));
    }

    @Override
    public void carpetColor(@Nullable DyeColor carpetColor) {
        set(CARPET_COLOR_INDEX, carpetColor == null ? -1 : carpetColor.ordinal());
        afterSet();
    }

    @Override
    public Color color() {
        return Color.values()[get(COLOR_INDEX, 0)];
    }

    @Override
    public void color(@NotNull Color color) {
        set(COLOR_INDEX, color.ordinal());
        afterSet();
    }
}
