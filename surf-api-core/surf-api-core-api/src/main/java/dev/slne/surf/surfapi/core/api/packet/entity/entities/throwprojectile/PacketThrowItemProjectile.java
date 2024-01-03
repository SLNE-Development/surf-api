package dev.slne.surf.surfapi.core.api.packet.entity.entities.throwprojectile;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;

public interface PacketThrowItemProjectile<Imp extends PacketThrowItemProjectile<Imp>> extends PacketEntity<Imp> {

    int ITEM_INDEX = 8;

    ItemStack item();

    void item(ItemStack item);
}
