package dev.slne.surf.surfapi.core.api.packet.entity.entities.throwprojectile;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

public interface PacketThrowSnowball extends PacketThrowItemProjectile<PacketThrowSnowball> {

    @ApiStatus.Obsolete // Always snowball - anything else is unsupported
    @Override
    void item(ItemStack item);
}
