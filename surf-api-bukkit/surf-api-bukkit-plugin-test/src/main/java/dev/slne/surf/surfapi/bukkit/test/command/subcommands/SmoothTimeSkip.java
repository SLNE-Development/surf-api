package dev.slne.surf.surfapi.bukkit.test.command.subcommands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.slne.surf.surfapi.bukkit.api.SurfBukkitApi;
import dev.slne.surf.surfapi.bukkit.api.time.SkipOperations;

public class SmoothTimeSkip extends CommandAPICommand {
    public SmoothTimeSkip(String commandName) {
        super(commandName);

        executes((commandSender, commandArguments) -> {
            SurfBukkitApi.get().skipTimeSmoothly(SkipOperations.NEXT_DAY);
        });
    }
}
