package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.basic.PacketFallingBlock;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public final class PacketFallingBlockImpl extends PacketEntityImpl<PacketFallingBlock> implements
    PacketFallingBlock {

  private int blockStateId = 0;

  public PacketFallingBlockImpl(UUID uuid) {
    super(uuid, EntityTypes.FALLING_BLOCK);
  }

  @Override
  public Vector3i spawnPosition() {
    return get(SPAWN_POSITION_INDEX, Vector3i.zero());
  }

  @Override
  public void spawnPosition(@NotNull Vector3i spawnPosition) {
    set(SPAWN_POSITION_INDEX, EntityDataTypes.BLOCK_POSITION, spawnPosition);
    afterSet();
  }

  @Override
  public WrappedBlockState blockState() {
    return WrappedBlockState.getByGlobalId(blockStateId).clone();
  }

  @Override
  public void blockState(@NotNull WrappedBlockState blockState) {
    blockStateId = blockState.getGlobalId();
  }

  @Override
  public int getData() {
    return blockStateId;
  }
}
