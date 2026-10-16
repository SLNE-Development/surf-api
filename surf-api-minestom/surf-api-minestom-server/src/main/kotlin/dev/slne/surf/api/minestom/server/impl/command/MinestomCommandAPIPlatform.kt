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
 * Installs CommandAPI commands into the Brigadier dispatcher, which owns their parsing and dispatch.
 *
 * Commands registered here are unknown to Minestom's own command manager, which reaches them
 * through [CommandAPIHook] from `CommandManagerMixin`. The manager is still consulted so a name
 * already taken by a foreign Minestom command is rejected rather than shadowed.
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

    override fun execute(sender: CommandSender, input: String): Int =
        tree.dispatcher.execute(input, sender)

    override fun unregister(name: String): Boolean = lock.withLock {
        val compiled = ownership.find(name) ?: return false
        if (compiled !in registrations) return false
        detach(compiled)
        return true
    }

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
         * The merger of the currently installed platform, or `null` while none is installed.
         *
         * The platform is created by the CommandAPI rather than by Minestom, so the mixin that
         * reaches it through [CommandAPIHook] cannot be given it.
         */
        fun activeMerger(): DeclareCommandsMerger? = installed.get()?.merger

        /** The dispatcher of the currently installed platform, or `null` while none is installed. */
        fun activeDispatcher(): CommandDispatcher<CommandSender>? = installed.get()?.tree?.dispatcher

        /** The name registry of the currently installed platform, or `null` while none is installed. */
        fun activeOwnership(): MinestomCommandOwnership? = installed.get()?.ownership
    }
}
