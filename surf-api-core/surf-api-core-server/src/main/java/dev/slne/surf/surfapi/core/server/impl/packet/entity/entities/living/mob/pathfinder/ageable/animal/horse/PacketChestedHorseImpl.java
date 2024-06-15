package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.horse.PacketChestedHorse;
import java.util.UUID;

public sealed class PacketChestedHorseImpl<Impl extends PacketChestedHorse<Impl>> extends
    PacketAbstractHorseImpl<Impl> implements PacketChestedHorse<Impl> permits PacketDonkeyImpl,
    PacketLlamaImpl, PacketMuleImpl {

  public PacketChestedHorseImpl(UUID uuid) {
    super(uuid, EntityTypes.CHESTED_HORSE);
  }

  protected PacketChestedHorseImpl(UUID uuid, EntityType type) {
    super(uuid, type);
  }

  @Override
  public boolean hasChest() {
    return get(HAS_CHEST_INDEX, false);
  }

  @Override
  public void hasChest(boolean hasChest) {
    set(HAS_CHEST_INDEX, hasChest);
    afterSet();
  }
}
