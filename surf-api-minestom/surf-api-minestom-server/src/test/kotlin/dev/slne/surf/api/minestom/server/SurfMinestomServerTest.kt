package dev.slne.surf.api.minestom.server

import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.permission.MinestomPermissions
import dev.slne.surf.api.minestom.permission.PermissionProvider
import dev.slne.surf.api.minestom.server.impl.inventory.framework.MinestomInventoryLoader
import net.kyori.adventure.util.TriState
import net.minestom.server.thread.TickThread
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch

@EnvTest
class SurfMinestomServerTest {

    @Test
    fun `starts features in order, runs commands and shuts down in reverse`(env: Env) {
        val calls = mutableListOf<String>()

        fun feature(id: String) = object : SurfMinestomFeature {
            override val id = id
            override fun load(api: SurfMinestomServer) {
                calls += "load:$id"
            }

            override fun enable(api: SurfMinestomServer) {
                calls += "enable:$id"
            }

            override fun disable(api: SurfMinestomServer) {
                calls += "disable:$id"
            }
        }

        val first = feature("first")
        val api = surfMinestomServer {
            install(first)
            install(feature("second"))
            withConfigurationPhase()
        }

        try {
            assertEquals(
                listOf("load:first", "load:second", "enable:first", "enable:second"),
                calls,
                "the features were not enabled before the start returned",
            )
            assertSame(api, SurfMinestomServer.instance)
            assertSame(first, api.feature("first"))
            assertThrows<IllegalStateException> { surfMinestomServer() }

            var ran = false
            CommandAPICommand("smoke").anyExecutor { _, _ -> ran = true }.register()
            val manager = env.process().command()
            manager.execute(manager.consoleSender, "smoke")
            assertTrue(ran)
        } finally {
            api.shutdown()
        }

        assertEquals(
            listOf(
                "load:first", "load:second", "enable:first", "enable:second",
                "disable:second", "disable:first",
            ),
            calls,
        )
        assertThrows<IllegalStateException> { SurfMinestomServer.instance }
    }

    @Test
    fun `the configured permission provider wins over the one of a feature`(env: Env) {
        val featureProvider = PermissionProvider { _, _ -> TriState.FALSE }
        val configuredProvider = PermissionProvider { _, _ -> TriState.TRUE }

        val api = surfMinestomServer {
            install(object : SurfMinestomFeature {
                override val id = "permissions"
                override fun load(api: SurfMinestomServer) {
                    MinestomPermissions.installProvider(featureProvider)
                }
            })
            permissions(configuredProvider)
        }

        try {
            assertSame(configuredProvider, MinestomPermissions.provider)
        } finally {
            api.shutdown()
        }
    }

    @Test
    fun `a rejected second start leaves the running api untouched`(env: Env) {
        val api = surfMinestomServer()

        try {
            val loader = MinestomInventoryLoader.instance
            assertThrows<IllegalStateException> { surfMinestomServer() }

            assertSame(loader, MinestomInventoryLoader.instance)
            assertSame(api, SurfMinestomServer.instance)
        } finally {
            api.shutdown()
        }
    }

    @Test
    fun `a failing load rolls back and a later start succeeds`(env: Env) {
        val calls = mutableListOf<String>()

        val failure = assertThrows<IllegalStateException> {
            surfMinestomServer {
                install(object : SurfMinestomFeature {
                    override val id = "first"
                    override fun load(api: SurfMinestomServer) {
                        calls += "load:first"
                    }

                    override fun disable(api: SurfMinestomServer) {
                        calls += "disable:first"
                    }
                })
                install(object : SurfMinestomFeature {
                    override val id = "broken"
                    override fun load(api: SurfMinestomServer) {
                        throw IllegalStateException("broken load")
                    }
                })
            }
        }

        assertEquals("broken load", failure.message)
        assertEquals(listOf("load:first", "disable:first"), calls)
        assertThrows<IllegalStateException> { SurfMinestomServer.instance }

        val api = surfMinestomServer()
        try {
            assertSame(api, SurfMinestomServer.instance)
        } finally {
            api.shutdown()
        }
    }

    @Test
    fun `a failing enable fails the start and disables every loaded feature`(env: Env) {
        val calls = mutableListOf<String>()

        val failure = assertThrows<IllegalStateException> {
            surfMinestomServer {
                install(object : SurfMinestomFeature {
                    override val id = "first"
                    override fun enable(api: SurfMinestomServer) {
                        calls += "enable:first"
                    }

                    override fun disable(api: SurfMinestomServer) {
                        calls += "disable:first"
                    }
                })
                install(object : SurfMinestomFeature {
                    override val id = "broken"
                    override fun enable(api: SurfMinestomServer) {
                        throw IllegalStateException("broken enable")
                    }

                    override fun disable(api: SurfMinestomServer) {
                        calls += "disable:broken"
                    }
                })
            }
        }

        assertEquals("broken enable", failure.message)
        assertEquals(listOf("enable:first", "disable:broken", "disable:first"), calls)
        assertThrows<IllegalStateException> { SurfMinestomServer.instance }
    }

