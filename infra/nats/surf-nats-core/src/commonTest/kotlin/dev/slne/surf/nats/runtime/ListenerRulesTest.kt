package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.rpc.Recipients
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ListenerRulesTest {

    @Test
    fun ignoringOwnEventsRequiresAllInstances() {
        assertEquals(Recipients.ALL_INSTANCES, ListenerRules.DEFAULT_RECIPIENTS)
        assertNull(ListenerRules.problem(Recipients.ALL_INSTANCES, ignoreFromThisClient = true))
        assertNull(ListenerRules.problem(Recipients.ANY_INSTANCE, ignoreFromThisClient = false))
        assertEquals(
            "ignoreFromThisClient requires recipients = ALL_INSTANCES",
            ListenerRules.problem(Recipients.ANY_INSTANCE, ignoreFromThisClient = true),
        )
    }

    @Test
    fun anyInstanceListenersShareTheClientQueueGroup() {
        assertEquals("surf-panel", ListenerRules.queueGroup(Recipients.ANY_INSTANCE, "surf-panel"))
        assertNull(ListenerRules.queueGroup(Recipients.ALL_INSTANCES, "surf-panel"))
    }
}
