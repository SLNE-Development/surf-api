package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.basic.PacketItem;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.google.common.base.Preconditions.*;

public class PacketItemImpl extends PacketEntityImpl<PacketItem> implements PacketItem {
    public PacketItemImpl(UUID uuid) {
        super(uuid, EntityTypes.ITEM);
    }

    @Override
    public ItemStack item() {
        return get(ITEM_INDEX, ItemStack.EMPTY.copy());
    }

    @Override
    public void item(@NotNull ItemStack item) {
        set(ITEM_INDEX, checkNotNull(item, "item"));
        afterSet();
    }

    @Override
    public int getData() {
        return 1; // TODO: whats this?
    }
}
