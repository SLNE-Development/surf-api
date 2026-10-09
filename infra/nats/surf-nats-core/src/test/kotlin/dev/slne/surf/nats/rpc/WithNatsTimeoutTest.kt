package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.NatsTimeoutElement
import dev.slne.surf.nats.withNatsTimeout
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class WithNatsTimeoutTest {

    @Test
    fun `positive timeout is put into the coroutine context`() = runTest {
        val timeout = withNatsTimeout(3.seconds) { currentCoroutineContext()[NatsTimeoutElement]?.timeout }
        assertEquals(3.seconds, timeout)
    }

    @Test
    fun `zero and negative timeouts are rejected`() = runTest {
        assertFailsWith<IllegalArgumentException> { withNatsTimeout(Duration.ZERO) {} }
        assertFailsWith<IllegalArgumentException> { withNatsTimeout((-1).seconds) {} }
    }
}
