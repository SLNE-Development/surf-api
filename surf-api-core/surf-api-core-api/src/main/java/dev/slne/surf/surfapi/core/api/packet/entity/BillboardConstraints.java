package dev.slne.surf.surfapi.core.api.packet.entity;

import dev.slne.surf.surfapi.core.api.util.ById;
import dev.slne.surf.surfapi.core.api.util.Util;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import org.jetbrains.annotations.Contract;

public enum BillboardConstraints implements ById.ByByteId {
  FIXED((byte) 0),
  VERTICAL((byte) 1),
  HORIZONTAL((byte) 2),
  CENTER((byte) 3);

  public static final Byte2ObjectMap<BillboardConstraints> BY_ID = Util.byByteIdMap(values());
  private final byte id;

  @Contract(pure = true)
  BillboardConstraints(byte id) {
    this.id = id;
  }

  @Contract(pure = true)
  @Override
  public byte id() {
    return id;
  }
}
