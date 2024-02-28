package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import static com.google.common.base.Preconditions.checkNotNull;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketFrog;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PacketFrogImpl extends PacketAnimalImpl<PacketFrog> implements PacketFrog {

  public PacketFrogImpl(UUID uuid) {
    super(uuid, EntityTypes.FROG);
  }

  @Override
  public Variant variant() {
    return Variant.values()[get(VARIANT_INDEX, Variant.TEMPERATE.ordinal())];
  }

  @Override
  public void variant(@NotNull Variant variant) {
    set(VARIANT_INDEX, checkNotNull(variant, "variant").ordinal());
    afterSet();
  }

  @Override
  public OptionalInt tongueTargetId() {
    return get(TONGUE_TARGET_INDEX, Optional.<Integer>empty()).map(OptionalInt::of)
        .orElse(OptionalInt.empty());
  }

  @Override
  public void tongueTargetId(@Nullable Integer id) {
    setOptInt(TONGUE_TARGET_INDEX, Optional.ofNullable(id));
    afterSet();
  }
}
