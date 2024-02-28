package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.villager;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.villager.PacketWanderingTrader;
import java.util.UUID;

public final class PacketWanderingTraderImpl extends
    PacketAbstractVillagerImpl<PacketWanderingTrader> implements PacketWanderingTrader {

  public PacketWanderingTraderImpl(UUID uuid) {
    super(uuid, EntityTypes.WANDERING_TRADER);
  }
}
