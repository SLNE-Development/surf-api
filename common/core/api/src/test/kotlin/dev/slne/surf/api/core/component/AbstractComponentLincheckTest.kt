package dev.slne.surf.api.core.component

import dev.slne.surf.api.shared.api.component.SurfComponentMeta
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import kotlinx.coroutines.runBlocking
import org.jetbrains.lincheck.Lincheck
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(InternalSurfApi::class)
class AbstractComponentLincheckTest {

    @SurfComponentMeta
    private class CountingComponent : AbstractComponent() {
        val loadCalls = AtomicInteger()
        val enableCalls = AtomicInteger()
        val disableCalls = AtomicInteger()

        override suspend fun onLoad() {
            loadCalls.incrementAndGet()
        }

        override suspend fun onEnable() {
            enableCalls.incrementAndGet()
        }

        override suspend fun onDisable() {
            disableCalls.incrementAndGet()
        }
    }

    @Test
    fun `lifecycle phases execute at most once under concurrent invocation`() {
        Lincheck.runConcurrentTest {
            val component = CountingComponent()
            val threads = List(2) {
                Thread {
                    runBlocking {
                        component.load()
                        component.enable()
                        component.disable()
                    }
                }
            }
            threads.forEach(Thread::start)
            threads.forEach(Thread::join)

            assertEquals(1, component.loadCalls.get(), "onLoad must run exactly once")
            assertEquals(1, component.enableCalls.get(), "onEnable must run exactly once")
            assertEquals(1, component.disableCalls.get(), "onDisable must run exactly once")
        }
    }
}
