package dev.slne.surf.api.minestom.server.mixin;

import dev.slne.surf.api.minestom.server.impl.performance.EntityTickFilter;
import net.minestom.server.Tickable;
import net.minestom.server.entity.Entity;
import net.minestom.server.thread.TickThread;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Skips the tick of entities whose type is disabled in {@link EntityTickFilter}. */
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
            return;
        }

        tickable.tick(time);
    }
}
