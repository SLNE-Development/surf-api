package dev.slne.surf.api.minestom.server.mixin;

import dev.slne.surf.api.minestom.server.impl.command.CommandAPIHook;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandResult;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes commands registered through CommandAPI past Minestom's own command handling: name
 * lookup, execution and the declare-commands packet.
 */
@Mixin(CommandManager.class)
abstract class CommandManagerMixin {

    @Inject(
        method = "commandExists(Ljava/lang/String;)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void surf$commandApiOwnsName(final String commandName, final CallbackInfoReturnable<Boolean> cir) {
        if (CommandAPIHook.owns(commandName)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Injected before {@code parseCommand} instead of at the method head so that
     * {@code PlayerCommandEvent} listeners see the command first and can cancel or rewrite it.
     */
    @Inject(
        method = "execute(Lnet/minestom/server/command/CommandSender;Ljava/lang/String;)Lnet/minestom/server/command/builder/CommandResult;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minestom/server/command/CommandManager;parseCommand(Lnet/minestom/server/command/CommandSender;Ljava/lang/String;)Lnet/minestom/server/command/CommandParser$Result;",
            remap = false
        ),
        cancellable = true,
        remap = false
    )
    private void surf$dispatchCommandApi(
        final CommandSender sender,
        final String command,
        final CallbackInfoReturnable<CommandResult> cir
    ) {
        final CommandResult result = CommandAPIHook.execute(sender, command);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    @Inject(
        method = "createDeclareCommandsPacket(Lnet/minestom/server/entity/Player;)Lnet/minestom/server/network/packet/server/play/DeclareCommandsPacket;",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private void surf$declareCommandApi(final Player player, final CallbackInfoReturnable<DeclareCommandsPacket> cir) {
        cir.setReturnValue(CommandAPIHook.declare(cir.getReturnValue(), player));
    }
}
