package dev.slne.surf.api.paper.sharedbean.consumer

/**
 * The shape of the line [SharedGreetingConsumer] logs, shared with the test that reads it.
 *
 * The test compiles against this module, so the string it searches a server log for and the string
 * the plugin writes cannot drift apart.
 */
object SharedBeanReport {

    /**
     * The prefix that identifies the line in a server log.
     *
     * Deliberately unlike anything a server, a library or another plugin prints, so finding it is
     * never a coincidence.
     */
    const val MARKER = "SURF-SHARED-BEAN-REPORT"

    /** The `sameLoader=` field as it reads when the interface was loaded exactly once. */
    const val SAME_LOADER = "sameLoader=true"
}
