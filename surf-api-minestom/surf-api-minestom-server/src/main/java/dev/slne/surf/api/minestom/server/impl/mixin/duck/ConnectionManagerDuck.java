package dev.slne.surf.api.minestom.server.impl.mixin.duck;

import java.util.Set;
import net.minestom.server.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Exposes the private player sets of {@code ConnectionManager}, implemented by {@code ConnectionManagerMixin}.
 */
@NullMarked
public interface ConnectionManagerDuck {

    Set<Player> surf$configurationPlayers();

    Set<Player> surf$keepAlivePlayers();
}