    @Test
    fun `a failing disable does not keep the api from shutting down`(env: Env) {
        val calls = mutableListOf<String>()

        val api = surfMinestomServer {
            install(object : SurfMinestomFeature {
                override val id = "first"
                override fun disable(api: SurfMinestomServer) {
                    calls += "disable:first"
                }
            })
            install(object : SurfMinestomFeature {
                override val id = "second"
                override fun disable(api: SurfMinestomServer) {
                    throw IllegalStateException("broken disable")
                }
            })
        }

        api.shutdown()
        api.shutdown()

        assertEquals(listOf("disable:first"), calls)
        assertThrows<IllegalStateException> { SurfMinestomServer.instance }

        val restarted = surfMinestomServer()
        try {
            assertNotSame(api, restarted)
        } finally {
            restarted.shutdown()
        }
    }

    @Test
    fun `stop disables the features and then runs every shutdown action once`(env: Env) {
        val calls = mutableListOf<String>()

        val api = surfMinestomServer {
            install(object : SurfMinestomFeature {
                override val id = "feature"
                override fun disable(api: SurfMinestomServer) {
                    calls += "disable"
                }
            })
            onShutdown { calls += "first action" }
            onShutdown { throw IllegalStateException("broken action") }
            onShutdown { calls += "last action" }
        }

        api.stop()
        api.stop()

        assertEquals(listOf("disable", "first action", "last action"), calls)
        assertThrows<IllegalStateException> { SurfMinestomServer.instance }
    }

    @Test
    fun `a shutdown action that stops the server again does not wait for itself`(env: Env) {
        val calls = mutableListOf<String>()

        lateinit var api: SurfMinestomServer
        api = surfMinestomServer {
            onShutdown {
                api.stop()
                calls += "action"
            }
        }

        api.stop()

        assertEquals(listOf("action"), calls)
    }

    @Test
    fun `a concurrent stop waits until the first one has finished`(env: Env) {
        val actionRunning = CountDownLatch(1)
        val finishAction = CountDownLatch(1)

        val api = surfMinestomServer {
            onShutdown {
                actionRunning.countDown()
                finishAction.await()
            }
        }

        val first = Thread.ofPlatform().start(api::stop)
        actionRunning.await()
        val second = Thread.ofPlatform().start(api::stop)

        second.join(Duration.ofMillis(200))
        assertTrue(second.isAlive, "a concurrent stop returned before the first one had finished")

        finishAction.countDown()
        first.join()
        second.join()
    }

    @Test
    fun `stopping the server from within the start fails the start`(env: Env) {
        val failure = assertThrows<IllegalStateException> {
            surfMinestomServer {
                install(object : SurfMinestomFeature {
                    override val id = "stopping"
                    override fun enable(api: SurfMinestomServer) = api.stop()
                })
            }
        }

        assertTrue("while it starts" in failure.message.orEmpty(), failure.message)
        assertThrows<IllegalStateException> { SurfMinestomServer.instance }

        surfMinestomServer().shutdown()
    }

    @Test
    fun `a shutdown requested by another thread while the api starts fails the start`(env: Env) {
        lateinit var shutdownThread: Thread

        val failure = assertThrows<IllegalStateException> {
            surfMinestomServer {
                install(object : SurfMinestomFeature {
                    override val id = "concurrent"
                    override fun enable(api: SurfMinestomServer) {
                        shutdownThread = Thread.ofPlatform().start(api::shutdown)
                        awaitWaiting(shutdownThread)
                    }
                })
            }
        }
        assertEquals("The surf api was shut down while it started", failure.message)
        assertThrows<IllegalStateException>("the failed start left the api running") { SurfMinestomServer.instance }

        shutdownThread.join()
        surfMinestomServer().shutdown()
    }

    @Test
    fun `stop refuses to run on a tick thread`(env: Env) {
        val api = surfMinestomServer()

        try {
            val failure = CompletableFuture<Throwable?>()
            object : TickThread("surf-test-tick") {
                override fun run() {
                    failure.complete(runCatching { api.stop() }.exceptionOrNull())
                }
            }.start()

            assertInstanceOf(IllegalStateException::class.java, failure.join())
            assertSame(api, SurfMinestomServer.instance, "the refused stop shut the api down")
        } finally {
            api.shutdown()
        }
    }

    /**
     * Waits until [thread] parks, e.g. on the lifecycle lock of the api.
     */
    private fun awaitWaiting(thread: Thread) {
        val deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos()
        while (thread.state != Thread.State.WAITING) {
            check(System.nanoTime() < deadline) { "${thread.name} never started waiting" }
            Thread.sleep(1)
        }
    }
}
