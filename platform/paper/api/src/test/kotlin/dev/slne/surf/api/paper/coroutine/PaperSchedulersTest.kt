package dev.slne.surf.api.paper.coroutine

import dev.slne.surf.api.spring.context.PluginDescriptor
import dev.slne.surf.api.spring.context.preparePluginContext
import dev.slne.surf.api.spring.scheduling.registerTaskScheduler
import dev.slne.surf.api.spring.scheduling.startScheduledTasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.bukkit.World
import org.bukkit.entity.Entity
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class PaperSchedulersTest {

    @Test
    fun `a sync method on the global region scheduler runs on the server thread`() {
        val scheduler = GlobalThreadScheduler()
        val dispatchers = PluginDispatchers("paper-test", scheduler)
        val scope = CoroutineScope(SupervisorJob())
        val recorder = GlobalRegionRecorder(scheduler)
        val root = AnnotationConfigApplicationContext().apply { refresh() }
        val context = AnnotationConfigApplicationContext()
        val plugin = PluginDescriptor(
            name = "paper-test",
            classLoader = javaClass.classLoader,
            source = null,
            basePackages = listOf("dev.slne.surf.api.paper.coroutine.unscanned"),
            dataDirectory = null,
        )

        try {
            preparePluginContext(context, root, plugin, scope)
            registerTaskScheduler(context, PaperSchedulers.GLOBAL_REGION, dispatchers.main)
            context.beanFactory.registerSingleton("recorder", recorder)
            context.register(GlobalRegionTask::class.java)
            context.refresh()
            dispatchers.enable()
            startScheduledTasks(context)

            assertTrue(recorder.ran.await(TIMEOUT, TimeUnit.MILLISECONDS))
            assertTrue(recorder.ranOnServerThread)
            assertNotSame(
                context.getBean(PaperSchedulers.GLOBAL_REGION),
                context.getBean<TaskScheduler>(),
            )
        } finally {
            context.close()
            root.close()
            scope.cancel()
            dispatchers.close()
            scheduler.close()
        }
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}

class GlobalRegionRecorder internal constructor(private val scheduler: PaperTaskScheduler) {
    val ran = CountDownLatch(1)

    @Volatile
    var ranOnServerThread = false
        private set

    fun record() {
        ranOnServerThread = scheduler.isGlobalTickThread()
        ran.countDown()
    }
}

class GlobalRegionTask(private val recorder: GlobalRegionRecorder) {

    @Scheduled(fixedDelay = 1_000, scheduler = PaperSchedulers.GLOBAL_REGION)
    fun run() {
        recorder.record()
    }
}

/**
 * Runs global tasks on one thread that stands in for the server thread.
 */
private class GlobalThreadScheduler : PaperTaskScheduler {

    @Volatile
    private var serverThread: Thread? = null

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { task ->
        Thread(task, "server-thread").also { thread -> serverThread = thread }
    }

    override val isRegionized = false

    override fun isGlobalTickThread() = Thread.currentThread() === serverThread

    override fun isOwnedByCurrentRegion(world: World, chunkX: Int, chunkZ: Int): Boolean {
        return isGlobalTickThread()
    }

    override fun isOwnedByCurrentRegion(entity: Entity) = isGlobalTickThread()

    override fun isAlive(entity: Entity) = true

    override fun runGlobal(task: Runnable) {
        executor.execute(task)
    }

    override fun runRegion(world: World, chunkX: Int, chunkZ: Int, task: Runnable) {
        executor.execute(task)
    }

    override fun runEntity(entity: Entity, task: Runnable, retired: Runnable): Boolean {
        executor.execute(task)

        return true
    }

    fun close() {
        executor.shutdownNow()
    }
}
