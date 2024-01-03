package dev.slne.surf.surfapi.core.api.packet.entity.entities.display;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import dev.slne.surf.surfapi.core.api.packet.entity.ItemDisplayTransform;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketItemDisplay extends PacketDisplay<PacketItemDisplay>, Spawnable {
    int ITEM_INDEX = 23, DISPLAY_TRANSFORM_INDEX = 24;

    @NotNull
    ItemStack item();

    /**
     * @param item the item to display
     * @see ItemStack#EMPTY
     */
    void item(@NotNull ItemStack item);

    ItemDisplayTransform itemDisplayTransform();

    void itemDisplayTransform(ItemDisplayTransform displayTransform);
}
