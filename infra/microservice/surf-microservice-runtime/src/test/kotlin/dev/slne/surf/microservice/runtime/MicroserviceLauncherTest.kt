package dev.slne.surf.microservice.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class MicroserviceLauncherTest {

    @Test
    fun `exits once a shutdown signal stopped the lifecycle`() {
        val result = ProbeJvm.run(LauncherExitProbe::class.java, timeout = 30.seconds)

        assertEquals(LauncherExitProbe.SIGNAL_EXIT_CODE, result.exitCode, result.errorOutput)
        assertTrue(LauncherExitProbe.STOPPED in result.output.lines(), result.output)
    }
}
