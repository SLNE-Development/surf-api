package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.throwprojectile;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.throwprojectile.PacketThrowPotion;
import dev.slne.surf.surfapi.core.api.util.ItemStackFactory;

import java.util.UUID;

public final class PacketThrowPotionImpl extends PacketThrowItemProjectileImpl<PacketThrowPotion> implements PacketThrowPotion {

    private static final ItemStack POTION = ItemStackFactory.of(ItemTypes.SPLASH_POTION);

    public PacketThrowPotionImpl(UUID uuid) {
        super(uuid, EntityTypes.POTION, POTION.copy());
    }
}
