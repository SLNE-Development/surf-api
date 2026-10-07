package dev.slne.surf.nats.event

import dev.slne.surf.nats.subject.Subjects
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EventSubjectsTest {

    @Test
    fun `kebab case splits words, acronyms and digits`() {
        assertEquals("player-health-changed", Subjects.kebabCase("PlayerHealthChanged"))
        assertEquals("nats-event", Subjects.kebabCase("NATSEvent"))
        assertEquals("event2-created", Subjects.kebabCase("Event2Created"))
        assertEquals("http-server2-up", Subjects.kebabCase("HTTPServer2Up"))
        assertEquals("ping", Subjects.kebabCase("Ping"))
    }

    @Test
    fun `valid subjects are accepted`() {
        for (subject in listOf("a", "punishment.created", "a.b-c.d_e", "Mixed.Case")) {
            assertEquals(subject, Subjects.requireValidSubject(subject, "test"))
        }
    }

    @Test
    fun `invalid subjects are rejected`() {
        for (subject in listOf("", "a b", "a..b", "a.*", "a.>", ".a", "a.", "a\tb")) {
            assertFailsWith<IllegalArgumentException>("subject '$subject'") {
                Subjects.requireValidSubject(subject, "test")
            }
        }
    }
}
