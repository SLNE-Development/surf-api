package dev.slne.surf.api.minestom.server.npc.mixin;

import codes.bed.minestom.npc.display.TextDisplayController;
import dev.slne.surf.api.minestom.server.npc.impl.NpcDisplaySync;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels the per-tick {@code syncWithNpc} teleport of the text display while the display
 * already sits on {@code npc.position.add(offset)}. See {@link NpcDisplaySync#isSynchronized}.
 */
@Mixin(value = TextDisplayController.class, remap = false)
abstract class TextDisplayControllerMixin {

    @Shadow
    private Entity entity;

    @Shadow
    private Vec offset;

    @Inject(
        method = "syncWithNpc(Lnet/minestom/server/entity/Entity;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 1
    )
    private void surf$skipSynchronizedDisplay(final Entity npc, final CallbackInfo ci) {
        if (NpcDisplaySync.isSynchronized(this.entity, npc, this.offset)) {
            ci.cancel();
        }
    }
}
