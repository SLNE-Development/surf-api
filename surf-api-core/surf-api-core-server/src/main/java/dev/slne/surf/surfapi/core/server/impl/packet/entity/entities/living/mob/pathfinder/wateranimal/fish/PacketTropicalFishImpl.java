package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.wateranimal.fish;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.DyeColor;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.wateranimal.fish.PacketTropicalFish;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PacketTropicalFishImpl extends PacketAbstractFishImpl<PacketTropicalFish> implements PacketTropicalFish {

    public PacketTropicalFishImpl(UUID uuid) {
        super(uuid, EntityTypes.TROPICAL_FISH);
    }

    @Override
    public Pattern variant() {
        return getPattern(getPackedVariant());
    }

    @Override
    public void variant(@NotNull Pattern variant) {
        set(VARIANT_INDEX, getData(patternColor(), bodyColor(), variant));
        afterSet();
    }

    @Override
    public DyeColor patternColor() {
        return getPatternColor(getPackedVariant());
    }

    @Override
    public void patternColor(@NotNull DyeColor patternColor) {
        set(VARIANT_INDEX, getData(patternColor, bodyColor(), variant()));
        afterSet();
    }

    @Override
    public DyeColor bodyColor() {
        return getBodyColor(getPackedVariant());
    }

    @Override
    public void bodyColor(@NotNull DyeColor bodyColor) {
        set(VARIANT_INDEX, getData(patternColor(), bodyColor, variant()));
        afterSet();
    }

    private int getPackedVariant() {
        return get(VARIANT_INDEX, 0);
    }

    @Contract(pure = true)
    @Override
    public int getData() {
        return 0;
    }

    @Contract(pure = true)
    private static DyeColor getPatternColor(int data) {
        return DyeColor.getByWoolData((byte) ((data >> 24) & 0xFF));
    }

    @Contract(pure = true)
    private static DyeColor getBodyColor(int data) {
        return DyeColor.getByWoolData((byte) ((data >> 16) & 0xFF));
    }

    public static Pattern getPattern(int data) {
        return Pattern.getByData(data);
    }

    private static int getData(@NotNull DyeColor patternColor, @NotNull DyeColor bodyColor, @NotNull Pattern variant) {
        return patternColor.getWoolData() << 24 | bodyColor.getWoolData() << 16 | variant.getDataValue();
    }
}
