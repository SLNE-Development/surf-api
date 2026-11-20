package dev.slne.surf.microservice.runtime.lifecycle

import kotlin.test.*

class RuntimeComponentLifecycleTest {
    private val component = RecordingComponent()
    private val lifecycle = RuntimeComponentLifecycle(component, phase = 0)

    @Test
    fun `start and stop run the component once`() {
        lifecycle.start()
        assertTrue(lifecycle.isRunning)

        lifecycle.stop()

        assertFalse(lifecycle.isRunning)
        assertEquals(listOf("start", "stop"), component.calls)
    }

    @Test
    fun `a stopped component cannot be started again`() {
        lifecycle.start()
        lifecycle.stop()

        assertFailsWith<IllegalStateException> { lifecycle.start() }

        assertEquals(listOf("start", "stop"), component.calls, "the component never sees the second start")
    }

    @Test
    fun `stop does nothing before start`() {
        lifecycle.stop()

        assertEquals(emptyList(), component.calls)
    }

    private class RecordingComponent : RuntimeComponent {
        val calls = mutableListOf<String>()
        override val name = "recording component"

        override suspend fun start() {
            calls += "start"
        }

        override suspend fun stop() {
            calls += "stop"
        }
    }
}
