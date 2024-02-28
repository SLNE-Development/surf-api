package dev.slne.surf.surfapi.core.api.packet.entity.entities.frame;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import dev.slne.surf.surfapi.core.api.packet.entity.Rotation;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.NeedsRespawn;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Orientation;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketItemFrame<Impl extends PacketItemFrame<Impl>> extends PacketEntity<Impl>,
    Spawnable {

  int ITEM_INDEX = 8, ROTATION_INDEX = 9;

  ItemStack item();

  void item(@NotNull ItemStack item);

  Rotation rotation();

  void rotation(@NotNull Rotation rotation);

  Orientation faceDirection();

  @NeedsRespawn
  void faceDirection(@NotNull Orientation faceDirection);
}
