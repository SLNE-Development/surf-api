package dev.slne.surf.surfapi.bukkit.api.packet.entity.interact;

import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import me.tofaa.entitylib.entity.WrapperEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.UnknownNullability;

@FunctionalInterface
public interface SurfBukkitInteractListener {
    void onInteract(WrapperEntity clickedEntity,
                    WrapperPlayClientInteractEntity.InteractAction interactAction,
                    InteractionHand interactionHand,
                    User user,
                    @UnknownNullability("In some cases the player is not present") Player player);
}
