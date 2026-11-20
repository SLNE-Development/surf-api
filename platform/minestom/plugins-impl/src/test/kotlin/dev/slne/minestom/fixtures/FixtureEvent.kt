package dev.slne.minestom.fixtures

import dev.slne.surf.api.event.SurfSyncEvent
import java.util.concurrent.CopyOnWriteArrayList

/**
 * An event that the fixture plugins listen to through the surf event bus.
 */
class FixtureEvent : SurfSyncEvent()

/**
 * The order in which the beans of the two-plugin fixture were called.
 */
object FixtureLog {
    val events = CopyOnWriteArrayList<String>()

    operator fun plusAssign(event: String) {
        events += event
    }
}
