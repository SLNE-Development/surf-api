package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.error.NatsCallTimeoutError
import dev.slne.surf.nats.js.error.NatsNoRespondersError
import dev.slne.surf.nats.js.external.nats.core.NoRespondersError
import dev.slne.surf.nats.js.external.nats.core.RequestError
import dev.slne.surf.nats.js.external.nats.core.TimeoutError
import dev.slne.surf.nats.js.rpc.mapRequestError
import js.errors.ErrorOptions
import js.objects.unsafeJso
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class RequestErrorsTest {

    private fun cause(error: Throwable): ErrorOptions = unsafeJso { cause = error }

    @Test
    fun requestErrorsMapToSurfErrors() {
        val noResponders = RequestError("no responders", cause(NoRespondersError("a.b")))
        val mapped = assertIs<NatsNoRespondersError>(mapRequestError(noResponders, "a.b", 100))
        assertEquals("a.b", mapped.subject)

        val direct = assertIs<NatsNoRespondersError>(mapRequestError(NoRespondersError("c.d"), "c.d", 100))
        assertEquals("c.d", direct.subject)

        val timeout = assertIs<NatsCallTimeoutError>(mapRequestError(TimeoutError(), "a.b", 250))
        assertEquals("a.b", timeout.subject)
        assertEquals(250, timeout.timeoutMs)

        val wrappedTimeout = RequestError("timeout", cause(TimeoutError()))
        assertIs<NatsCallTimeoutError>(mapRequestError(wrappedTimeout, "a.b", 250))

        val other = IllegalStateException("other")
        assertSame(other, mapRequestError(other, "a.b", 250))
    }
}
