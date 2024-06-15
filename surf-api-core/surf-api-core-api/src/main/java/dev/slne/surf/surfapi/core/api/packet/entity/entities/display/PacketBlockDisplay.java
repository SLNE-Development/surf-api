package dev.slne.surf.surfapi.core.api.packet.entity.entities.display;

import static com.google.common.base.Preconditions.checkNotNull;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketBlockDisplay extends PacketDisplay<PacketBlockDisplay>, Spawnable {

  int BLOCK_STATE_INDEX = 23;

  WrappedBlockState blockState();

  void blockState(WrappedBlockState blockState);

  default void blockState(StateType type) {
    blockState(checkNotNull(type, "type").createBlockState());
  }
}
