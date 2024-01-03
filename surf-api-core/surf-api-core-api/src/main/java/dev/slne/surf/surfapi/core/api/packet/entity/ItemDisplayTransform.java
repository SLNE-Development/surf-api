package dev.slne.surf.surfapi.core.api.packet.entity;

import dev.slne.surf.surfapi.core.api.util.ById;
import dev.slne.surf.surfapi.core.api.util.Util;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;

public enum ItemDisplayTransform implements ById.ByByteId {
    NONE(0),
    THIRDPERSON_LEFTHAND(1),
    THIRDPERSON_RIGHTHAND(2),
    FIRSTPERSON_LEFTHAND(3),
    FIRSTPERSON_RIGHTHAND(4),
    HEAD(5),
    GUI(6),
    GROUND(7),
    FIXED(8);

    public static final Byte2ObjectMap<ItemDisplayTransform> BY_ID = Util.byByteIdMap(values());
    private final int id;

    ItemDisplayTransform(int id) {
        this.id = id;
    }

    @Override
    public byte id() {
        return (byte) id;
    }
}
