package dev.slne.surf.api.nats

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NatsClientNamesTest {

    @Test
    fun `a plain plugin name is lowercased`() {
        assertEquals("surfchat", NatsClientNames.of("SurfChat"))
    }

    @Test
    fun `runs of other characters become one hyphen`() {
        assertEquals("surf-chat-v2", NatsClientNames.of("Surf Chat__v2"))
    }

    @Test
    fun `hyphens at either end are dropped`() {
        assertEquals("lobby", NatsClientNames.of("-.Lobby!-"))
    }

    @Test
    fun `non-ASCII letters count as separators`() {
        assertEquals("sch-n", NatsClientNames.of("Schön"))
    }

    @Test
    fun `an already valid name is unchanged`() {
        assertEquals("surf-nats-test", NatsClientNames.of("surf-nats-test"))
    }

    @Test
    fun `a name without letters or digits is rejected`() {
        assertFailsWith<IllegalArgumentException> { NatsClientNames.of("---") }
        assertFailsWith<IllegalArgumentException> { NatsClientNames.of("") }
    }
}
