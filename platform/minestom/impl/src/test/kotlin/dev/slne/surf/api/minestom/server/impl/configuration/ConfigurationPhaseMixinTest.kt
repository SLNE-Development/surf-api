package dev.slne.surf.api.minestom.server.impl.configuration

import dev.slne.surf.api.minestom.server.configuration.ConfigurationTask
import dev.slne.surf.api.minestom.server.configuration.ConfigurationTaskId
import dev.slne.surf.api.minestom.server.configuration.ConfigurationTasks
import dev.slne.surf.api.minestom.server.surfMinestomServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestConnection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Tests that `ConnectionManagerMixin` hands `ConnectionManager#doConfiguration` to the surf
 * configuration phase while the phase is installed, and leaves it to Minestom otherwise.
 */
@EnvTest
class ConfigurationPhaseMixinTest {

    @Test
    fun `the installed configuration phase runs in place of Minestom's`(env: Env) {
        val runs = CopyOnWriteArrayList<Boolean>()
        val server = surfMinestomServer {
            withConfigurationPhase {
                // Both wait for a client response that a test connection never sends.
                remove(ConfigurationTasks.AWAIT_SETTINGS)
                remove(ConfigurationTasks.SYNCHRONIZE_REGISTRIES)
                first(RECORD, ConfigurationTask { context -> runs += context.isFirstConfig })
            }
        }

        val instance = env.createFlatInstance()
        try {
            val player = connect(env.createConnection(), instance)
            assertEquals(listOf(false), runs)
            assertTrue(player.isOnline)
            player.remove()
        } finally {
            env.destroyInstance(instance)
            server.shutdown()
        }
    }

    @Test
    fun `without the configuration phase Minestom configures the player`(env: Env) {
        val server = surfMinestomServer()
        val instance = env.createFlatInstance()
        try {
            val player = connect(env.createConnection(), instance)
            assertTrue(player.isOnline)
            player.remove()
        } finally {
            env.destroyInstance(instance)
            server.shutdown()
        }
    }

    private fun connect(connection: TestConnection, instance: Instance): Player {
        val player = CompletableFuture<Player>()
        Thread.startVirtualThread {
            runCatching { connection.connect(instance, Pos.ZERO) }
                .onSuccess(player::complete)
                .onFailure(player::completeExceptionally)
        }
        return player.join()
    }

    private companion object {
        val RECORD = ConfigurationTaskId("record")
    }
}
