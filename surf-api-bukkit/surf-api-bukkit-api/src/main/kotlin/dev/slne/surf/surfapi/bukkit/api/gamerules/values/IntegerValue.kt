package dev.slne.surf.surfapi.bukkit.api.gamerules.values

import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.slne.surf.surfapi.bukkit.api.gamerules.GameRules
import dev.slne.surf.surfapi.core.api.util.logger
import org.bukkit.command.CommandSender
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.concurrent.ThreadSafe

/**
 * Thread-safe game-rule value storing an `Int`.
 */
@ThreadSafe
class IntegerValue<CTX : Any>(type: GameRules.Type<IntegerValue<CTX>, CTX, Int>) :
    GameRules.Value<IntegerValue<CTX>, CTX, Int>(type) {
    private val value = AtomicInteger()

    /** @inheritDoc */
    override fun updateFromArgument(
        sender: CommandSender,
        args: CommandArguments,
        name: String,
        key: GameRules.Key<IntegerValue<CTX>, CTX, Int>,
    ) {
        val newValue = args.get(name) as Int
        value.set(newValue)
    }

    /** @inheritDoc */
    override fun get() = value.get()
    override fun self() = this

    /** @inheritDoc */
    override fun deserialize(value: String) {
        val deserialized = value.toIntOrNull()
        if (deserialized != null) {
            this.value.set(deserialized)
        } else {
            log.atWarning()
                .log("Failed to deserialize IntegerValue from string: '$value'. Using default value 0.")
            this.value.set(0)
        }
    }

    /** @inheritDoc */
    override fun serialize() = value.get().toString()

    /** @inheritDoc */
    override fun copy() = IntegerValue(type).also { it.value.set(this.value.get()) }


    /** @inheritDoc */
    override fun setFrom(
        other: IntegerValue<CTX>,
        context: CTX,
    ) {
        this.value.set(other.value.get())
        onChanged(context)
    }

    companion object {
        private val log = logger()

        /**
         * Factory for an [IntegerValue] rule.
         *
         * @param defaultValue default starting value
         * @param min minimum allowed value
         * @param max maximum allowed value
         * @param onChange callback invoked whenever the value changes
         */
        fun <CTX : Any> create(
            defaultValue: Int,
            min: Int = Int.MIN_VALUE,
            max: Int = Int.MAX_VALUE,
            onChange: (CTX, IntegerValue<CTX>) -> Unit = { _, _ -> },
        ) = GameRules.Type(
            { IntegerArgument(it, min, max) },
            { type -> IntegerValue(type).also { it.value.set(defaultValue) } },
            onChange,
            { visitor, key, type -> visitor.visitInteger(key, type) },
        )
    }
}