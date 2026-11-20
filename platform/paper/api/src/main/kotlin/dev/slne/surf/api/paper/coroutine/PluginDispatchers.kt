package dev.slne.surf.api.paper.coroutine

import dev.slne.surf.api.paper.util.chunkX
import dev.slne.surf.api.paper.util.chunkZ
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import java.lang.Runnable
import java.lang.Thread
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

/**
 * The [PaperDispatchers] of one plugin, and the protocol that keeps them safe across the plugin's
 * lifecycle.
 *
 * Paper refuses or silently drops tasks of a plugin that is not enabled, so the dispatchers admit
 * work according to their own [State] instead of Paper's: before [enable] work is parked, while
 * enabled it is handed to Paper, from [beginDisable] on it is rejected, and [close] rejects whatever
 * is left. A rejected block is never dropped: its coroutine is cancelled and the block runs on
 * [Dispatchers.IO], where the continuation completes with the cancellation. Each block runs or is
 * rejected exactly once, whichever of Paper's callbacks, a state change or a retired entity gets to
 * it first.
 *
 * While any plugin's lifecycle phase blocks the server thread in [lifecycle], the main work of every
 * plugin's dispatchers runs inside that phase's event loop instead, so a hook can wait for it, even
 * for work another plugin or surf-api itself dispatched. On a server without regionized ticking that
 * includes region and entity work, which runs on the server thread too. On a regionized server, such
 * as Folia, the thread of a lifecycle phase counts as the main thread, because the global region does
 * not tick yet while plugins load and enable; region and entity work still goes to Paper there, so a
 * hook must not wait for it.
 *
 * Thread-safe. The owning runtime calls [enable], [beginDisable] and [close] in that order.
 */
