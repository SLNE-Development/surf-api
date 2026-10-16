package dev.slne.surf.api.minestom.server.impl.command

import com.mojang.brigadier.CommandDispatcher
import dev.slne.surf.api.minestom.command.CommandDefinition
import dev.slne.surf.api.minestom.command.RegisteredCommand
import dev.slne.surf.api.minestom.command.internal.CommandAPIPlatform
import dev.slne.surf.api.minestom.coroutine.minestomAsyncScope
import dev.slne.surf.api.minestom.server.impl.command.brigadier.BrigadierCommandTree
import dev.slne.surf.api.minestom.server.impl.command.brigadier.DeclareCommandsMerger
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import kotlinx.coroutines.CoroutineScope
import net.minestom.server.command.CommandManager
import net.minestom.server.command.CommandSender
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Registers CommandAPI commands in the Brigadier dispatcher, which owns their parsing and dispatch.
 *
 * Commands are not registered with Minestom's [CommandManager]; the manager reaches them through
 * [CommandAPIHook] from `CommandManagerMixin`. The platform queries the manager only at
 * registration, so that a label already used by a Minestom command is rejected instead of
 * shadowed.
 *
 * Registration state is guarded by a lock, and a failed registration is rolled back completely.
 * Constructing a platform makes it the active one that the accessors in the companion expose.
 */
internal class MinestomCommandAPIPlatform(
    private val commandManager: CommandManager,
    private val ownership: MinestomCommandOwnership,
    suggestionScope: () -> CoroutineScope = { minestomAsyncScope },
    private val compiler: MinestomCommandCompiler = MinestomCommandCompiler(),
    private val tree: BrigadierCommandTree = BrigadierCommandTree(suggestionScope = suggestionScope),
) : CommandAPIPlatform, AutoCloseable {

    private val lock = ReentrantLock()
    private val registrations = ObjectOpenHashSet<CompiledRegistration>()
    private val merger = DeclareCommandsMerger(tree)

    private var closed = false

    init {
        installed.set(this)
    }

    /**
     * Registers [definition] under its name, its aliases and, when [namespace] is set, their
     * `namespace:label` forms.
     *
     * Definitions that cannot be declared to a client are rejected by [MinestomCommandCompiler].
     *
     * @throws IllegalStateException if the platform is closed or a label is already taken
     */
    override fun register(
        definition: CommandDefinition,
        namespace: String?
    ): RegisteredCommand = lock.withLock {
        check(!closed) { "The Minestom CommandAPI platform is closed" }

        val compiled = compiler.compile(definition, namespace)
        compiled.names.forEach { name ->
            check(!ownership.contains(name) && commandManager.getCommand(name) == null) {
                "Command name '$name' is already registered"
            }
        }

        var treeRegistered = false
        try {
            tree.register(definition.name, compiled.names, definition)
            treeRegistered = true

            ownership.claim(compiled.names, compiled)
            registrations += compiled

            PluginCommands.track(compiled.registration.name)
        } catch (failure: Throwable) {
            if (treeRegistered) {
                try {
                    detach(compiled)
                } catch (rollbackFailure: Throwable) {
                    failure.addSuppressed(rollbackFailure)
                }
            } else {
                ownership.release(compiled)
            }
            throw failure
        }
        return compiled.registration
    }

    /**
     * Executes [input] with the current dispatcher; Brigadier failures propagate to the caller.
     */
    override fun execute(sender: CommandSender, input: String): Int =
        tree.dispatcher.execute(input, sender)

    /**
     * Unregisters the command that owns the label [name].
     *
     * Returns `false` when no command of this platform owns it.
     */
    override fun unregister(name: String): Boolean = lock.withLock {
        val compiled = ownership.find(name) ?: return false
        if (compiled !in registrations) return false
        detach(compiled)
        return true
    }

    /**
     * Unregisters every command and rejects further registrations.
     *
     * Every command is detached even when some fail; the first failure is rethrown with the later
     * ones attached as suppressed exceptions.
     */
    override fun close(): Unit = lock.withLock {
        if (closed) return
        closed = true
        installed.compareAndSet(this, null)

        var failure: Throwable? = null
        val registrationsSnapshot = ObjectArrayList(registrations)
        registrationsSnapshot.forEach { compiled ->
            try {
                detach(compiled)
            } catch (currentFailure: Throwable) {
                if (failure == null) {
                    failure = currentFailure
                } else {
                    failure.addSuppressed(currentFailure)
                }
            }
        }

        registrations.clear()
        failure?.let { throw it }
    }

    private fun detach(compiled: CompiledRegistration) {
        check(lock.isHeldByCurrentThread) { "Platform state lock must be held while detaching a command" }

        try {
            PluginCommands.forget(compiled.registration.name)
            tree.unregister(compiled.registration.name)
        } finally {
            registrations.remove(compiled)
            ownership.release(compiled)
        }
    }

    internal companion object {
        private val installed = AtomicReference<MinestomCommandAPIPlatform?>()

        /**
         * The merger of the active platform, or `null` while none is active.
         *
         * The active platform is the most recently constructed one, until it is closed. The
         * accessors serve [CommandAPIHook] and the suggestion listener, which cannot be handed the
         * instance.
         */
        fun activeMerger(): DeclareCommandsMerger? = installed.get()?.merger

        /**
         * The dispatcher of the active platform, or `null` while none is active.
         */
        fun activeDispatcher(): CommandDispatcher<CommandSender>? = installed.get()?.tree?.dispatcher

        /**
         * The name registry of the active platform, or `null` while none is active.
         */
        fun activeOwnership(): MinestomCommandOwnership? = installed.get()?.ownership
    }
}
