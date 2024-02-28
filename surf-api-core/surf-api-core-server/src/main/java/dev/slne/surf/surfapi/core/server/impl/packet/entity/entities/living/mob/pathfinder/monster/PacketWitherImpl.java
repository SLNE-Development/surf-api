package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster;

import static com.google.common.base.Preconditions.checkNotNull;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketWither;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public final class PacketWitherImpl extends PacketMonsterImpl<PacketWither> implements
    PacketWither {

  public PacketWitherImpl(UUID uuid) {
    super(uuid, EntityTypes.WITHER);
  }

  @Override
  public int targetEntityId(@NotNull Head head) {
    return get(checkNotNull(head, "head").id(), 0);
  }

  @Override
  public void targetEntityId(@NotNull Head head, int entityId) {
    set(checkNotNull(head, "head").id(), entityId);
    afterSet();
  }

  @Override
  public int invulnerableTicks() {
    return get(INVULNERABLE_TIME_INDEX, 0);
  }

  @Override
  public void invulnerableTicks(int ticks) {
    set(INVULNERABLE_TIME_INDEX, ticks);
    afterSet();
  }
}
