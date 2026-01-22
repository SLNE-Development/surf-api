package dev.slne.surf.surfapi.bukkit.api.gamerules.values

import dev.jorel.commandapi.arguments.LongArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.slne.surf.surfapi.bukkit.api.gamerules.GameRules
import dev.slne.surf.surfapi.core.api.util.logger
import org.bukkit.command.CommandSender
import java.util.concurrent.atomic.AtomicLong
import javax.annotation.concurrent.ThreadSafe

/**
 * Thread-safe game-rule value storing a `Long`.
 */
@ThreadSafe
class LongValue<CTX: Any>(type: GameRules.Type<LongValue<CTX>, CTX, Long>) : GameRules.Value<LongValue<CTX>, CTX, Long>(
    type
) {
    private val value = AtomicLong()

    /** @inheritDoc */
    override fun updateFromArgument(
        sender: CommandSender,
        args: CommandArguments,
        name: String,
        key: GameRules.Key<LongValue<CTX>, CTX, Long>,
    ) {
        val newValue = args.get(name) as Long
        value.set(newValue)
    }

    /** @inheritDoc */
    override fun get() = value.get()
    override fun self() = this

    /** @inheritDoc */
    override fun deserialize(value: String) {
        val deserialized = value.toLongOrNull()
        if (deserialized != null) {
            this.value.set(deserialized)
        } else {
            log.atWarning()
                .log("Failed to deserialize LongValue from string: '$value'. Using default value 0L.")
            this.value.set(0L)
        }
    }

    /** @inheritDoc */
    override fun serialize() = value.get().toString()

    /** @inheritDoc */
    override fun copy() = LongValue(type).also { it.value.set(this.value.get()) }

    /** @inheritDoc */
    override fun setFrom(
        other: LongValue<CTX>,
        context: CTX,
    ) {
        this.value.set(other.value.get())
        onChanged(context)
    }

    companion object {
        private val log = logger()

        /**
         * Factory for a [LongValue] rule.
         *
         * @param defaultValue default starting value
         * @param min minimum allowed value
         * @param max maximum allowed value
         * @param onChange callback invoked whenever the value changes
         */
        fun <CTX: Any> create(
            defaultValue: Long,
            min: Long = Long.MIN_VALUE,
            max: Long = Long.MAX_VALUE,
            onChange: (CTX, LongValue<CTX>) -> Unit = { _, _ -> },
        ) = GameRules.Type(
            { name -> LongArgument(name, min, max) },
            { type -> LongValue(type).also { it.value.set(defaultValue) } },
            onChange,
            { visitor, key, type -> visitor.visitLong(key, type) },
        )
    }
}