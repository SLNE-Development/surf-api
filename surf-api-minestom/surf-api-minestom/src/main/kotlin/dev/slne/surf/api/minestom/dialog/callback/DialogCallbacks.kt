package dev.slne.surf.api.minestom.dialog.callback

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.BinaryTag
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.minestom.server.dialog.DialogAction
import net.minestom.server.entity.Player
import org.jetbrains.annotations.ApiStatus
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Registry of server-side callbacks triggered by custom dialog actions.
 *
 * Each registration is bound to a random action key and expires after the use count or lifetime of
 * its [DialogCallbackOptions]. Expired registrations are purged on [register] and [dispatch]. The
 * registry is thread-safe.
 */
object DialogCallbacks {
    private const val NAMESPACE = "surf_dialog_callback"

    private val registrations = ConcurrentHashMap<Key, Registration>()

    /**
     * Registers [callback] under [options] and returns the custom action key that triggers it.
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
     * Registers [callback] under [options] and returns the custom action that triggers it.
     */
    fun action(
        options: DialogCallbackOptions = DialogCallbackOptions.DEFAULT,
        callback: (DialogCallbackContext) -> Unit,
    ): DialogAction = DialogAction.Custom(register(options, callback), null)

    /**
     * Registers [callback] under [options] and returns the custom action that triggers it with the
     * dialog's input values.
     *
     * Unlike [action], the client sends the input values back, merged with [additions].
     */
    fun dynamicAction(
        options: DialogCallbackOptions = DialogCallbackOptions.DEFAULT,
        additions: CompoundBinaryTag? = null,
        callback: (DialogCallbackContext) -> Unit,
    ): DialogAction = DialogAction.DynamicCustom(register(options, callback), additions)

    /**
     * Removes the callback registered under [key].
     *
     * @return whether a callback was registered under [key]
     */
    fun unregister(key: Key): Boolean = registrations.remove(key) != null

    /**
     * Runs the callback registered under [key] with [player] and [payload], consuming one use.
     *
     * @return `false` when no callback is registered under [key] or it has expired or run out of uses
     */
    @ApiStatus.Internal
    @InternalSurfApi
    fun dispatch(player: Player, key: Key, payload: BinaryTag?): Boolean {
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
