package dev.slne.surf.api.paper.scenario.probe

/**
 * One request the probe answers for scenario tests.
 *
 * [ProbeCommand] runs the bean whose [name] matches the request and logs the result under the
 * request id. A new scenario request is a new bean of this plugin that implements this interface;
 * names must be unique.
 *
 * Requests arrive on the server thread inside the plugin's coroutine scope. An action that touches
 * a player, a block or an entity switches to the thread that owns it through `PaperDispatchers`,
 * so it also works on Folia.
 */
interface ProbeAction {

    /**
     * The word that selects this action, such as `inventory`.
     */
    val name: String

    /**
     * Answers one request.
     *
     * A thrown exception becomes the request's error reply with the exception's message.
     *
     * @param arguments the words after [name]
     * @return the reply, one line without line breaks
     */
    suspend fun run(arguments: List<String>): String
}
