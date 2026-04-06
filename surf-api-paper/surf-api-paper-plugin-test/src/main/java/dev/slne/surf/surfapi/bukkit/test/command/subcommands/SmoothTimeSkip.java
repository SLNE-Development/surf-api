package dev.slne.surf.api.paper.test.command.subcommands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.slne.surf.api.paper.api.SurfApiBukkit;
import dev.slne.surf.api.paper.api.time.SkipOperations;
import java.util.concurrent.CompletableFuture;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.GlobalScope;
import kotlinx.coroutines.future.FutureKt;

public class SmoothTimeSkip extends CommandAPICommand {

    public SmoothTimeSkip(String commandName) {
        super(commandName);

        executes((commandSender, commandArguments) -> {
            CompletableFuture<Object> future = FutureKt.future(GlobalScope.INSTANCE,
                Dispatchers.getIO(),
                CoroutineStart.DEFAULT,
                (coroutineScope, continuation) -> SurfApiBukkit
                    .skipTimeSmoothly(SkipOperations.NEXT_DAY, continuation));

            future.thenAccept((result) -> {
                commandSender.sendMessage("Time skipped smoothly");
            });
        });
    }
}
