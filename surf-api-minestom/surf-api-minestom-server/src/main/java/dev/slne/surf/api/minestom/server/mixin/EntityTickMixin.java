package dev.slne.surf.api.minestom.server.mixin;

import dev.slne.surf.api.minestom.server.impl.performance.EntityTickFilter;
import net.minestom.server.Tickable;
import net.minestom.server.entity.Entity;
import net.minestom.server.thread.TickThread;
import net.minestom.server.utils.chunk.ChunkUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Skips the tick of entities rejected by {@link EntityTickFilter#shouldTick}.
 * <p>
 * The scheduler of a skipped entity is still processed under the conditions of {@code Entity#tick},
 * so tasks scheduled through {@code Entity#scheduler()} keep running.
 */
@SuppressWarnings("UnstableApiUsage")
@Mixin(TickThread.class)
abstract class EntityTickMixin {

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minestom/server/Tickable;tick(J)V",
            remap = false
        ),
        remap = false
    )
    private void surf$filterEntityTick(final Tickable tickable, final long time) {
        if (tickable instanceof Entity entity && !EntityTickFilter.shouldTick(entity)) {
            surf$tickScheduler(entity);
            return;
        }

        tickable.tick(time);
    }

    @Unique
    private static void surf$tickScheduler(final Entity entity) {
        if (entity.getInstance() == null
            || entity.isRemoved()
            || !ChunkUtils.isLoaded(entity.getChunk())) {
            return;
        }

        entity.scheduler().processTick();
        if (!entity.isRemoved()) {
            entity.scheduler().processTickEnd();
        }
    }
}
