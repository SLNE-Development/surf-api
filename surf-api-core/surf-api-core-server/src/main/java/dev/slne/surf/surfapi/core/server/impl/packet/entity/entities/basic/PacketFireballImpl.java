package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.basic.PacketFireball;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.google.common.base.Preconditions.*;

public final class PacketFireballImpl extends PacketEntityImpl<PacketFireball> implements PacketFireball {
    public PacketFireballImpl(UUID uuid) {
        super(uuid, EntityTypes.FIREBALL);
    }

    @Override
    public @NotNull ItemStack displayItem() {
        return get(ITEM_INDEX, ItemStack.EMPTY.copy());
    }

    @Override
    public void displayItem(@NotNull ItemStack displayItem) {
        set(ITEM_INDEX, checkNotNull(displayItem, "displayItem"));
        afterSet();
    }
}
