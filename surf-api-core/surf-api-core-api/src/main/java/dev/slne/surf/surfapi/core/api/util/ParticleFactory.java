package dev.slne.surf.surfapi.core.api.util;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.particle.data.*;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleType;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.google.common.base.Preconditions.*;

@ApiStatus.NonExtendable
public interface ParticleFactory {

    @Contract("_ -> new")
    static @NotNull Particle of(@NotNull ParticleType type) {
        checkNotNull(type, "Particle type may not be null");

        return new Particle(type);
    }

    @Contract(value = "_, _ -> new", pure = true)
    static @NotNull Particle of(@NotNull ParticleType type, @NotNull ParticleData data) {
        checkNotNull(type, "Particle type may not be null");
        checkNotNull(data, "Particle data may not be null");

        return new Particle(type, data);
    }

    @Contract("_, _ -> new")
    static @NotNull Particle of(@NotNull ParticleType type, @NotNull WrappedBlockState blockState) {
        checkNotNull(blockState, "Block state may not be null");

        return of(type, new ParticleBlockStateData(blockState));
    }

    @Contract("_, _, _, _ -> new")
    static @NotNull Particle of(@NotNull ParticleType type, float scale, @NotNull TextColor start, @NotNull TextColor end) {
        checkNotNull(start, "Start color may not be null");
        checkNotNull(end, "End color may not be null");

        return of(type, new ParticleDustColorTransitionData(scale, start.red(), start.green(), start.blue(), end.red(), end.green(), end.blue()));
    }

    @Contract("_, _ -> new")
    static @NotNull Particle of(@NotNull ParticleType type, @NotNull ItemStack itemStack) {
        checkNotNull(itemStack, "Item stack may not be null");

        return of(type, new ParticleItemStackData(itemStack));
    }

    @Contract("_, _ -> new")
    static @NotNull Particle of(@NotNull ParticleType type, float roll) {
        return of(type, new ParticleSculkChargeData(roll));
    }

    @Contract("_, _ -> new")
    static @NotNull Particle of(@NotNull ParticleType type, int delay) {
        return of(type, new ParticleShriekData(delay));
    }

    @Contract("_, _, _, _ -> new")
    static @NotNull Particle of(@NotNull ParticleType type, @NotNull Vector3i startingPosition, @Nullable Vector3i blockPosition, int ticks) {
        checkNotNull(startingPosition, "Starting position may not be null");

        return of(type, new ParticleVibrationData(startingPosition, blockPosition, ticks));
    }

    @Contract("_, _, _, _ -> new")
    static @NotNull Particle of(@NotNull ParticleType type, @NotNull Vector3i startingPosition, int entityId, int ticks) {
        checkNotNull(startingPosition, "Starting position may not be null");

        return of(type, new ParticleVibrationData(startingPosition, entityId, ticks));
    }
}
