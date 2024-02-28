package dev.slne.surf.surfapi.core.api.packet.entity.entities.basic;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.math.vector.Vector3i;

@CanBeSpawned
public interface PacketEndCrystal extends PacketEntity<PacketEndCrystal>, Spawnable {

  int BEAM_TARGET_INDEX = 8, SHOW_BOTTOM_INDEX = 9;

  Optional<Vector3i> beamTarget();

  void beamTarget(@Nullable Vector3i beamTarget);

  boolean showBottom();

  void showBottom(boolean showBottom);
}
