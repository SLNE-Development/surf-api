package dev.slne.surf.nats.event

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.TimeSource

/**
 * Remembers the IDs of delivered events so that a listener acts on each logical event once.
 *
 * Core NATS never redelivers an event, and the JVM client publishes every event once under a new
 * [NatsEventContext.eventId]. An ID arrives twice only when a publisher sends it twice on purpose, such as a TypeScript
 * client that retries a publish with the same `eventId` or an external publisher that reuses a `Surf-Event-Id`.
 *
 * An ID is remembered for [retention] after its first delivery; a repeat does not extend it. At most [maxEntries] IDs
 * are remembered, and the oldest is forgotten first.
 * A repeat that arrives after its ID was forgotten counts as a first delivery again, so both limits must cover the
 * time in which a publisher repeats an event.
 *
 * Thread-safe. Share one instance between the listeners that must not act twice on the same event.
 *
 * @param timeSource the clock of [retention]; tests may pass a `TestTimeSource`.
 * @throws IllegalArgumentException if [maxEntries] or [retention] is not positive.
 */
public class NatsEventDeduplicator(
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    private val retention: Duration = DEFAULT_RETENTION,
    timeSource: TimeSource = TimeSource.Monotonic,
) {
    init {
        require(maxEntries > 0) { "maxEntries must be positive, was $maxEntries" }
        require(retention.isPositive()) { "retention must be positive, was $retention" }
    }

    private val origin = timeSource.markNow()

    // Event ID to the time of its first delivery in nanoseconds since origin, oldest first
    private val firstDeliveries = LinkedHashMap<String, Long>()

    /**
     * Records [eventId] and returns `true` if it was not delivered within [retention] before.
     */
    public fun firstDelivery(eventId: String): Boolean {
        synchronized(firstDeliveries) {
            // Read under the lock, so that the insertion order is the time order forgetExpired relies on
            val now = origin.elapsedNow().inWholeNanoseconds
            forgetExpired(now)
            if (firstDeliveries.containsKey(eventId)) return false

            firstDeliveries.put(eventId, now)
            if (firstDeliveries.size > maxEntries) firstDeliveries.pollFirstEntry()
            return true
        }
    }

    /**
     * Records the event of the calling listener like [firstDelivery] and returns `true` if it was not delivered
     * before. An event without an ID cannot be recognized and always counts as a first delivery.
     *
     * @throws IllegalStateException if called outside a listener's coroutine context.
     */
    public suspend fun firstDelivery(): Boolean {
        val eventId = natsEventContext().eventId ?: return true
        return firstDelivery(eventId)
    }

    private fun forgetExpired(now: Long) {
        val oldestKept = now - retention.inWholeNanoseconds
        while (firstDeliveries.isNotEmpty() && firstDeliveries.firstEntry().value <= oldestKept) {
            firstDeliveries.pollFirstEntry()
        }
    }

    public companion object {
        /**
         * Default of `maxEntries`.
         */
        public const val DEFAULT_MAX_ENTRIES: Int = 10_000

        /**
         * Default of `retention`.
         */
        public val DEFAULT_RETENTION: Duration = 7.days
    }
}
