package dev.slne.surf.surfapi.core.api.packet.entity.interact;

import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity.InteractAction;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;

public interface SurfInteractHandler<E extends PacketEntity<E>> {

    void handle(E entity, InteractAction interactAction, InteractionHand interactionHand, User user);

    default void handleInternal(PacketEntity<?> entity, InteractAction interactAction, InteractionHand interactionHand, User user) {
        handle((E) entity, interactAction, interactionHand, user);
    }
}
