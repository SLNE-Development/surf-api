package dev.slne.surf.api.paper.coroutine

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The coroutine dispatchers that run code on a plugin's behalf on the threads Paper assigns to
 * it.
 *
 * Every plugin context holds one as a bean, and so does surf-api's root context. On Paper the
 * server thread runs [main], every region and every entity; on a server with regionized ticking,
 * such as Folia, each has a thread of its own.
 *
 * A dispatcher never drops a block. When Paper cannot run it, because the plugin is not enabled yet
 * or any more, or because the entity was removed, the dispatcher cancels the coroutine and lets it
 * finish on `Dispatchers.IO`, where the cancellation surfaces at the suspension point, even inside
 * `withContext(NonCancellable)`. Code after that point therefore does not run, but `finally` blocks
 * run on the IO thread and must not touch the entity or the world; cleanup that has to finish once
 * the plugin is disabled stays on `Dispatchers.IO`. [withEntity] never runs its block against a
 * removed entity.
 *
 * Main work that the server thread has to run while a lifecycle hook of any plugin blocks it, runs
 * inside that hook, so a hook can wait for it; on Paper, region and entity work does too. On Folia
 * a hook must not wait for region or entity work. Once a plugin is disabled, Paper runs none of its
 * tasks: an `onDisable` hook does its blocking work on `Dispatchers.IO`.
 */
interface PaperDispatchers {

    /**
     * Runs on the server thread, or on Folia on the global region thread.
     */
    val main: CoroutineDispatcher

    /**
     * Runs off the server threads; the same as `Dispatchers.Default`.
     */
    val async: CoroutineDispatcher

    /**
     * Runs on the thread that owns the chunk at [chunkX] and [chunkZ] of [world].
     */
    fun region(world: World, chunkX: Int, chunkZ: Int): CoroutineDispatcher

    /**
     * Runs on the thread that owns the chunk of [location].
     */
    fun region(location: Location): CoroutineDispatcher

    /**
     * Runs on the thread that owns [entity], and follows it when it moves to another region.
     */
    fun entity(entity: Entity): CoroutineDispatcher
}

/**
 * Carries the [PaperDispatchers] of the plugin a coroutine belongs to.
 *
 * Every coroutine of a plugin's scope and every lifecycle hook has it, so that [withEntity] finds
 * the plugin to schedule for.
 */
@InternalSurfApi
class PaperDispatchersElement(val dispatchers: PaperDispatchers) :
    AbstractCoroutineContextElement(Key) {

    /**
     * The key of [PaperDispatchersElement].
     */
    @InternalSurfApi
    companion object Key : CoroutineContext.Key<PaperDispatchersElement>
}

/**
 * Runs [block] on the thread that owns this entity and returns its result.
 *
 * Runs [block] right away if the calling thread already owns the entity. If the entity is removed
 * before [block] runs, or the plugin is disabled, [block] never runs and the call fails with a
 * `CancellationException`. Unlike a suspending block on [PaperDispatchers.entity], [block] can
 * therefore never run against a removed entity.
 *
 * @throws IllegalStateException if the calling coroutine does not belong to a plugin that runs on
 * surf-api
 */
@OptIn(ExperimentalContracts::class)
suspend fun <T : Entity, R> T.withEntity(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    val dispatchers = currentCoroutineContext()[PaperDispatchersElement]?.dispatchers
        ?: error(
            "withEntity needs a coroutine of a surf plugin, such as one of its scope or its lifecycle hooks",
        )

    check(dispatchers is PluginDispatchers) { "withEntity needs surf-api's own PaperDispatchers" }

    return dispatchers.runOnEntity(this, block)
}

/**
 * The nominal duration of this many server ticks, 50 ms each.
 *
 * It is wall-clock time: a server that lags runs fewer ticks in it.
 */
val Int.ticks: Duration get() = (this * MILLIS_PER_TICK).milliseconds

private const val MILLIS_PER_TICK = 50L
