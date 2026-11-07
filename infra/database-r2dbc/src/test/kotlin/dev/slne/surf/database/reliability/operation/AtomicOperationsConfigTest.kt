package dev.slne.surf.database.reliability.operation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds

class AtomicOperationsConfigTest {

    @Test
    fun rejectsLockTimeoutsBelowOneMillisecond() {
        assertFailsWith<IllegalArgumentException> { AtomicOperationsConfig(lockTimeout = 999.microseconds) }
        assertFailsWith<IllegalArgumentException> { AtomicOperationsConfig(lockTimeout = 0.milliseconds) }
    }

    @Test
    fun rejectsLockTimeoutsAbovePostgresLimit() {
        assertFailsWith<IllegalArgumentException> { AtomicOperationsConfig(lockTimeout = 25.days) }
    }

    @Test
    fun acceptsLockTimeoutsWithinBounds() {
        assertEquals(1.milliseconds, AtomicOperationsConfig(lockTimeout = 1.milliseconds).lockTimeout)
        assertEquals(Int.MAX_VALUE.milliseconds, AtomicOperationsConfig(lockTimeout = Int.MAX_VALUE.milliseconds).lockTimeout)
    }
}
