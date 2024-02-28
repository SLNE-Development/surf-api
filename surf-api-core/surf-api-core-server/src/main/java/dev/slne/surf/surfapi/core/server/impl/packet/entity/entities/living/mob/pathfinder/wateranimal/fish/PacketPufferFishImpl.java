package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.wateranimal.fish;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.wateranimal.fish.PacketPufferFish;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public final class PacketPufferFishImpl extends PacketAbstractFishImpl<PacketPufferFish> implements
    PacketPufferFish {

  public PacketPufferFishImpl(UUID uuid) {
    super(uuid, EntityTypes.PUFFERFISH);
  }

  @Override
  public PuffState puffState() {
    return PuffState.values()[get(PUFF_STATE_INDEX, PuffState.NONE.ordinal())];
  }

  @Override
  public void puffState(@NotNull PuffState puffState) {
    set(PUFF_STATE_INDEX, puffState.ordinal());
    afterSet();
  }
}
