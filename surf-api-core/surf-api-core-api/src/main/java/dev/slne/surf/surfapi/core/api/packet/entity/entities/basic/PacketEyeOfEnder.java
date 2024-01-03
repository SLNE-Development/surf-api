package dev.slne.surf.surfapi.core.api.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketEyeOfEnder extends PacketEntity<PacketEyeOfEnder>, Spawnable {

    int ITEM_INDEX = 8;

    ItemStack item();

    @ApiStatus.Obsolete
        // Always ender eye
    void item(@NotNull ItemStack item);
}
