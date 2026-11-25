package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.api.paper.SurfApiPaper
import dev.slne.surf.api.paper.command.executors.anyExecutorSuspend
import dev.slne.surf.api.paper.time.SkipOperations

class SmoothTimeSkip(name: String) : CommandAPICommand(name) {
    init {
        anyExecutorSuspend { sender, _ ->
            SurfApiPaper.instance.skipTimeSmoothly(SkipOperations.NEXT_DAY)
            sender.sendMessage("Time skipped smoothly")
        }
    }
}
