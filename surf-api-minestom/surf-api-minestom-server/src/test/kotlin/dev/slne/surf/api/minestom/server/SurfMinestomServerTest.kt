package dev.slne.surf.api.minestom.server

import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.permission.MinestomPermissions
import dev.slne.surf.api.minestom.permission.PermissionProvider
import net.kyori.adventure.util.TriState
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
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
}
