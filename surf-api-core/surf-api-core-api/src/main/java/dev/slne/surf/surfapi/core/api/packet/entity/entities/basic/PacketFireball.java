package dev.slne.surf.surfapi.core.api.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Shootable;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.SpawnVelocity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketFireball extends PacketEntity<PacketFireball>, SpawnVelocity, Shootable,
    Spawnable {

  int ITEM_INDEX = 8;

  @NotNull
  ItemStack displayItem();

  void displayItem(@NotNull ItemStack displayItem);
}
