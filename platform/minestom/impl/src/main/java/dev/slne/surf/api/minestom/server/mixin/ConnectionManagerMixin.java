package dev.slne.surf.api.minestom.server.mixin;

import dev.slne.surf.api.minestom.server.impl.configuration.ConfigurationPhaseHook;
import dev.slne.surf.api.minestom.server.impl.mixin.duck.ConnectionManagerDuck;
import dev.slne.surf.api.minestom.server.impl.player.SurfPlayerProviders;
import java.util.Set;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.network.PlayerProvider;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Redirects {@code doConfiguration} to {@link ConfigurationPhaseHook}, guards the player provider
 * through {@link SurfPlayerProviders} and exposes the private player sets through
 * {@link ConnectionManagerDuck}.
 */
@Mixin(ConnectionManager.class)
abstract class ConnectionManagerMixin implements ConnectionManagerDuck {

    @Inject(
        method = "setPlayerProvider(Lnet/minestom/server/network/PlayerProvider;)V",
        at = @At("HEAD"),
        remap = false
    )
    private void surf$checkPlayerProvider(final @Nullable PlayerProvider playerProvider, final CallbackInfo ci) {
        SurfPlayerProviders.checkProvider(playerProvider);
    }

    @Accessor(value = "configurationPlayers", remap = false)
    @Override
    public abstract Set<Player> surf$configurationPlayers();

    @Accessor(value = "keepAlivePlayers", remap = false)
    @Override
    public abstract Set<Player> surf$keepAlivePlayers();

    @Inject(
        method = "doConfiguration(Lnet/minestom/server/entity/Player;Z)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void surf$runConfigurationPhase(final Player player, final boolean isFirstConfig, final CallbackInfo ci) {
        if (ConfigurationPhaseHook.configure(this, player, isFirstConfig)) {
            ci.cancel();
        }
    }
}
