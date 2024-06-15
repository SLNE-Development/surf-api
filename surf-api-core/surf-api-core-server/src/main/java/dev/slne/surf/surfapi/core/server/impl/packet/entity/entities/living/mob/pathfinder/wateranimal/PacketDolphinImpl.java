package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.wateranimal;

import static com.google.common.base.Preconditions.checkNotNull;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.wateranimal.PacketDolphin;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class PacketDolphinImpl extends PacketWaterAnimalImpl<PacketDolphin> implements
    PacketDolphin {

  public PacketDolphinImpl(UUID uuid) {
    super(uuid, EntityTypes.DOLPHIN);
  }

  @Override
  public org.spongepowered.math.vector.Vector3i treasurePosition() {
    return fromPacketEvents(get(TREASURE_POSITION_INDEX, Vector3i.zero()));
  }

  @Override
  public void treasurePosition(@NotNull org.spongepowered.math.vector.Vector3i treasurePosition) {
    set(TREASURE_POSITION_INDEX,
        toPacketEvents(checkNotNull(treasurePosition, "treasurePosition")));
    afterSet();
  }

  @Override
  public boolean hasFish() {
    return get(HAS_FISH_INDEX, false);
  }

  @Override
  public void hasFish(boolean hasFish) {
    set(HAS_FISH_INDEX, hasFish);
    afterSet();
  }

  @Override
  public int moistnessLevel() {
    return get(MOISTNESS_LEVEL_INDEX, 2400);
  }

  @Override
  public void moistnessLevel(int moistnessLevel) {
    set(MOISTNESS_LEVEL_INDEX, moistnessLevel);
    afterSet();
  }
}
