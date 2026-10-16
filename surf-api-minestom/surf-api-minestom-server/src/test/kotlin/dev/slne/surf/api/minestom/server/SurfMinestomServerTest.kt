package dev.slne.surf.api.minestom.server

import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.permission.MinestomPermissions
import dev.slne.surf.api.minestom.permission.PermissionProvider
import dev.slne.surf.api.minestom.server.impl.inventory.framework.MinestomInventoryLoader
import net.kyori.adventure.util.TriState
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
            listOf("load:first", "load:second", "disable:second", "disable:first"),
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
}
