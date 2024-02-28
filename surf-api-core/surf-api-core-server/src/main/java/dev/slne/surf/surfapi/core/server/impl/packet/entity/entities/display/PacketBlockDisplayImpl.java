package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.display;

import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.display.PacketBlockDisplay;
import java.util.UUID;

public final class PacketBlockDisplayImpl extends PacketDisplayImpl<PacketBlockDisplay> implements
    PacketBlockDisplay {

  public PacketBlockDisplayImpl(UUID uuid) {
    super(uuid, EntityTypes.BLOCK_DISPLAY);
  }

  @Override
  public WrappedBlockState blockState() {
    return WrappedBlockState.getByGlobalId(get(BLOCK_STATE_INDEX, 0));
  }

  @Override
  public void blockState(WrappedBlockState blockState) {
    set(BLOCK_STATE_INDEX, EntityDataTypes.BLOCK_STATE, blockState.getGlobalId());
    afterSet();
  }
}
