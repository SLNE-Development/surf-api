package dev.slne.surf.microservice.runtime.spark

import me.lucko.spark.common.util.log.Logger
import me.lucko.spark.common.util.log.SparkStaticLogger

/**
 * Routes spark's static log output, such as uncaught exceptions in spark's own threads, to a [Logger].
 *
 * spark-common 1.10.172 initialises [SparkStaticLogger] with its `System.err` fallback, and
 * [SparkStaticLogger.setLogger] only replaces a `null` logger, so the call in `SparkPlatform`'s constructor has no
 * effect. The field is therefore set directly.
 */
internal object SparkStaticLogging {
    private const val LOGGER_FIELD = "logger"

    /**
     * Makes [logger] receive spark's static log output. Returns `false` if this spark version has no such field.
     */
    fun redirectTo(logger: Logger): Boolean = try {
        val loggerField = SparkStaticLogger::class.java.getDeclaredField(LOGGER_FIELD)

        loggerField.trySetAccessible()
        loggerField.set(null, logger)

        true
    } catch (_: ReflectiveOperationException) {
        false
    }
}
