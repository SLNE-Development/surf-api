package dev.slne.surf.database.config

import dev.slne.surf.database.config.internal.inWholeSecondsRoundedUp
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Checks the value ranges of [DatabaseConfig] that the drivers and servers impose. Nothing here
 * connects to a database.
 */
class DatabaseConfigValidationTest {

    private fun configWithLockTimeout(millis: Long) = DatabaseConfig(
        pool = PoolConfig(
            timeouts = PoolConfig.Timeouts(
                lockTimeoutMillis = millis,
            ),
        ),
    )

    @Test
    fun `accepts the defaults`() {
        DatabaseConfig().validate()
    }

    @Test
    fun `accepts the bounds of the lock timeout`() {
        configWithLockTimeout(1).validate()
        configWithLockTimeout(Int.MAX_VALUE.toLong()).validate()
    }

    @Test
    fun `rejects a lock timeout outside the range PostgreSQL accepts`() {
        for (millis in listOf(0L, -1L, Int.MAX_VALUE + 1L, 3_000_000_000L, Long.MAX_VALUE)) {
            val failure = assertFailsWith<IllegalArgumentException>("$millis must be rejected") {
                configWithLockTimeout(millis).validate()
            }

            assertContains(failure.message.orEmpty(), "SURF_DATABASE_POOL_LOCK_TIMEOUT_MILLIS")
        }
    }

    @Test
    fun `rounds the MariaDB lock wait up to whole seconds`() {
        assertEquals(1, Duration.ofMillis(1).inWholeSecondsRoundedUp())
        assertEquals(1, Duration.ofMillis(1_000).inWholeSecondsRoundedUp())
        assertEquals(2, Duration.ofMillis(1_001).inWholeSecondsRoundedUp())
        assertEquals(2_147_484, Duration.ofMillis(Int.MAX_VALUE.toLong()).inWholeSecondsRoundedUp())
    }
}
