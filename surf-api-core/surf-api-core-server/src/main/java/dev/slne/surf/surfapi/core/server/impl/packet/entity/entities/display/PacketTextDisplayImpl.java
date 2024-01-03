package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.display;

import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.TextAlignment;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.display.PacketTextDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.google.common.base.Preconditions.*;

public final class PacketTextDisplayImpl extends PacketDisplayImpl<PacketTextDisplay> implements PacketTextDisplay {

    public PacketTextDisplayImpl(UUID uuid) {
        super(uuid, EntityTypes.TEXT_DISPLAY);
    }

    @Override
    public @NotNull Component text() {
        return get(TEXT_INDEX, Component.empty());
    }

    @Override
    public void text(@NotNull Component text) {
        set(TEXT_INDEX, EntityDataTypes.ADV_COMPONENT, checkNotNull(text, "Text may not be null"));
        afterSet();
    }

    @Override
    public int lineWidth() {
        return get(LINE_WIDTH_INDEX, 200);
    }

    @Override
    public void lineWidth(int lineWidth) {
        set(LINE_WIDTH_INDEX, lineWidth);
        afterSet();
    }

    @Override
    public @NotNull TextColor backgroundColor() {
        return TextColor.color(get(BACKGROUND_COLO_INDEX, 0x40000000));
    }

    @Override
    public void backgroundColor(@NotNull TextColor backgroundColor) {
        set(BACKGROUND_COLO_INDEX, backgroundColor.value());
        afterSet();
    }

    @Override
    public byte textOpacity() {
        return get(TEXT_OPACITY_INDEX, (byte) -1);
    }

    @Override
    public void textOpacity(byte textOpacity) {
        setByte(TEXT_OPACITY_INDEX, textOpacity);
        afterSet();
    }

    @Override
    public boolean shadow() {
        return getMaskBit(TEXT_BIT_MASK_INDEX, HAS_SHADOW_BIT);
    }

    @Override
    public void shadow(boolean shadow) {
        setMaskBit(TEXT_BIT_MASK_INDEX, HAS_SHADOW_BIT, shadow);
        afterSet();
    }

    @Override
    public boolean seeThrough() {
        return getMaskBit(TEXT_BIT_MASK_INDEX, SEE_THROUGH_BIT);
    }

    @Override
    public void seeThrough(boolean seeThrough) {
        setMaskBit(TEXT_BIT_MASK_INDEX, SEE_THROUGH_BIT, seeThrough);
        afterSet();
    }

    @Override
    public boolean defaultBackgroundColor() {
        return getMaskBit(TEXT_BIT_MASK_INDEX, DEFAULT_BACKGROUND_COLOR_BIT);
    }

    @Override
    public void defaultBackgroundColor(boolean defaultBackgroundColor) {
        setMaskBit(TEXT_BIT_MASK_INDEX, DEFAULT_BACKGROUND_COLOR_BIT, defaultBackgroundColor);
        afterSet();
    }

    @Override
    public TextAlignment alignment() {
        return TextAlignment.BY_ID.get(getMaskBitRaw(TEXT_BIT_MASK_INDEX, ALIGNMENT_BIT));
    }

    @Override
    public void alignment(TextAlignment alignment) {
        setMaskBit(TEXT_BIT_MASK_INDEX, ALIGNMENT_BIT, checkNotNull(alignment, "alignment may not be null").id());
        afterSet();
    }
}
