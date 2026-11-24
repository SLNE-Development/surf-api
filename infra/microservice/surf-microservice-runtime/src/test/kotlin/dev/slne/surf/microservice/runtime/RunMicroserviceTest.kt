package dev.slne.surf.microservice.runtime

import dev.slne.probe.microservice.*
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.seconds

/**
 * Starts a Spring Boot microservice through [runMicroservice] in a separate JVM, because a
 * microservice owns its JVM.
 */
class RunMicroserviceTest {

    private val probe = Class.forName("dev.slne.probe.microservice.ProbeApplicationKt")

    @Test
    fun `a microservice runs its hooks in order, delivers events to its handler beans and exits with 0 after a shutdown request`() {
        val result = ProbeJvm.run(probe)

        assertEquals(0, result.exitCode, result.errorOutput + result.output)
        assertEquals(
            listOf(
                "probe:onEnable name=probe",
                "probe:event source=onEnable",
                "probe:afterEnable health=probeHealth:true",
                "probe:beforeDisable",
                "probe:onDisable",
            ),
            result.output.lines().filter { it.startsWith("probe:") },
        )
    }

    @Test
    fun `a failing start disables what started and exits with 1`() {
        val result = ProbeJvm.run(probe, listOf(FAIL_ENABLE))

        assertEquals(1, result.exitCode, result.errorOutput + result.output)
        val events = result.output.lines().filter { it.startsWith("probe:") }
        assertContains(events, "probe:onDisable")
        assertFalse("probe:afterEnable" in result.output, "enable stops at the failing hook")
        assertContains(result.errorOutput + result.output, "enable failed")
    }

    @Test
    fun `closing the context from a bean ends the process with the exit code of SpringApplication exit`() {
        val result = ProbeJvm.run(probe, listOf(EXIT_CONTEXT), timeout = 30.seconds)

        assertEquals(CONTEXT_EXIT_CODE, result.exitCode, result.errorOutput + result.output)
        assertContains(result.output.lines(), "probe:onDisable")
    }

    @Test
    fun `logging stays usable until a context closed by another thread finished its disable hooks`() {
        val result = ProbeJvm.run(
            probe,
            listOf(EXIT_CONTEXT, LATE_DISABLE_LOG),
            timeout = 30.seconds,
        )

        assertEquals(CONTEXT_EXIT_CODE, result.exitCode, result.errorOutput + result.output)
        assertContains(result.output, LATE_LOG_MESSAGE)
        assertContains(result.output.lines(), "probe:onDisable")
    }
}
