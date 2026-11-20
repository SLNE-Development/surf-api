package dev.slne.surf.api.minestom.server.plugins

import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Captures what reaches Log4j's root logger, which both SLF4J and commons-logging write to on the
 * test class path.
 */
object CapturedLog {

    /**
     * Runs [block] and returns the events logged while it ran, from any thread.
     */
    fun during(block: () -> Unit): List<LogEvent> {
        val appender = RecordingAppender().apply { start() }
        val context = LoggerContext.getContext(false)
        val rootLogger = context.configuration.rootLogger
        rootLogger.addAppender(appender, null, null)
        context.updateLoggers()
        try {
            block()
        } finally {
            rootLogger.removeAppender(appender.name)
            context.updateLoggers()
            appender.stop()
        }
        return appender.events.toList()
    }

    private class RecordingAppender : AbstractAppender("captured-log", null, null, true, Property.EMPTY_ARRAY) {
        val events = CopyOnWriteArrayList<LogEvent>()

        override fun append(event: LogEvent) {
            events += event.toImmutable()
        }
    }
}
