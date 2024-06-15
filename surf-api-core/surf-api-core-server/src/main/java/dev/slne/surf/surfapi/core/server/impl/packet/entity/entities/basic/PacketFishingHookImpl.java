package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.basic.PacketFishingHook;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;
import java.util.UUID;

public final class PacketFishingHookImpl extends PacketEntityImpl<PacketFishingHook> implements
    PacketFishingHook {

  public PacketFishingHookImpl(UUID uuid) {
    super(uuid, EntityTypes.FISHING_BOBBER);
  }

  @Override
  public int hookedEntityId() {
    return get(HOOKED_ENTITY_ID_INDEX, 0);
  }

  @Override
  public void hookedEntityId(int hookedEntityId) {
    set(HOOKED_ENTITY_ID_INDEX, Math.max(hookedEntityId, 0));
    afterSet();
  }

  @Override
  public boolean catchable() {
    return get(IS_CATCHABLE_INDEX, false);
  }

  @Override
  public void catchable(boolean catchable) {
    set(IS_CATCHABLE_INDEX, catchable);
    afterSet();
  }

  @Override
  public int getData() {
    return Math.max(shooterEntityId, 0);
  }
}
