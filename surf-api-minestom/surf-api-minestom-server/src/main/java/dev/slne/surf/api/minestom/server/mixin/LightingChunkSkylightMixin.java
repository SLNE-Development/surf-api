package dev.slne.surf.api.minestom.server.mixin;

import dev.slne.surf.api.minestom.server.impl.performance.SkylightSuppression;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.Section;
import net.minestom.server.instance.light.Light;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps sky light out of {@code createLightData} for dimensions without skylight.
 */
@SuppressWarnings("UnstableApiUsage")
@Mixin(LightingChunk.class)
abstract class LightingChunkSkylightMixin {

    @Redirect(
        method = "createLightData",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minestom/server/instance/Section;skyLight()Lnet/minestom/server/instance/light/Light;",
            remap = false
        ),
        remap = false,
        require = 3,
        allow = 3
    )
    private Light surf$skipDisabledSkyLight(final Section section) {
        final Chunk chunk = (Chunk) (Object) this;
        return SkylightSuppression.sectionSkyLight(
            chunk.getInstance().getCachedDimensionType().hasSkylight(),
            section
        );
    }
}
