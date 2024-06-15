package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob;

import static com.google.common.base.Preconditions.checkArgument;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.PacketSlime;
import java.util.UUID;

public final class PacketSlimeImpl extends PacketMobImpl<PacketSlime> implements PacketSlime {

  public PacketSlimeImpl(UUID uuid) {
    super(uuid, EntityTypes.SLIME);
  }

  @Override
  public int size() {
    return get(SIZE_INDEX, 1);
  }

  @Override
  public void size(int size) {
    checkArgument(size >= 0, "Size cannot be negative");
    set(SIZE_INDEX, size);
    afterSet();
  }
}
