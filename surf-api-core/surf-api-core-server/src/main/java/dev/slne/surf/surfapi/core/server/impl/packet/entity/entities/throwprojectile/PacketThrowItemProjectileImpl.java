package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.throwprojectile;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.throwprojectile.PacketThrowItemProjectile;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;

import java.util.UUID;

public abstract class PacketThrowItemProjectileImpl<Impl extends PacketThrowItemProjectile<Impl>> extends PacketEntityImpl<Impl> implements PacketThrowItemProjectile<Impl> {

    private final ItemStack defaultItem;

    public PacketThrowItemProjectileImpl(UUID uuid, EntityType type, ItemStack defaultItem) {
        super(uuid, type);
        this.defaultItem = defaultItem;
    }

    @Override
    public ItemStack item() {
        return get(ITEM_INDEX, defaultItem);
    }

    @Override
    public void item(ItemStack item) {
        set(ITEM_INDEX, item);
        afterSet();
    }
}
