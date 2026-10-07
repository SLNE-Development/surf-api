package dev.slne.surf.nats.event

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EventSubjectsTest {

    @Test
    fun `kebab case splits words, acronyms and digits`() {
        assertEquals("player-health-changed", EventSubjects.kebabCase("PlayerHealthChanged"))
        assertEquals("nats-event", EventSubjects.kebabCase("NATSEvent"))
        assertEquals("event2-created", EventSubjects.kebabCase("Event2Created"))
        assertEquals("http-server2-up", EventSubjects.kebabCase("HTTPServer2Up"))
        assertEquals("ping", EventSubjects.kebabCase("Ping"))
    }

    @Test
    fun `valid subjects are accepted`() {
        for (subject in listOf("a", "punishment.created", "a.b-c.d_e", "Mixed.Case")) {
            assertEquals(subject, EventSubjects.requireValidSubject(subject, "test"))
        }
    }

    @Test
    fun `invalid subjects are rejected`() {
        for (subject in listOf("", "a b", "a..b", "a.*", "a.>", ".a", "a.", "a\tb")) {
            assertFailsWith<IllegalArgumentException>("subject '$subject'") {
                EventSubjects.requireValidSubject(subject, "test")
            }
        }
    }
}
