package dev.slne.surf.api.velocity.sharedbean.consumer

/**
 * The markers the consumer writes and the boot test looks for.
 */
object SharedBeanReport {

    /**
     * The consumer's plugin id.
     */
    const val PLUGIN_ID = "surf-shared-bean-consumer"

    /**
     * The prefix that identifies the report line in a proxy log.
     *
     * Deliberately unlike anything a proxy, a library or another plugin prints, so finding it is
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
}
