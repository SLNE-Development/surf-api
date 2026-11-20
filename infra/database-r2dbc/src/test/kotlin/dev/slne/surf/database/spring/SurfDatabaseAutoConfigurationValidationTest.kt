package dev.slne.surf.database.spring

import dev.slne.surf.database.config.DatabaseConfig
import dev.slne.surf.database.config.PoolConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.BeanCreationException
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.registerBean
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * Checks that [SurfDatabaseAutoConfiguration] validates a [DatabaseConfig] bean of the application. The start fails
 * before a pool is opened, so nothing here connects to a database.
 */
class SurfDatabaseAutoConfigurationValidationTest {

    @Test
    fun `an application config with a lock timeout outside the valid range fails the start`() {
        val invalid = DatabaseConfig(pool = PoolConfig(timeouts = PoolConfig.Timeouts(lockTimeoutMillis = 0)))

        val failure = assertFailsWith<BeanCreationException> {
            AnnotationConfigApplicationContext().apply {
                registerBean<DatabaseConfig> { invalid }
                register(SurfDatabaseAutoConfiguration::class.java)
                refresh()
            }
        }

        val cause = assertIs<IllegalArgumentException>(failure.mostSpecificCause)
        assertContains(cause.message.orEmpty(), "lockTimeoutMillis")
    }
}
