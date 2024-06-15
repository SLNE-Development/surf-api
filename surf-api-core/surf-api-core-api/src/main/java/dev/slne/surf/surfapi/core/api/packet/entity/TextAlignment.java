package dev.slne.surf.surfapi.core.api.packet.entity;

import dev.slne.surf.surfapi.core.api.util.ById;
import dev.slne.surf.surfapi.core.api.util.Util;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;

public enum TextAlignment implements ById.ByByteId {
  CENTER(0),
  LEFT(1),
  RIGHT(2);

  public static final Byte2ObjectMap<TextAlignment> BY_ID = Util.byByteIdMap(values());
  private final int id;

  TextAlignment(int id) {
    this.id = id;
  }

  @Override
  public byte id() {
    return ((byte) id);
  }
}
