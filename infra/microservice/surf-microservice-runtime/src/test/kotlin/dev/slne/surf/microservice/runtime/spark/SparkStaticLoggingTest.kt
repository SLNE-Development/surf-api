package dev.slne.surf.microservice.runtime.spark

import me.lucko.spark.common.util.log.Logger
import me.lucko.spark.common.util.log.SparkStaticLogger
import java.util.concurrent.CancellationException
import java.util.logging.Level
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SparkStaticLoggingTest {

    @AfterTest
    fun restoreFallback() {
        SparkStaticLogging.redirectTo(Logger.FALLBACK)
    }

    @Test
    fun `routes spark's static log output to the given logger`() {
        val recorded = mutableListOf<Pair<String, Throwable?>>()
        val logger = object : Logger {
            override fun log(level: Level, message: String) {
                recorded += message to null
            }

            override fun log(level: Level, message: String, throwable: Throwable) {
                recorded += message to throwable
            }
        }
        val failure = CancellationException()

        assertTrue(SparkStaticLogging.redirectTo(logger))
        SparkStaticLogger.log(Level.SEVERE, "Uncaught exception thrown in thread spark-test", failure)

        assertEquals(listOf<Pair<String, Throwable?>>("Uncaught exception thrown in thread spark-test" to failure), recorded)
    }
}
