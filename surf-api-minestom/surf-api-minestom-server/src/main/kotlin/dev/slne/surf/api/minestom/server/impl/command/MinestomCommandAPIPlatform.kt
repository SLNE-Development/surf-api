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
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import kotlinx.coroutines.CoroutineScope
import net.minestom.server.command.CommandManager
import net.minestom.server.command.CommandSender
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Installs CommandAPI commands into the Brigadier dispatcher, which owns their parsing and dispatch.
 *
 * Every command is also represented in Minestom's own command manager by a [CommandAPIBridgeCommand]
 * that hands the whole input over to the dispatcher, so the command runs no matter how Minestom is
 * asked to execute it. The manager is consulted first so a name already taken by a foreign Minestom
 * command is rejected rather than shadowed.
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
    private val bridges = Reference2ObjectOpenHashMap<CompiledRegistration, CommandAPIBridgeCommand>()
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

            val bridge = CommandAPIBridgeCommand(compiled.names)
            commandManager.register(bridge)
            bridges[compiled] = bridge

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
            bridges.remove(compiled)?.let(commandManager::unregister)
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
         * Minestom reaches the platform through [CommandAPIHook] from its own command manager and
         * packet pipeline, neither of which can be handed an instance.
         */
        fun activeMerger(): DeclareCommandsMerger? = installed.get()?.merger

        /** The dispatcher of the currently installed platform, or `null` while none is installed. */
        fun activeDispatcher(): CommandDispatcher<CommandSender>? = installed.get()?.tree?.dispatcher

        /** The name registry of the currently installed platform, or `null` while none is installed. */
        fun activeOwnership(): MinestomCommandOwnership? = installed.get()?.ownership
    }
}
