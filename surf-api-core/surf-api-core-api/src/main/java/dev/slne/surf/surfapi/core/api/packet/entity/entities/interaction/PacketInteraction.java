package dev.slne.surf.surfapi.core.api.packet.entity.entities.interaction;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.ApiStatus;

@CanBeSpawned
public interface PacketInteraction extends PacketEntity<PacketInteraction>, Spawnable {

    /**
     * Index numbers for packets
     */
    int WIDTH_INDEX = 8, HEIGHT_INDEX = 9, RESPONSIVE_INDEX = 10;

    float width();

    void width(float width);

    float height();

    void height(float height);

    /**
     * Gets weather the interaction can be attacked / interacted
     *
     * @return weather the interaction can be attacked / interacted
     */
    boolean responsive();

    /**
     * Sets weather the interaction can be attacked / interacted
     */
    void responsive(boolean responsive);
}
