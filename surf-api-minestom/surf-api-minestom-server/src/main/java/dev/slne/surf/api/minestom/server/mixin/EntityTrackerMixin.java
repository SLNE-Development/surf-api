package dev.slne.surf.api.minestom.server.mixin;

import dev.slne.surf.api.minestom.server.impl.mixin.duck.EntityTrackerEntryDuck;
import dev.slne.surf.api.minestom.server.impl.performance.EntityViewerLookup;
import dev.slne.surf.api.minestom.server.impl.performance.EntityViewerLookup.EntityPositionResolver;
import java.util.Set;
import java.util.function.Consumer;
import net.minestom.server.coordinate.ChunkRange;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.EntityTracker;
import net.minestom.server.instance.EntityTracker.Target;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.vectrix.flare.fastutil.Int2ObjectSyncMap;

/**
 * Speeds up {@code nearbyEntitiesByChunkRange}: with few tracked entities it scans them directly
 * against the requested range instead of probing every chunk bucket ({@code (2r+1)^2} map gets,
 * 4225 at view distance 32); the remaining bucket path drops the spiral traversal for a plain
 * rectangular loop. Membership checks use the tracker-recorded position, keeping vanilla
 * chunk-boundary semantics.
 */
@Mixin(targets = "net.minestom.server.instance.EntityTrackerImpl", remap = false)
abstract class EntityTrackerMixin {

    @Shadow
    @Final
    private Int2ObjectSyncMap<Object> entriesByEntityId;

    @Unique
    private EntityPositionResolver surf$positionResolver;

    @Inject(
        method = "nearbyEntitiesByChunkRange",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 1
    )
    private <T extends Entity> void surf$directNearbyScan(
        final Point point,
        final int chunkRange,
        final Target<T> target,
        final Consumer<T> query,
        final CallbackInfo ci
    ) {
        final Set<T> candidates = ((EntityTracker) (Object) this).entities(target);
        if (!EntityViewerLookup.useDirectScan(candidates.size(), chunkRange)) {
            return;
        }

        EntityViewerLookup.directScan(
            candidates,
            surf$resolver(),
            point.chunkX(),
            point.chunkZ(),
            chunkRange,
            query
        );
        ci.cancel();
    }

    @Redirect(
        method = "nearbyEntitiesByChunkRange",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minestom/server/coordinate/ChunkRange;chunksInRange(Lnet/minestom/server/coordinate/Point;ILnet/minestom/server/coordinate/ChunkRange$ChunkConsumer;)V",
            remap = false
        ),
        remap = false,
        require = 1
    )
    private void surf$rectangularChunkScan(
        final Point point,
        final int range,
        final ChunkRange.ChunkConsumer consumer
    ) {
        EntityViewerLookup.chunksInRangeRectangular(point.chunkX(), point.chunkZ(), range, consumer);
    }

    @Unique
    private EntityPositionResolver surf$resolver() {
        EntityPositionResolver resolver = this.surf$positionResolver;
        if (resolver == null) {
            final Int2ObjectSyncMap<Object> entries = this.entriesByEntityId;
            resolver = entity -> {
                final Object entry = entries.get(entity.getEntityId());
                return entry == null ? null : ((EntityTrackerEntryDuck) entry).surf$lastPosition();
            };
            this.surf$positionResolver = resolver;
        }
        return resolver;
    }
}
