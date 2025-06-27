package dev.slne.surf.surfapi.bukkit.api.gamerules.values

import dev.jorel.commandapi.arguments.TimeArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.slne.surf.surfapi.bukkit.api.gamerules.GameRules
import dev.slne.surf.surfapi.core.api.util.logger
import net.kyori.adventure.util.Ticks
import org.bukkit.command.CommandSender
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.concurrent.ThreadSafe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Thread-safe game-rule value storing a Kotlin [Duration].
 *
 * Values are represented as ticks in commands and serialized using ISO-8601
 * duration strings.
 */
@ThreadSafe
class DurationValue<CTX : Any>(type: GameRules.Type<DurationValue<CTX>, CTX, Duration>) :
    GameRules.Value<DurationValue<CTX>, CTX, Duration>(type) {
    private val value = AtomicReference(Duration.ZERO)

    /** @inheritDoc */
    override fun updateFromArgument(
        sender: CommandSender,
        args: CommandArguments,
        name: String,
        key: GameRules.Key<DurationValue<CTX>, CTX, Duration>,
    ) {
        val ticks = args.get(name) as Int
        value.set((ticks * Ticks.SINGLE_TICK_DURATION_MS).milliseconds)
    }

    /** @inheritDoc */
    /** @inheritDoc */
    override fun get(): Duration = value.get()
    override fun self() = this

    /** @inheritDoc */
    override fun deserialize(value: String) {
        try {
            this.value.set(Duration.parseIsoString(value))
        } catch (e: IllegalArgumentException) {
            log.atWarning()
                .log("Failed to deserialize DurationValue from string: '$value'. Using default value Duration.ZERO.")
            this.value.set(Duration.ZERO)
        }
    }

    /** @inheritDoc */
    override fun serialize() = value.get().toIsoString()
    /** @inheritDoc */
    override fun copy() = DurationValue(type).also { it.value.set(this.value.get()) }

    /** @inheritDoc */
    override fun displayValue() = value.get().toString()

    /** @inheritDoc */
    override fun setFrom(
        other: DurationValue<CTX>,
        context: CTX,
    ) {
        this.value.set(other.value.get())
        onChanged(context)
    }

    companion object {
        private val log = logger()

        /**
         * Factory for a [DurationValue] rule.
         *
         * @param defaultValue default starting duration
         * @param onChange     callback invoked whenever the value changes
         */
        fun <CTX : Any> create(
            defaultValue: Duration = Duration.ZERO,
            onChange: (CTX, DurationValue<CTX>) -> Unit = { _, _ -> },
        ) = GameRules.Type(
            { name -> TimeArgument(name) },
            { type -> DurationValue(type).also { it.value.set(defaultValue) } },
            onChange,
            { visitor, key, type -> visitor.visitDuration(key, type) }
        )
    }
}