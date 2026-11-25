package dev.slne.surf.api.spring.scheduling

import dev.slne.surf.api.spring.SurfSchedulers
import dev.slne.surf.api.spring.context.*
import dev.slne.surf.schedulingfixture.ScheduledApplicationBean
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.springframework.beans.factory.getBean
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.MapPropertySource
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.config.TaskManagementConfigUtils
import java.net.URL
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SchedulingTest {

    @Test
    fun `a sync method without scheduler runs on the scope's dispatcher`() = runTest {
        withScheduling(SyncFixedDelay::class.java) { context, recorder ->
            context.startTasks()
            advanceTimeBy(250)
            runCurrent()

            assertEquals(listOf(0L, 100L, 200L), recorder.starts)
        }
    }

    @Test
    fun `no task runs before the context starts its tasks`() = runTest {
        withScheduling(SyncFixedDelay::class.java, SuspendFixedDelay::class.java) { _, recorder ->
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(emptyList(), recorder.starts)
        }
    }

    @Test
    fun `afterEnable starts the tasks and beforeDisable stops them`() = runTest {
        withScheduling(SuspendFixedDelay::class.java) { context, recorder ->
            context.runAfterEnable()
            advanceTimeBy(150)
            runCurrent()
            context.runBeforeDisable("test")
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(listOf(0L, 100L), recorder.starts)
        }
    }

    @Test
    fun `closing the context cancels every task`() = runTest {
        withScheduling(
            SyncFixedDelay::class.java,
            SuspendFixedDelay::class.java,
        ) { context, recorder ->
            context.startTasks()
            runCurrent()
            context.close()
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(2, recorder.starts.size)
        }
    }

    @Test
    fun `a suspending fixed delay waits for the run to complete`() = runTest {
        withScheduling(SlowFixedDelay::class.java) { context, recorder ->
            context.startTasks()
            advanceTimeBy(900)
            runCurrent()

            assertEquals(listOf(0L, 400L, 800L), recorder.starts)
        }
    }

    @Test
    fun `a suspending fixed rate skips missed starts and never overlaps`() = runTest {
        withScheduling(SlowFixedRate::class.java) { context, recorder ->
            context.startTasks()
            advanceTimeBy(700)
            runCurrent()

            assertEquals(listOf(0L, 300L, 600L), recorder.starts)
            assertEquals(1, recorder.maxConcurrent)
        }
    }

    @Test
    fun `a sync fixed rate skips missed starts and never overlaps`() = runTest {
        withScheduling(SlowSyncFixedRate::class.java) { context, recorder ->
            context.startTasks()
            advanceTimeBy(700)
            runCurrent()

            assertEquals(listOf(0L, 300L, 600L), recorder.starts.take(3))
            assertEquals(1, recorder.maxConcurrent)
        }
    }

    @Test
    fun `a suspending fixed rate keeps its rate when runs are short`() = runTest {
        withScheduling(FastFixedRate::class.java) { context, recorder ->
            context.startTasks()
            advanceTimeBy(350)
            runCurrent()

            assertEquals(listOf(0L, 100L, 200L, 300L), recorder.starts)
        }
    }

    @Test
    fun `a suspending cron method runs at the expression's times`() = runTest {
        withScheduling(EveryTenSeconds::class.java) { context, recorder ->
            context.startTasks()
            advanceTimeBy(35_000)
            runCurrent()

            assertEquals(listOf(10_000L, 20_000L, 30_000L), recorder.starts)
        }
    }

    @Test
    fun `an initial delay postpones the first run`() = runTest {
        withScheduling(DelayedStart::class.java) { context, recorder ->
            context.startTasks()
            advanceTimeBy(200)
            runCurrent()

            assertEquals(listOf(50L, 150L), recorder.starts)
        }
    }

    @Test
    fun `a failing run is logged and the task keeps its schedule`() = runTest {
        withScheduling(FailingFirstRun::class.java) { context, recorder ->
            context.startTasks()
            advanceTimeBy(250)
            runCurrent()

            assertEquals(listOf(0L, 100L, 200L), recorder.starts)
        }
    }

    @Test
    fun `placeholders in string attributes are resolved`() = runTest {
        val properties = MapPropertySource("test", mapOf("test.delay" to "100ms"))

        withScheduling(
            PlaceholderDelay::class.java,
            configure = { context -> context.environment.propertySources.addFirst(properties) },
        ) { context, recorder ->
            context.startTasks()
            advanceTimeBy(150)
            runCurrent()

            assertEquals(listOf(0L, 100L), recorder.starts)
        }
    }

    @Test
    fun `a placeholder that resolves to null leaves its attribute unset`() = runTest {
        withScheduling(NullCronWithFixedDelay::class.java) { context, recorder ->
            context.startTasks()
            advanceTimeBy(150)
            runCurrent()

            assertEquals(listOf(0L, 100L), recorder.starts)
        }
    }

    @Test
    fun `a suspending method with parameters is rejected`() = runTest {
        val failure = runCatching {
            withScheduling(ParameterizedSuspend::class.java) { _, _ -> }
        }.exceptionOrNull()

        assertNotNull(failure)
        assertContains(failure.stackTraceToString(), "must not take parameters")
    }

    @Test
    fun `a non-positive interval is rejected`() = runTest {
        val failure = runCatching {
            withScheduling(ZeroRate::class.java) { _, _ -> }
        }.exceptionOrNull()

        assertNotNull(failure)
        assertContains(failure.stackTraceToString(), "must be positive")
    }

    @Test
    fun `a sync method with a non-positive interval fails the refresh`() = runTest {
        val failure = runCatching {
            withScheduling(SyncZeroRate::class.java) { _, _ -> }
        }.exceptionOrNull()

        assertNotNull(failure)
        assertContains(failure.stackTraceToString(), "must be positive")
    }

    @Test
    fun `a sync method naming an unknown scheduler fails the refresh`() = runTest {
        val failure = runCatching {
            withScheduling(SyncOnMissingScheduler::class.java) { _, _ -> }
        }.exceptionOrNull()

        assertNotNull(failure)
        assertContains(failure.stackTraceToString(), "missing")
    }

    @Test
    fun `a hosting root runs scheduled methods once enabled`() = runBlocking {
        val loader = SurfApiHidingClassLoader(javaClass.classLoader)
        val application = PluginDescriptor(
            name = "hosted",
            classLoader = loader,
            source = null,
            basePackages = listOf("dev.slne.surf.schedulingfixture"),
            dataDirectory = null,
        )
        val root = SurfRootContext.create(
            classLoader = loader,
            coroutineScope = CoroutineScope(SupervisorJob()),
            application = application,
        )

        try {
            root.load()
            val bean = root.context.getBean<ScheduledApplicationBean>()
            assertEquals(2L, bean.runs.count)
            root.enable()

            assertTrue(bean.runs.await(5, TimeUnit.SECONDS))
            root.disable()
        } finally {
            root.close()
        }
    }

    @Test
    fun `a suspending method runs on the named scheduler inside the plugin scope`() {
        val scope = CoroutineScope(SupervisorJob() + CoroutineName("surf-io-test"))
        val recorder = Recorder(Clock.systemUTC())
        val context = AnnotationConfigApplicationContext()

        try {
            registerScheduling(context, "io-test", scope)
            context.beanFactory.registerSingleton("recorder", recorder)
            context.register(SuspendOnIo::class.java)
            context.refresh()
            context.startTasks()

            assertTrue(recorder.latch.await(5, TimeUnit.SECONDS))
            assertSame(Dispatchers.IO, recorder.dispatchers.first())
            assertEquals("surf-io-test", recorder.names.first())
        } finally {
            context.close()
            scope.cancel()
        }
    }

    @Test
    fun `a sync method runs on the scheduler its attribute names`() {
        val executor = Executors.newSingleThreadExecutor { task -> Thread(task, "custom-thread") }
        val scope = CoroutineScope(SupervisorJob())
        val recorder = Recorder(Clock.systemUTC())
        val context = AnnotationConfigApplicationContext()

        try {
            registerScheduling(context, "custom-test", scope)
            registerTaskScheduler(context, "custom", executor.asCoroutineDispatcher())
            context.beanFactory.registerSingleton("recorder", recorder)
            context.register(SyncOnCustom::class.java)
            context.refresh()
            context.startTasks()

            assertTrue(recorder.latch.await(5, TimeUnit.SECONDS))
            assertTrue(recorder.threads.first().startsWith("custom-thread"))
        } finally {
            context.close()
            scope.cancel()
            executor.shutdownNow()
        }
    }

    @Test
    fun `the surf schedulers use their dispatchers and the default one is injectable`() {
        val scope = CoroutineScope(SupervisorJob())
        val context = AnnotationConfigApplicationContext()

        try {
            registerScheduling(context, "beans", scope)
            context.refresh()

            val default = context.getBean<CoroutineTaskScheduler>(SurfSchedulers.DEFAULT)
            val io = context.getBean<CoroutineTaskScheduler>(SurfSchedulers.IO)

            assertSame(Dispatchers.Default, default.dispatcher)
            assertSame(Dispatchers.IO, io.dispatcher)
            assertSame(context.getBean(PLUGIN_SCHEDULER_BEAN), context.getBean<TaskScheduler>())
        } finally {
            context.close()
            scope.cancel()
        }
    }

    @Test
    fun `an EnableScheduling configuration keeps surf-api's processor`() {
        for (allowOverriding in listOf(true, false)) {
            val scope = CoroutineScope(SupervisorJob())
            val context = AnnotationConfigApplicationContext()

            try {
                context.setAllowBeanDefinitionOverriding(allowOverriding)
                registerScheduling(context, "enabled", scope)
                context.register(SchedulingEnabled::class.java)
                context.refresh()

                val processor = context.getBean(
                    TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME,
                )
                assertIs<SurfScheduledAnnotationProcessor>(processor, "overriding $allowOverriding")
            } finally {
                context.close()
                scope.cancel()
            }
        }
    }

    @Test
    fun `a plugin context prepared by surf-api has scheduling`() {
        val scope = CoroutineScope(SupervisorJob())
        val root = AnnotationConfigApplicationContext().apply { refresh() }
        val context = AnnotationConfigApplicationContext()
        val plugin = PluginDescriptor(
            name = "prepared",
            classLoader = javaClass.classLoader,
            source = null,
            basePackages = listOf("dev.slne.surf.api.spring.scheduling.unscanned"),
            dataDirectory = null,
        )

        try {
            preparePluginContext(context, root, plugin, scope)
            context.refresh()

            val processor = context.getBean(
                TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME,
            )
            assertIs<SurfScheduledAnnotationProcessor>(processor)
        } finally {
            context.close()
            root.close()
            scope.cancel()
        }
    }

    private suspend fun TestScope.withScheduling(
        vararg beans: Class<*>,
        configure: (AnnotationConfigApplicationContext) -> Unit = {},
        test: suspend (AnnotationConfigApplicationContext, Recorder) -> Unit,
    ) {
        val clock = VirtualClock(this)
        val recorder = Recorder(clock)
        val scope = CoroutineScope(
            SupervisorJob() + StandardTestDispatcher(testScheduler) + CoroutineName("surf-test"),
        )
        val context = AnnotationConfigApplicationContext()

        try {
            registerScheduling(context, "test", scope, clock)
            context.beanFactory.registerSingleton("recorder", recorder)
            context.beanFactory.registerSingleton("clock", clock)
            context.register(*beans)
            configure(context)
            context.refresh()
            test(context, recorder)
        } finally {
            context.close()
            scope.cancel()
        }
    }

    private fun AnnotationConfigApplicationContext.startTasks() {
        startScheduledTasks(this)
    }
}

