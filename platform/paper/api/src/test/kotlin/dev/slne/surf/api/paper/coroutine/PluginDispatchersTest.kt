package dev.slne.surf.api.paper.coroutine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.bukkit.World
import org.bukkit.entity.Entity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PluginDispatchersTest {

    private val scheduler = FakeScheduler()
    private val dispatchers = PluginDispatchers("test", scheduler)
    private val failures = ConcurrentLinkedQueue<Throwable>()
    private val scope = CoroutineScope(
        SupervisorJob() + CoroutineExceptionHandler { _, failure -> failures += failure },
    )

    @AfterEach
    fun close() {
        scope.cancel()
        dispatchers.close()
    }

    @Test
    fun `work dispatched before enable waits for enable`() = runBlocking {
        val result = scope.async(dispatchers.main) { "ran" }

        scheduler.drain()
        assertFalse(result.isCompleted)

        dispatchers.enable()
        scheduler.drain()
        assertEquals("ran", withTimeout(TIMEOUT) { result.await() })
    }

    @Test
    fun `work dispatched after disable is cancelled and finishes off the server thread`() = runBlocking {
        dispatchers.enable()
        dispatchers.beginDisable()
        var resumed = false

        val job = scope.launch(dispatchers.main) { resumed = true }

        withTimeout(TIMEOUT) { job.join() }
        assertTrue(job.isCancelled)
        assertFalse(resumed)
    }

    @Test
    fun `work Paper holds when the plugin disables is released, not lost`() = runBlocking {
        dispatchers.enable()
        val job = scope.launch(dispatchers.main) { }
        awaitQueued(1)

        // Paper drops a disabled plugin's tasks without calling them; nothing drains the queue.
        dispatchers.beginDisable()

        withTimeout(TIMEOUT) { job.join() }
        assertTrue(job.isCancelled)
    }

    @Test
    fun `a scheduler that refuses the task cancels the coroutine instead of throwing`() = runBlocking {
        dispatchers.enable()
        scheduler.refuse = true

        val job = scope.launch(dispatchers.main) { }

        withTimeout(TIMEOUT) { job.join() }
        assertTrue(job.isCancelled)
    }

    @Test
    fun `a retired entity cancels the work, which never runs, and not on the retiring thread`() = runBlocking {
        dispatchers.enable()
        val entity = fakeEntity()
        var ran = false
        val rejectionThread = CompletableThread()

        val job = scope.launch(dispatchers.entity(entity)) { ran = true }
        job.invokeOnCompletion { rejectionThread.record() }
        awaitQueued(1)

        scheduler.retire(entity)

        withTimeout(TIMEOUT) { job.join() }
        assertTrue(job.isCancelled)
        assertFalse(ran)
        assertNotSame(scheduler.retiringThread, rejectionThread.await())
    }

    @Test
    fun `an entity that is already retired cancels the work`() = runBlocking {
        dispatchers.enable()
        val entity = fakeEntity()
        scheduler.retiredBeforehand += entity

        val job = scope.launch(dispatchers.entity(entity)) { }

        withTimeout(TIMEOUT) { job.join() }
        assertTrue(job.isCancelled)
    }

    @Test
    fun `withEntity never runs its block once the entity is retired`() = runBlocking {
        dispatchers.enable()
        val entity = fakeEntity()
        var ran = false

        val call = scope.async(PaperDispatchersElement(dispatchers)) {
            entity.withEntity { ran = true }
        }
        awaitQueued(1)
        scheduler.retire(entity)

        assertFailsWith<CancellationException> { withTimeout(TIMEOUT) { call.await() } }
        assertFalse(ran)
    }

    @Test
    fun `withEntity runs its block on the entity's thread and returns the result`() = runBlocking {
        dispatchers.enable()
        val entity = fakeEntity()

        val call = scope.async(PaperDispatchersElement(dispatchers)) {
            entity.withEntity { "done" }
        }
        awaitQueued(1)
        scheduler.drain()

        assertEquals("done", withTimeout(TIMEOUT) { call.await() })
    }

    @Test
    fun `a lifecycle hook can wait for work that needs the thread it blocks`() {
        dispatchers.enable()
        val hookThread = Thread.currentThread()
        scheduler.globalThread = hookThread

        val ranOn = runBlocking {
            dispatchers.lifecycle {
                scope.async(Dispatchers.Default) {
                    withContext(dispatchers.main) { Thread.currentThread() }
                }.await()
            }
        }

        assertSame(hookThread, ranOn)
    }

    @Test
    fun `an enable hook can wait for work parked during load`() {
        scheduler.globalThread = Thread.currentThread()
        val parked = runBlocking(Dispatchers.Default) {
            scope.async(dispatchers.main) { Thread.currentThread() }
        }
        assertFalse(parked.isCompleted)
        assertEquals(0, scheduler.queued)

        val ranOn = runBlocking {
            dispatchers.lifecycle {
                dispatchers.enable()
                withTimeout(TIMEOUT) { parked.await() }
            }
        }

        assertSame(scheduler.globalThread, ranOn)
    }

    @Test
    fun `a lifecycle hook can wait for work already handed to Paper`() {
        dispatchers.enable()
        val handedOver = scope.async(dispatchers.main) { Thread.currentThread() }
        awaitQueued(1)
        scheduler.globalThread = Thread.currentThread()

        val ranOn = runBlocking {
            dispatchers.lifecycle { withTimeout(TIMEOUT) { handedOver.await() } }
        }

        assertSame(scheduler.globalThread, ranOn)
    }

    @Test
    fun `a hook can wait for main work another plugin's dispatchers dispatched`() {
        val other = PluginDispatchers("other", scheduler)
        other.enable()
        scheduler.globalThread = Thread.currentThread()

        try {
            val ranOn = runBlocking {
                dispatchers.lifecycle {
                    scope.async(Dispatchers.Default) {
                        withContext(other.main) { Thread.currentThread() }
                    }.await()
                }
            }
            assertSame(scheduler.globalThread, ranOn)
        } finally {
            other.close()
        }
    }

    @Test
    fun `on a regionized server a lifecycle phase runs main work although its thread does not tick`() {
        scheduler.isRegionized = true
        val phaseThread = Thread.currentThread()

        val ranOn = runBlocking {
            dispatchers.lifecycle {
                dispatchers.enable()
                scope.async(Dispatchers.Default) {
                    withContext(dispatchers.main) { Thread.currentThread() }
                }.await()
            }
        }

        assertSame(phaseThread, ranOn)
    }

    @Test
    fun `entity work routed into a lifecycle phase is retired if the entity is removed before it runs`() {
        dispatchers.enable()
        scheduler.globalThread = Thread.currentThread()
        val entity = fakeEntity()
        var ran = false

        runBlocking {
            dispatchers.lifecycle {
                // Removed after the work is routed into the loop, but before the loop runs it.
                val job = withContext(Dispatchers.Default) {
                    scope.launch(dispatchers.entity(entity)) { ran = true }.also {
                        scheduler.retiredBeforehand += entity
                    }
                }
                withTimeout(TIMEOUT) { job.join() }
                assertTrue(job.isCancelled)
            }
        }

        assertFalse(ran)
    }

    @RepeatedTest(25)
    fun `running, retiring and disabling race for each block, which completes exactly once`() = runBlocking {
        dispatchers.enable()
        val entity = fakeEntity()
        val completions = AtomicInteger()
        val runs = AtomicInteger()

        val jobs = List(RACE_ROUNDS) {
            scope.launch(
                dispatchers.entity(entity),
                CoroutineStart.DEFAULT,
            ) { runs.incrementAndGet() }
                .also { job -> job.invokeOnCompletion { completions.incrementAndGet() } }
        }
        awaitQueued(RACE_ROUNDS)

        val racers = listOf(
            Thread { scheduler.drain() },
            Thread { scheduler.retire(entity) },
            Thread { dispatchers.beginDisable() },
        )
        racers.forEach(Thread::start)
        racers.forEach(Thread::join)

        withTimeout(TIMEOUT) {
            for (job in jobs) {
                job.join()
            }
        }
        assertTrue(failures.isEmpty(), "a block ran or was rejected twice: ${failures.toList()}")
        assertEquals(RACE_ROUNDS, completions.get())
        assertEquals(RACE_ROUNDS, runs.get() + jobs.count { it.isCancelled })
    }

    private fun awaitQueued(count: Int) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT)

        while (scheduler.queued < count) {
            check(System.nanoTime() < deadline) {
                "only ${scheduler.queued} of $count tasks were scheduled"
            }
            Thread.onSpinWait()
        }
    }

    private fun fakeEntity(): Entity {
        val identity = Any()

        return Proxy.newProxyInstance(
            Entity::class.java.classLoader,
            arrayOf(Entity::class.java),
        ) { proxy, method, args ->
            when (method.name) {
                "hashCode" -> identity.hashCode()
                "equals" -> proxy === args?.get(0)
                "toString" -> "FakeEntity@${identity.hashCode()}"
                else -> null
            }
        } as Entity
    }

    /**
     * Records the thread a callback ran on.
     */
    private class CompletableThread {
        private val latch = CountDownLatch(1)

        @Volatile
        private var thread: Thread? = null

        fun record() {
            thread = Thread.currentThread()
            latch.countDown()
        }

        fun await(): Thread? {
            latch.await(TIMEOUT, TimeUnit.MILLISECONDS)

            return thread
        }
    }

    /**
     * Holds scheduled tasks until a test drains or retires them, like a server that ticks on
     * demand.
     */
    private class FakeScheduler : TaskScheduler {
        private val tasks = ConcurrentLinkedQueue<Scheduled>()

        val retiredBeforehand: MutableSet<Entity> = ConcurrentHashMap.newKeySet()

        @Volatile
        var globalThread: Thread? = null

        @Volatile
        var refuse = false

        @Volatile
        var retiringThread: Thread? = null

        val queued: Int get() = tasks.size

        @Volatile
        override var isRegionized = false

        override fun isGlobalTickThread() = Thread.currentThread() === globalThread

        override fun isOwnedByCurrentRegion(
            world: World,
            chunkX: Int,
            chunkZ: Int,
        ) = isGlobalTickThread()

        override fun isOwnedByCurrentRegion(entity: Entity) = isGlobalTickThread()

        override fun isAlive(entity: Entity) = entity !in retiredBeforehand

        override fun runGlobal(task: Runnable) = schedule(Scheduled(task, null, null))

        override fun runRegion(world: World, chunkX: Int, chunkZ: Int, task: Runnable) = schedule(
            Scheduled(task, null, null),
        )

        override fun runEntity(entity: Entity, task: Runnable, retired: Runnable): Boolean {
            if (entity in retiredBeforehand) return false

            schedule(Scheduled(task, retired, entity))

            return true
        }

        fun drain() {
            while (true) tasks.poll()?.task?.run() ?: return
        }

        fun retire(entity: Entity) {
            retiringThread = Thread.currentThread()
            val entityTasks = tasks.filter { it.entity == entity }
            for (scheduled in entityTasks) {
                if (tasks.remove(scheduled)) scheduled.retired?.run()
            }
        }

        private fun schedule(scheduled: Scheduled) {
            if (refuse) throw IllegalStateException(
                "Plugin attempted to register task while disabled",
            )
            tasks += scheduled
        }

        private class Scheduled(val task: Runnable, val retired: Runnable?, val entity: Entity?)
    }

    private companion object {
        const val TIMEOUT = 5_000L
        const val RACE_ROUNDS = 200
    }
}
