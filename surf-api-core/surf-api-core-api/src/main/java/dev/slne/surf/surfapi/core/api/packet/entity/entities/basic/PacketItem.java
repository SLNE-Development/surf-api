package dev.slne.surf.surfapi.core.api.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.SpawnVelocity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketItem extends PacketEntity<PacketItem>, SpawnVelocity, Spawnable {

  int ITEM_INDEX = 8;

  ItemStack item();

  void item(@NotNull ItemStack item);
}
