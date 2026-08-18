package dev.slne.surf.api.minestom.dialog.callback

import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.BinaryTag
import net.minestom.server.dialog.DialogAction
import net.minestom.server.entity.Player
import org.jetbrains.annotations.ApiStatus
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object DialogCallbacks {
    private const val NAMESPACE = "surf_dialog_callback"

    private val registrations = ConcurrentHashMap<Key, Registration>()

    /**
     * Registers [callback] and returns the custom action key that runs it.
     */
    fun register(
        options: DialogCallbackOptions = DialogCallbackOptions.DEFAULT,
        callback: (DialogCallbackContext) -> Unit,
    ): Key {
        purgeExpired()

        val key = Key.key(NAMESPACE, UUID.randomUUID().toString())
        registrations[key] = Registration(options, callback)

        return key
    }

    /**
     * Registers [callback] and returns the custom action that runs it.
     */
    fun action(
        options: DialogCallbackOptions = DialogCallbackOptions.DEFAULT,
        callback: (DialogCallbackContext) -> Unit,
    ): DialogAction = DialogAction.Custom(register(options, callback), null)

    /**
     * Forgets the callback [key] was handed out for.
     */
    fun unregister(key: Key): Boolean = registrations.remove(key) != null

    /**
     * Runs the callback [key] was handed out for and reports whether one was still registered.
     */
    @ApiStatus.Internal
    internal fun dispatch(player: Player, key: Key, payload: BinaryTag?): Boolean {
        purgeExpired()

        val registration = registrations[key] ?: return false
        if (!registration.tryConsume()) {
            registrations.remove(key, registration)
            return false
        }

        if (registration.isExhausted) {
            registrations.remove(key, registration)
        }

        registration.callback(DialogCallbackContext(player, payload))
        return true
    }

    private fun purgeExpired() {
        registrations.values.removeIf { it.isExpired }
    }

    private class Registration(
        options: DialogCallbackOptions,
        val callback: (DialogCallbackContext) -> Unit,
    ) {
        private val expiresAt = System.nanoTime() + options.lifetime.inWholeNanoseconds
        private val remainingUses = AtomicInteger(options.uses)

        val isExpired get() = System.nanoTime() - expiresAt >= 0
        val isExhausted get() = remainingUses.get() == 0

        fun tryConsume(): Boolean {
            if (isExpired) return false

            while (true) {
                val remaining = remainingUses.get()
                if (remaining == DialogCallbackOptions.UNLIMITED_USES) return true
                if (remaining <= 0) return false
                if (remainingUses.compareAndSet(remaining, remaining - 1)) return true
            }
        }
    }
}
