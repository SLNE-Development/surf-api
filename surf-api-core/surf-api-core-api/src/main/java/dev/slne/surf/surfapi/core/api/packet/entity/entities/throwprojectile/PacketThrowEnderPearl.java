package dev.slne.surf.surfapi.core.api.packet.entity.entities.throwprojectile;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.ApiStatus;

@CanBeSpawned
public interface PacketThrowEnderPearl extends PacketThrowItemProjectile<PacketThrowEnderPearl>, Spawnable {

    @ApiStatus.Obsolete // Always ender pearl - anything else is unsupported
    @Override
    void item(ItemStack item);
}