/**
 * Hides surf-api's packages from a root context's component scan, so the scan finds none of the
 * test fixtures under them.
 */
private class SurfApiHidingClassLoader(parent: ClassLoader) : ClassLoader(parent) {

    override fun getResources(name: String): Enumeration<URL> {
        if (name.startsWith("dev/slne/surf/api")) return Collections.emptyEnumeration()

        return super.getResources(name)
    }
}

/**
 * A clock that reads the virtual time of a test, starting at a full minute.
 *
 * [elapse] moves it ahead of the virtual time, for sync runs that cannot suspend to pass time.
 */
internal class VirtualClock(private val test: TestScope) : Clock() {

    @Volatile
    private var aheadMillis = 0L

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun instant(): Instant {
        return EPOCH.plusMillis(test.testScheduler.currentTime + aheadMillis)
    }

    fun elapse(millis: Long) {
        aheadMillis += millis
    }

    companion object {
        val EPOCH: Instant = Instant.parse("2026-01-01T00:00:00Z")
    }
}

class Recorder(private val clock: Clock) {
    val starts = CopyOnWriteArrayList<Long>()
    val threads = CopyOnWriteArrayList<String>()
    val dispatchers = CopyOnWriteArrayList<ContinuationInterceptor?>()
    val names = CopyOnWriteArrayList<String?>()
    val latch = CountDownLatch(1)
    private val running = AtomicInteger()

