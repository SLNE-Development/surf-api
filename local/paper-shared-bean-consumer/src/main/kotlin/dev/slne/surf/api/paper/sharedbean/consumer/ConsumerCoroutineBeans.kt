package dev.slne.surf.api.paper.sharedbean.consumer

import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.paper.command.executors.anyExecutorSuspend
import dev.slne.surf.api.paper.coroutine.PaperDispatchers
import dev.slne.surf.api.spring.SurfLifecycle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.bukkit.Bukkit
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Probes the consumer's coroutine scope across the plugin lifecycle.
 *
 * Both probes block the server thread inside a lifecycle hook and wait for work that the hook's
 * own event loop has to run on that thread.
 *
 * - `scopeSurvivesLoad`: a coroutine launched in `onLoad` outlives the load phase, and its main
 *   thread block still runs. The result reaches the report line through [scopeSurvivesLoad].
 * - `disableJoin`: `onDisable` cancels and joins a coroutine that is suspended on main thread
 *   work, and logs the outcome after the consumer's own disable line.
 */
@Service
class ConsumerCoroutineProbe(
    private val scope: CoroutineScope,
    private val dispatchers: PaperDispatchers,
) : SurfLifecycle {

    private val log = logger()

    @Volatile
    private var delayElapsed = false

    private val reachedMain = CompletableDeferred<Unit>()

    /**
     * `true` once the block launched in `onLoad` ran on the main thread; otherwise names the stage
     * the coroutine got stuck at. Set by [onEnable].
     */
    @Volatile
    var scopeSurvivesLoad = "unchecked"

    override suspend fun onLoad() {
        scope.launch {
            delay(LOAD_DELAY)
            delayElapsed = true
            withContext(dispatchers.main) { reachedMain.complete(Unit) }
        }
    }

    override suspend fun onEnable() {
        val reached = withTimeoutOrNull(ENABLE_AWAIT) { reachedMain.await() } != null

        scopeSurvivesLoad = when {
            reached -> "true"
            delayElapsed -> "stuckBeforeMain"
            else -> "neverResumed"
        }
    }

    override suspend fun onDisable() {
        val onMain = CompletableDeferred<Unit>()
        val job = scope.launch(dispatchers.async) {
            withContext(dispatchers.main) {
                onMain.complete(Unit)
                awaitCancellation()
            }
        }

        val joined = withTimeoutOrNull(DISABLE_AWAIT) {
            onMain.await()
            job.cancelAndJoin()
        } != null

        log.atInfo()
            .log(
                "%s disableJoin=%s",
                SharedBeanReport.SPIKE_MARKER,
                if (joined) "returned" else "timeout",
            )
    }

    private companion object {
        val LOAD_DELAY = 100.milliseconds
        val ENABLE_AWAIT = 5.seconds
        val DISABLE_AWAIT = 5.seconds
    }
}

/**
 * A command bean with a suspending executor on its default scope. The boot test runs it from the
 * console and reads where its executor ran.
 */
@Service
class SpikeScopeCommand(private val pluginScope: CoroutineScope) :
    CommandAPICommand(SharedBeanReport.SCOPE_COMMAND) {

    private val log = logger()

    init {
        anyExecutorSuspend { _, _ -> logExecutorOrigin() }
    }

    /**
     * Logs whether the executor's job descends from [pluginScope]'s job and whether the executor
     * runs on the server thread.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun CoroutineScope.logExecutorOrigin() {
        val consumerJob = pluginScope.coroutineContext.job
        val belongsToConsumer = generateSequence(coroutineContext.job) { it.parent }.any {
            it === consumerJob
        }

        log.atInfo()
            .log(
                "%s commandScope=%s commandThread=%s",
                SharedBeanReport.SPIKE_MARKER,
                if (belongsToConsumer) "consumer" else "foreign",
                if (Bukkit.isPrimaryThread()) "main" else "other",
            )
    }
}
