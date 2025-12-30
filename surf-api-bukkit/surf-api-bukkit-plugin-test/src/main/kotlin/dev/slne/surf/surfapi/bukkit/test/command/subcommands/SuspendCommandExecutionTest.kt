package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.suspendexecution.*

class SuspendCommandExecutionTest(name: String) : CommandAPICommand(name) {
    init {
        withSubcommands(
            SuspendCommandExecutionDelayTest("delay"),
            SuspendCommandExecutionSyntaxExceptionTest("syntaxException"),
            SuspendCommandExecutionUncheckedExceptionTest("uncheckedException"),
            SuspendCommandExecutionCancellationExceptionTest("cancellationException"),
            SuspendCommandExecutionDelayWithSuspendArgTest("delayWithSuspendArg")
        )
    }
}