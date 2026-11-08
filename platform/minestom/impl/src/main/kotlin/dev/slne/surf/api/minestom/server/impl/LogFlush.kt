package dev.slne.surf.api.minestom.server.impl

/**
 * Flushes the logs of a server whose Log4j configuration disables its own shutdown hook, e.g. with
 * `shutdownHook="disable"` and asynchronous appenders.
 *
 * Log4j is addressed by reflection, so the surf api needs no dependency on it.
 */
internal object LogFlush {

    private const val LOG_MANAGER = "org.apache.logging.log4j.LogManager"

    /**
     * Shuts Log4j down, which drains its asynchronous appenders and closes the appenders.
     *
     * Does nothing without Log4j on the class path. A failure is printed to stderr and ignored,
     * since the logging system cannot report it.
     */
    fun flush() {
        try {
            val logManager = Class.forName(LOG_MANAGER, false, LogFlush::class.java.classLoader)
            logManager.getMethod("shutdown").invoke(null)
        } catch (_: ClassNotFoundException) {
            // Log4j is not used
        } catch (_: NoClassDefFoundError) {
            // Log4j is only partially on the class path
        } catch (failure: Throwable) {
            System.err.println("Failed to flush the logs")
            (failure.cause ?: failure).printStackTrace()
        }
    }
}
