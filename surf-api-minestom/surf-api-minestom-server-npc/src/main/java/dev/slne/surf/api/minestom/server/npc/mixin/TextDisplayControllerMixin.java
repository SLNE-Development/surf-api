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
 * Drops the per-tick hologram teleport that {@code AbstractNpcEntity#update} triggers for every
 * NPC: the display is only moved when it is not already on {@code npc.position.add(offset)}.
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