    @Volatile
    var maxConcurrent = 0
        private set

    fun start(): Int {
        starts += clock.millis() - VirtualClock.EPOCH.toEpochMilli()
        threads += Thread.currentThread().name
        val concurrent = running.incrementAndGet()
        maxConcurrent = maxOf(maxConcurrent, concurrent)
        latch.countDown()

        return starts.size
    }

    fun end() {
        running.decrementAndGet()
    }

    suspend fun recordContext() {
        val context = currentCoroutineContext()
        dispatchers += context[ContinuationInterceptor]
        names += context[CoroutineName]?.name
        start()
    }
}

class SyncFixedDelay(private val recorder: Recorder) {

    @Scheduled(fixedDelay = 100)
    fun run() {
        recorder.start()
        recorder.end()
    }
}

class SuspendFixedDelay(private val recorder: Recorder) {

    @Scheduled(fixedDelay = 100)
    suspend fun run() {
        recorder.start()
        recorder.end()
    }
}

class SlowFixedDelay(private val recorder: Recorder) {

    @Scheduled(fixedDelay = 100)
    suspend fun run() {
        recorder.start()
        delay(300)
        recorder.end()
    }
}

class SlowFixedRate(private val recorder: Recorder) {

    @Scheduled(fixedRate = 100)
    suspend fun run() {
        recorder.start()
        delay(250)
        recorder.end()
    }
}

