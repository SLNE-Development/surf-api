package dev.slne.surf.api.paper.scenario.probe

/**
 * The console command and the log lines through which scenario tests talk to the probe.
 *
 * A test sends `probe <request id> <action> <arguments...>` to the console. The probe answers with
 * one log line, `SURF-SCENARIO-PROBE reply <request id> <result>` or
 * `SURF-SCENARIO-PROBE error <request id> <message>`, and logs what players do as
 * `SURF-SCENARIO-PROBE event <kind> <player> [<detail>]`.
 */
object ProbeProtocol {

    /**
     * The prefix of every line the probe logs.
     *
     * Deliberately unlike anything a server, a library or another plugin prints, so finding it is
     * never a coincidence.
     */
    const val MARKER = "SURF-SCENARIO-PROBE"

    const val COMMAND = "probe"

    const val REPLY = "$MARKER reply"

    const val ERROR = "$MARKER error"

    const val EVENT = "$MARKER event"

    const val JOIN = "join"

    const val QUIT = "quit"

    const val DEATH = "death"

    const val RESPAWN = "respawn"

    /**
     * Logged with the type of the inventory the player opened as detail.
     */
    const val OPEN = "open"

    /**
     * Logged with the type of the closed inventory and the close reason as detail.
     */
    const val CLOSE = "close"
}