@InternalSurfApi
class PluginDispatchers internal constructor(
    private val owner: String,
    private val scheduler: TaskScheduler,
) : PaperDispatchers {

    /**
     * Creates the dispatchers of [plugin], scheduling through Paper.
     */
    constructor(plugin: Plugin) : this(plugin.name, BukkitTaskScheduler(plugin))

    @Volatile
    private var state = State.PARKING

    private val tickets: MutableSet<Ticket> = ConcurrentHashMap.newKeySet()

    init {
        open += this
    }

    override val main: CoroutineDispatcher = TargetDispatcher(Target.Main)

    override val async: CoroutineDispatcher get() = Dispatchers.Default

    override fun region(world: World, chunkX: Int, chunkZ: Int): CoroutineDispatcher {
        return TargetDispatcher(Target.Region(world, chunkX, chunkZ))
    }

    override fun region(location: Location): CoroutineDispatcher {
        return region(location.world, location.chunkX, location.chunkZ)
    }

    override fun entity(entity: Entity): CoroutineDispatcher {
        return TargetDispatcher(Target.Entity(entity))
    }

    /**
     * Hands the work parked so far to Paper and lets later work through.
     */
    fun enable() {
        state = State.ENABLED
        tickets.forEach(::admit)
    }

    /**
     * Stops handing work to Paper, which drops the tasks of a disabled plugin, and releases
     * everything Paper still holds: work for the blocked server thread of a running [lifecycle]
     * phase runs there, the rest is rejected.
     */
    fun beginDisable() {
        state = State.DISABLING
        tickets.forEach(::admit)
    }

    /**
     * Rejects all remaining and all future work.
     */
    fun close() {
        state = State.CLOSED
        open -= this
        tickets.forEach(::admit)
    }

    /**
     * Runs [phase], a plugin's or surf-api's lifecycle hooks, without deadlocking on the server thread.
     *
     * Lifecycle hooks run in a `runBlocking` on the server thread, so work dispatched to that thread
     * while a hook waits for it could never run. While [phase] runs, the dispatchers of every plugin
     * therefore hand such work to the event loop of this `runBlocking`, which the waiting thread runs
     * itself; work handed to Paper or parked before moves there as well. Afterwards work takes the
     * regular way through Paper again. Nested phases restore the outer one when they end.
     *
     * On Paper this covers main, region and entity work, which all belong to the server thread. On a
     * regionized server such as Folia, region and entity work still goes to its region's thread, so a
     * hook must not wait for it; the phase's thread counts as the main thread there, because no global
     * region ticks while plugins load.
     */
    suspend fun <T> lifecycle(phase: suspend () -> T): T {
        val interceptor = currentCoroutineContext()[ContinuationInterceptor] as? CoroutineDispatcher
            ?: return phase()

        val onGlobalThread = scheduler.isGlobalTickThread()
        val regionized = scheduler.isRegionized
        val loop = LifecycleLoop(
            dispatcher = interceptor,
            thread = Thread.currentThread(),
            runsMain = onGlobalThread || regionized,
            runsRegionsAndEntities = onGlobalThread && !regionized,
        )

        val outer = activeLoop
        activeLoop = loop

        // Work already parked or handed to Paper would otherwise wait for the thread this phase blocks.
        open.forEach { it.readmit(all = true) }

        try {
            return phase()
        } finally {
            activeLoop = outer
            // Work the loop received but never ran goes the regular way now.
            open.forEach { it.readmit(all = false) }
        }
    }

    private fun readmit(all: Boolean) {
        tickets.forEach { if (all || it.route == Route.LOOP) admit(it) }
    }

    /**
     * Runs [block] on the thread that owns [entity], suspending until it has.
     *
     * If the entity is removed before [block] runs, or the plugin is disabled, [block] never runs
     * and the call fails with a [CancellationException].
     */
    @OptIn(ExperimentalContracts::class)
    internal suspend fun <T : Entity, R> runOnEntity(entity: T, block: (T) -> R): R {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }

        val target = Target.Entity(entity)
        if (!target.isDispatchNeeded()) return block(entity)

        return suspendCancellableCoroutine { continuation ->
            val ticket = Ticket(
                target = target,
                run = { continuation.resumeWith(runCatching { block(entity) }) },
                reject = { cause -> continuation.cancel(cause) },
            )

            continuation.invokeOnCancellation { ticket.discard() }
            submit(ticket)
        }
    }

    private fun submit(ticket: Ticket) {
        tickets += ticket
        if (ticket.isDone) {
            tickets -= ticket
            return
        }
        admit(ticket)
    }

    /**
     * Routes [ticket] according to the current state; safe to call again for the same ticket.
     */
    private fun admit(ticket: Ticket) {
        if (ticket.isDone) return

        val loop = loopFor(ticket.target)
        when {
            loop != null -> ticket.runIn(loop)
            state == State.PARKING -> Unit
            state == State.ENABLED -> handToPaper(ticket)
            else -> ticket.reject(CancellationException("Plugin '$owner' is disabled"))
        }
    }

    private fun loopFor(target: Target): LifecycleLoop? {
        val loop = activeLoop ?: return null

        return when (target) {
            Target.Main -> loop.takeIf { it.runsMain }
            is Target.Region -> loop.takeIf { it.runsRegionsAndEntities }
            is Target.Entity -> loop.takeIf { it.runsRegionsAndEntities && scheduler.isAlive(target.entity) }
        }
    }

    /**
     * Whether the calling thread runs a lifecycle phase that main work is routed into.
     */
    private fun isLoopThreadForMain(): Boolean {
        val loop = activeLoop ?: return false
        return loop.runsMain && loop.thread === Thread.currentThread()
    }

    private fun handToPaper(ticket: Ticket) {
        if (!ticket.markHandedToPaper()) return

        try {
            when (val target = ticket.target) {
                Target.Main -> scheduler.runGlobal(ticket::run)
                is Target.Region -> scheduler.runRegion(
                    target.world,
                    target.chunkX,
                    target.chunkZ,
                    ticket::run
                )

                is Target.Entity -> {
                    val scheduled = scheduler.runEntity(target.entity, ticket::run, ticket::retire)
                    if (!scheduled) ticket.retire()
                }
            }
        } catch (exception: RuntimeException) {
            ticket.reject(CancellationException("Plugin '$owner' could not schedule a task").apply {
                initCause(exception)
            })
        }
    }

    private fun Target.isDispatchNeeded(): Boolean = when (this) {
        Target.Main -> !(scheduler.isGlobalTickThread() || isLoopThreadForMain())
        is Target.Region -> !scheduler.isOwnedByCurrentRegion(world, chunkX, chunkZ)
        is Target.Entity -> !(scheduler.isOwnedByCurrentRegion(entity) && scheduler.isAlive(entity))
    }

    /**
     * A dispatcher that sends each block to [target] through this plugin's protocol.
     */
    private inner class TargetDispatcher(private val target: Target) : CoroutineDispatcher() {

        override fun isDispatchNeeded(context: CoroutineContext): Boolean {
            return target.isDispatchNeeded()
        }

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            submit(
                Ticket(
                    target = target,
                    run = block,
                    reject = { cause ->
                        context.cancel(cause)
                        block.run()
                    },
                )
            )
        }

        override fun toString(): String = "PaperDispatchers($owner).$target"
    }

    /**
     * One dispatched block on its way to exactly one of: running, or being rejected.
     *
     * [reject] runs on [Dispatchers.IO], never on the thread that rejects, because Paper calls the
     * retired callback in code that must not run user code.
     */
    private inner class Ticket(
        val target: Target,
        run: Runnable,
        reject: (CancellationException) -> Unit,
    ) {
        private val done = AtomicBoolean()
        private val handedToPaper = AtomicBoolean()

        @Volatile
        private var runAction: Runnable? = run

        @Volatile
        private var rejectAction: ((CancellationException) -> Unit)? = reject

        @Volatile
        var route: Route = Route.NONE
            private set

        val isDone: Boolean get() = done.get()

        fun markHandedToPaper(): Boolean {
            route = Route.PAPER
            return handedToPaper.compareAndSet(false, true)
        }

        /**
         * Hands the ticket to [loop]. A loop that has already ended may run the task on another
         * thread; the ticket then goes the regular way instead. An entity removed in the meantime
         * retires the ticket, as Paper's entity scheduler would.
         */
        fun runIn(loop: LifecycleLoop) {
            route = Route.LOOP
            loop.dispatcher.dispatch(loop.dispatcher) {
                val entity = (target as? Target.Entity)?.entity
                when {
                    Thread.currentThread() !== loop.thread -> admit(this)
                    entity != null && !scheduler.isAlive(entity) -> retire()
                    else -> run()
                }
            }
        }

        fun run() {
            val action = claim()?.first ?: return
            action.run()
        }

        fun retire() = reject(CancellationException("The entity of plugin '$owner' was removed"))

        fun reject(cause: CancellationException) {
            val action = claim()?.second ?: return
            Dispatchers.IO.dispatch(Dispatchers.IO) { action(cause) }
        }

        /**
         * Gives up the ticket without running or rejecting it, for a caller that is cancelled.
         */
        fun discard() {
            claim()
        }

        private fun claim(): Pair<Runnable, (CancellationException) -> Unit>? {
            if (!done.compareAndSet(false, true)) return null
            tickets -= this

            val claimed = checkNotNull(runAction) to checkNotNull(rejectAction)
            runAction = null
            rejectAction = null
            return claimed
        }
    }

    /**
     * The event loop of a lifecycle phase that blocks [thread], and which work it runs.
     */
    private class LifecycleLoop(
        val dispatcher: CoroutineDispatcher,
        val thread: Thread,
        val runsMain: Boolean,
        val runsRegionsAndEntities: Boolean,
    )

    private sealed interface Target {
        data object Main : Target
        data class Region(val world: World, val chunkX: Int, val chunkZ: Int) : Target
        data class Entity(val entity: org.bukkit.entity.Entity) : Target
    }

    private enum class Route {
        NONE,
        LOOP,
        PAPER
    }

    private enum class State {
        PARKING,
        ENABLED,
        DISABLING,
        CLOSED
    }

    private companion object {

        /**
         * The lifecycle phase that currently blocks a server thread, shared by every plugin's
         * dispatchers because they share that thread.
         */
        @Volatile
        private var activeLoop: LifecycleLoop? = null

        /**
         * Every instance that is not closed, so a phase can route their work.
         */
        private val open: MutableSet<PluginDispatchers> = ConcurrentHashMap.newKeySet()
    }
}
