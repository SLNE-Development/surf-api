package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.basic.PacketEyeOfEnder;
import dev.slne.surf.surfapi.core.api.util.ItemStackFactory;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.google.common.base.Preconditions.*;

public final class PacketEyeOfEnderImpl extends PacketEntityImpl<PacketEyeOfEnder> implements PacketEyeOfEnder {

    private static final ItemStack EYE_OF_ENDER = ItemStackFactory.of(ItemTypes.ENDER_EYE);

    public PacketEyeOfEnderImpl(UUID uuid) {
        super(uuid, EntityTypes.EYE_OF_ENDER);
    }

    @Override
    public ItemStack item() {
        return get(ITEM_INDEX, EYE_OF_ENDER.copy());
    }

    @Override
    public void item(@NotNull ItemStack item) {
        set(ITEM_INDEX, checkNotNull(item, "Item may not be null"));
        afterSet();
    }
}
