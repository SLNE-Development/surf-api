package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.throwprojectile;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.throwprojectile.PacketThrowEgg;
import dev.slne.surf.surfapi.core.api.util.ItemStackFactory;

import java.util.UUID;

public final class PacketThrowEggImpl extends PacketThrowItemProjectileImpl<PacketThrowEgg> implements PacketThrowEgg {
    private static final ItemStack EGG = ItemStackFactory.of(ItemTypes.EGG);

    public PacketThrowEggImpl(UUID uuid) {
        super(uuid, EntityTypes.EGG, EGG.copy());
    }
}
