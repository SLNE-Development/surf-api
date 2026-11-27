package dev.slne.surf.api.paper.scenario.probe

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.greedyStringArgument
import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.paper.command.executors.anyExecutorSuspend
import kotlinx.coroutines.CancellationException
import org.springframework.stereotype.Service

/**
 * `probe <request id> <action> <arguments...>`: runs the [ProbeAction] named `action` and logs its
 * reply in the form [ProbeProtocol] describes.
 */
@Service
class ProbeCommand(actions: List<ProbeAction>) : CommandAPICommand(ProbeProtocol.COMMAND) {

    private val log = logger()
    private val actionsByName = actions.associateBy { it.name }

    init {
        require(actionsByName.size == actions.size) { "Two probe actions share a name." }

        withPermission(PERMISSION)
        greedyStringArgument("request")
        anyExecutorSuspend { _, arguments ->
            val request: String by arguments
            answer(request.trim().split(' ').filter { it.isNotEmpty() })
        }
    }

    private suspend fun answer(words: List<String>) {
        val requestId = words.firstOrNull() ?: return
        val action = actionsByName[words.getOrNull(1)]

        if (action == null) {
            reply(ProbeProtocol.ERROR, requestId, "unknown action ${words.getOrNull(1)}")
            return
        }

        try {
            reply(ProbeProtocol.REPLY, requestId, action.run(words.drop(2)))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            reply(ProbeProtocol.ERROR, requestId, failure.toString())
        }
    }

    private fun reply(kind: String, requestId: String, text: String) {
        log.atInfo()
            .log("%s %s %s", kind, requestId, text.replace('\n', ' '))
    }

    private companion object {
        const val PERMISSION = "surf.scenario.probe"
    }
}
