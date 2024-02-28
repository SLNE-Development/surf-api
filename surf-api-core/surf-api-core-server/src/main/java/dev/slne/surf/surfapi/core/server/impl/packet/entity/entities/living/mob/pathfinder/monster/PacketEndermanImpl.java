package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketEnderman;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class PacketEndermanImpl extends PacketMonsterImpl<PacketEnderman> implements
    PacketEnderman {

  public PacketEndermanImpl(UUID uuid) {
    super(uuid, EntityTypes.ENDERMAN);
  }

  @Override
  public Optional<WrappedBlockState> carriedBlock() {
    return Optional.<Integer>ofNullable(get(CARRIED_BLOCK_INDEX, null))
        .map(WrappedBlockState::getByGlobalId);
  }

  @Override
  public void carriedBlock(@Nullable WrappedBlockState blockData) {
    setOptInt(CARRIED_BLOCK_INDEX,
        Optional.ofNullable(blockData).map(WrappedBlockState::getGlobalId));
    afterSet();
  }

  @Override
  public boolean screaming() {
    return get(SCREAMING_INDEX, false);
  }

  @Override
  public void screaming(boolean screaming) {
    set(SCREAMING_INDEX, screaming);
    afterSet();
  }

  @Override
  public boolean hasBeenStaredAt() {
    return get(STARED_AT_INDEX, false);
  }

  @Override
  public void hasBeenStaredAt(boolean hasBeenStaredAt) {
    set(STARED_AT_INDEX, hasBeenStaredAt);
    afterSet();
  }
}
