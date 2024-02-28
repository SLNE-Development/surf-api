package dev.slne.surf.surfapi.core.api.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.world.PaintingType;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.NeedsRespawn;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Orientation;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketPainting extends PacketEntity<PacketPainting>, Spawnable {

  int PAINTING_INDEX = 8;

  PaintingType painting();

  void painting(@NotNull PaintingType painting);

  Orientation orientation();

  @NeedsRespawn
  void orientation(@NotNull Orientation orientation);
}
