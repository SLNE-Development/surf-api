package dev.slne.surf.api.minestom.coroutine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.instance.Instance
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

@EnvTest
class WithEntityTest {

    @Test
    fun `withEntity runs the block on the entity scheduler and returns its value`(env: Env) {
        val instance = env.createFlatInstance()
        val scope = CoroutineScope(Job())

        try {
            val zombie = zombieIn(instance)

            val result = scope.async(start = CoroutineStart.UNDISPATCHED) {
                zombie.withEntity { it.entityType }
            }
            tickUntilCompleted(env, result)

            assertEquals(EntityType.ZOMBIE, runBlocking { result.await() })
        } finally {
            scope.cancel()
            env.destroyInstance(instance)
        }
    }

    @Test
    fun `withEntity cancels instead of hanging when the entity is removed after scheduling`(env: Env) {
        val instance = env.createFlatInstance()
        val scope = CoroutineScope(Job())

        try {
            val zombie = zombieIn(instance)
            val ran = AtomicBoolean(false)

            // Undispatched: the task is scheduled by the time async returns, before any tick.
            val result = scope.async(start = CoroutineStart.UNDISPATCHED) {
                zombie.withEntity { ran.set(true) }
            }
            zombie.remove()
            assertTrue(zombie.isRemoved)

            tickUntilCompleted(env, result)

            assertTrue(result.isCancelled)
            assertThrows(CancellationException::class.java) { runBlocking { result.await() } }
            assertFalse(ran.get())
        } finally {
            scope.cancel()
            env.destroyInstance(instance)
        }
    }

    @Test
    fun `withEntity does not run its block once the caller is cancelled`(env: Env) {
        val instance = env.createFlatInstance()
        val scope = CoroutineScope(Job())

        try {
            val zombie = zombieIn(instance)
            val ran = AtomicBoolean(false)

            val result = scope.async(start = CoroutineStart.UNDISPATCHED) {
                zombie.withEntity { ran.set(true) }
            }
            result.cancel()
            repeat(3) { env.tick() }

            assertTrue(result.isCancelled)
            assertFalse(ran.get())
        } finally {
            scope.cancel()
            env.destroyInstance(instance)
        }
    }

    private fun zombieIn(instance: Instance): Entity =
        Entity(EntityType.ZOMBIE).also { it.setInstance(instance, Pos.ZERO).join() }

    /**
     * Ticks the server until [deferred] is done, failing the test instead of hanging when it is not.
     */
    private fun tickUntilCompleted(env: Env, deferred: Deferred<*>) {
        val deadline = System.nanoTime() + TIMEOUT_NANOS
        while (!deferred.isCompleted) {
            check(System.nanoTime() < deadline) { "withEntity did not complete within the timeout" }
            env.tick()
            Thread.sleep(1)
        }
    }

    private companion object {
        const val TIMEOUT_NANOS = 5_000_000_000L
    }
}
