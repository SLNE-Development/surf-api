package dev.slne.surf.microservice.runtime.logging

import dev.slne.surf.microservice.runtime.ProbeJvm
import org.junit.jupiter.api.AfterEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Slf4jProviderTest {
    private val originalProvider: String? = System.getProperty(Slf4jProvider.PROPERTY)

    @AfterEach
    fun restoreProperty() {
        if (originalProvider == null) {
            System.clearProperty(Slf4jProvider.PROPERTY)
        } else {
            System.setProperty(Slf4jProvider.PROPERTY, originalProvider)
        }
    }

    @Test
    fun `selects the log4j provider when none is configured`() {
        System.clearProperty(Slf4jProvider.PROPERTY)

        Slf4jProvider.selectLog4j()

        assertEquals(Slf4jProvider.LOG4J, System.getProperty(Slf4jProvider.PROPERTY))
    }

    @Test
    fun `keeps a configured provider`() {
        System.setProperty(Slf4jProvider.PROPERTY, CONFIGURED_PROVIDER)

        Slf4jProvider.selectLog4j()

        assertEquals(CONFIGURED_PROVIDER, System.getProperty(Slf4jProvider.PROPERTY))
    }

    // Canary for the defect's precondition: fails once the class path no longer holds a second provider, for
    // example when spark stops bundling slf4j-simple. The pin is then obsolete.
    @Test
    fun `the class path contains a second provider that SLF4J reports`() {
        val probe = runProbe()

        assertTrue(MULTIPLE_PROVIDERS_WARNING in probe.errorOutput, probe.errorOutput)
    }

    @Test
    fun `binds SLF4J to log4j without scanning for providers once pinned`() {
        val probe = runProbe(Slf4jBindingProbe.PIN_ARGUMENT)

        assertEquals(LOG4J_LOGGER_FACTORY, probe.output.lines().last { it.isNotBlank() })
        assertFalse(MULTIPLE_PROVIDERS_WARNING in probe.errorOutput, probe.errorOutput)
    }

    /**
     * Runs [Slf4jBindingProbe] in a new JVM, since SLF4J binds once per JVM.
     */
    private fun runProbe(vararg args: String): ProbeJvm.Result {
        val result = ProbeJvm.run(Slf4jBindingProbe::class.java, args.asList())
        check(result.exitCode == 0) { "The probe JVM failed: ${result.errorOutput}" }
        return result
    }

    private companion object {
        const val CONFIGURED_PROVIDER = "org.example.ConfiguredProvider"
        const val LOG4J_LOGGER_FACTORY = "org.apache.logging.slf4j.Log4jLoggerFactory"
        const val MULTIPLE_PROVIDERS_WARNING = "multiple SLF4J providers"
    }
}
