package dev.slne.surf.api.paper.sharedbean.consumer

/**
 * The markers the consumer writes and the boot test looks for.
 */
object SharedBeanReport {

    /**
     * The prefix that identifies the report line in a server log.
     *
     * Deliberately unlike anything a server, a library or another plugin prints, so finding it is
     * never a coincidence.
     */
    const val MARKER = "SURF-SHARED-BEAN-REPORT"

    /**
     * The prefix of the phase lines both test plugins log.
     */
    const val SPIKE_MARKER = "SURF-SPRING-SPIKE"

    /**
     * The name of the consumer's command bean, which the boot test runs from the console.
     */
    const val COMMAND = "surfspike"

    /**
     * The name of the consumer's suspending command bean, which the boot test runs from the
     * console.
     */
    const val SCOPE_COMMAND = "surfspikescope"

    /**
     * The name of the consumer's NATS command bean, which the boot test runs from the console once
     * the server is up.
     */
    const val NATS_COMMAND = "surfspikenats"

    /**
     * The name of the consumer's inventory command bean, which the boot test runs from the console
     * to exercise item insertion on the server's own inventories.
     */
    const val INVENTORY_COMMAND = "surfspikeinventory"
}
