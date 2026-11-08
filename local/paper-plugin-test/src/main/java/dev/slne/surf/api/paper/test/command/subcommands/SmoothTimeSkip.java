package dev.slne.surf.api.paper.test.command.subcommands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.slne.surf.api.paper.SurfApiPaper;
import dev.slne.surf.api.paper.time.SkipOperations;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.GlobalScope;
import kotlinx.coroutines.future.FutureKt;

import java.util.concurrent.CompletableFuture;

public class SmoothTimeSkip extends CommandAPICommand {

    public SmoothTimeSkip(String commandName) {
        super(commandName);

        executes((commandSender, commandArguments) -> {
            CompletableFuture<Object> future = FutureKt.future(GlobalScope.INSTANCE,
                    Dispatchers.getIO(),
                    CoroutineStart.DEFAULT,
                    (coroutineScope, continuation) -> SurfApiPaper.Companion
                            .skipTimeSmoothly(SkipOperations.NEXT_DAY, continuation));

            future.thenAccept((result) -> {
                commandSender.sendMessage("Time skipped smoothly");
            });
        });
    }
}