internal class SlowSyncFixedRate(
    private val recorder: Recorder,
    private val clock: VirtualClock,
) {

    @Scheduled(fixedRate = 100)
    fun run() {
        recorder.start()
        clock.elapse(250)
        recorder.end()
    }
}

class FastFixedRate(private val recorder: Recorder) {

    @Scheduled(fixedRate = 100)
    suspend fun run() {
        recorder.start()
        delay(10)
        recorder.end()
    }
}

class EveryTenSeconds(private val recorder: Recorder) {

    @Scheduled(cron = "*/10 * * * * *", zone = "UTC")
    suspend fun run() {
        recorder.start()
        recorder.end()
    }
}

class DelayedStart(private val recorder: Recorder) {

    @Scheduled(fixedDelay = 100, initialDelay = 50)
    suspend fun run() {
        recorder.start()
        recorder.end()
    }
}

class FailingFirstRun(private val recorder: Recorder) {

    @Scheduled(fixedDelay = 100)
    suspend fun run() {
        val run = recorder.start()
        recorder.end()
        check(run != 1) { "The first run fails" }
    }
}

class PlaceholderDelay(private val recorder: Recorder) {

    @Scheduled(fixedDelayString = "\${test.delay}")
    suspend fun run() {
        recorder.start()
        recorder.end()
    }
}

class NullCronWithFixedDelay(private val recorder: Recorder) {

    @Scheduled(cron = "\${test.cron:#{null}}", fixedDelay = 100)
    suspend fun run() {
        recorder.start()
        recorder.end()
    }
}

class ParameterizedSuspend {

    @Scheduled(fixedDelay = 100)
    suspend fun run(@Suppress("unused") times: Int) {
    }
}

class ZeroRate {

    @Scheduled(fixedRate = 0)
    suspend fun run() {
    }
}

class SyncZeroRate {

    @Scheduled(fixedRate = 0)
    fun run() {
    }
}

class SyncOnMissingScheduler {

    @Scheduled(fixedDelay = 100, scheduler = "missing")
    fun run() {
    }
}

class SuspendOnIo(private val recorder: Recorder) {

    @Scheduled(fixedDelay = 1_000, scheduler = SurfSchedulers.IO)
    suspend fun run() {
        recorder.recordContext()
    }
}

class SyncOnCustom(private val recorder: Recorder) {

    @Scheduled(fixedDelay = 1_000, scheduler = "custom")
    fun run() {
        recorder.start()
    }
}

@Configuration(proxyBeanMethods = false)
@EnableScheduling
class SchedulingEnabled
