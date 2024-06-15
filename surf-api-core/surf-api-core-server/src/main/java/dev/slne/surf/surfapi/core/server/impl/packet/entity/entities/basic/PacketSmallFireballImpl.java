package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.basic;

import static com.google.common.base.Preconditions.checkNotNull;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.basic.PacketSmallFireball;
import dev.slne.surf.surfapi.core.api.util.ItemStackFactory;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public final class PacketSmallFireballImpl extends PacketEntityImpl<PacketSmallFireball> implements
    PacketSmallFireball {

  private static final ItemStack SMALL_FIREBALL = ItemStackFactory.of(ItemTypes.FIRE_CHARGE);

  public PacketSmallFireballImpl(UUID uuid) {
    super(uuid, EntityTypes.SMALL_FIREBALL);
  }

  @Override
  public @NotNull ItemStack displayItem() {
    return get(ITEM_INDEX, SMALL_FIREBALL.copy());
  }

  @Override
  public void displayItem(@NotNull ItemStack displayItem) {
    set(ITEM_INDEX, checkNotNull(displayItem, "displayItem"));
    afterSet();
  }
}
