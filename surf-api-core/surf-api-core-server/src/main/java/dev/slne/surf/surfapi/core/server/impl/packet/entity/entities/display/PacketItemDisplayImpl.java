package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.display;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import dev.slne.surf.surfapi.core.api.packet.entity.ItemDisplayTransform;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.display.PacketItemDisplay;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public final class PacketItemDisplayImpl extends PacketDisplayImpl<PacketItemDisplay> implements
    PacketItemDisplay {

  public PacketItemDisplayImpl(UUID uuid) {
    super(uuid, EntityTypes.ITEM_DISPLAY);
  }

  @Override
  public @NotNull ItemStack item() {
    return get(ITEM_INDEX, ItemStack.EMPTY);
  }

  @Override
  public void item(@NotNull ItemStack item) {
    set(ITEM_INDEX, item);
    afterSet();
  }

  @Override
  public ItemDisplayTransform itemDisplayTransform() {
    return ItemDisplayTransform.BY_ID.get(
        get(DISPLAY_TRANSFORM_INDEX, ItemDisplayTransform.NONE.id()));
  }

  @Override
  public void itemDisplayTransform(ItemDisplayTransform displayTransform) {
    setByte(DISPLAY_TRANSFORM_INDEX, displayTransform.id());
    afterSet();
  }
}
