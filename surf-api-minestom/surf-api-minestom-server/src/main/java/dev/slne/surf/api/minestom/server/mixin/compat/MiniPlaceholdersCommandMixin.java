package dev.slne.surf.api.minestom.server.mixin.compat;

import java.util.Collection;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Compatibility with MiniPlaceholders 3.2.0, which is compiled against Minestom 26.2.
 *
 * <p>There {@code ConnectionManager#getOnlinePlayers} returned a {@code Collection}; Minestom 26.3
 * returns a {@code Set}, so the original call fails with a {@code NoSuchMethodError} as soon as the
 * {@code /miniplaceholders} command lists the online players.</p>
 *
 * <p>Applied through {@code mixins.surf-api-minestom-server-miniplaceholders.json} when
 * MiniPlaceholders is on the system class path, as a server library or a plugin the bootstrap
 * places there; not when a plugin with a class loader of its own shades it. A version compiled
 * against Minestom 26.3 has no such call, which leaves the class untransformed without an error
 * ({@code require = 0}); {@code MiniPlaceholdersCommandMixinTest} asserts that 3.2.0 is patched.</p>
 */
@Mixin(targets = "io.github.miniplaceholders.minestom.command.MinestomCommand", remap = false)
abstract class MiniPlaceholdersCommandMixin {

    @Redirect(
        method = "lambda$new$0",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minestom/server/network/ConnectionManager;getOnlinePlayers()Ljava/util/Collection;",
            remap = false
        ),
        remap = false,
        require = 0
    )
    private static Collection<Player> surf$onlinePlayers(final ConnectionManager connectionManager) {
        return connectionManager.getOnlinePlayers();
    }
}
