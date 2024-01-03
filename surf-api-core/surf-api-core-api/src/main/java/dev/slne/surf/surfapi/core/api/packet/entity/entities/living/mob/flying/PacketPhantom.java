package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.flying;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import dev.slne.surf.surfapi.core.api.util.pos.Hitbox;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Range;

@CanBeSpawned
public interface PacketPhantom extends PacketFlying<PacketPhantom>, Spawnable {

    int SIZE_INDEX = 16;

    /**
     * The size of the phantom.
     * <p>
     * The Hitbox size is determined by:
     * <li>
     * {@code horizontal = 0.9 + 0.2 * size}
     * </li>
     * <li>
     * {@code vertical = 0.5 + 0.1 * size}
     * </li>
     * </p>
     *
     * @return The size of the phantom
     * @apiNote You should consider using {@link #hitbox()} instead as it is more simple to use.
     */
    @ApiStatus.Obsolete
    int size();

    /**
     * Sets the size of the phantom.
     *
     * @param size The new size of the phantom.
     * @apiNote You should consider using {@link #hitbox(double)} instead as it is more simple to use.
     */
    @ApiStatus.Obsolete
    void size(int size);

    /**
     * The hitbox of the phantom.
     *
     * @return The hitbox of the phantom.
     */
    Hitbox hitbox();

    /**
     * Sets the hitbox size of the phantom.
     * <p>
     * This method will automatically calculate the size of the phantom with the formula from {@link #size()}.
     * </p>
     * <br>
     * <b>Example:</b>
     * <pre>{@code
     *    final PacketPhantom phantom = ...;
     *    phantom.hitbox(1.0); // 1.0 is the width and height of the hitbox
     *    phantom.size(); // 5
     *    }</pre>
     *
     * @param hitboxSize
     */
    void hitbox(@Range(from = 0, to = ((long) Double.MAX_VALUE)) double hitboxSize);
}
