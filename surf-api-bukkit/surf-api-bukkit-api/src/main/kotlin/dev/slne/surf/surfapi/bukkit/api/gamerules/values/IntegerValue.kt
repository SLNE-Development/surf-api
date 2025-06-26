package dev.slne.surf.surfapi.bukkit.api.gamerules.values

import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.slne.surf.surfapi.bukkit.api.gamerules.GameRules
import dev.slne.surf.surfapi.core.api.util.logger
import org.bukkit.command.CommandSender
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.concurrent.ThreadSafe

@ThreadSafe
class IntegerValue<CTX : Any>(type: GameRules.Type<IntegerValue<CTX>, CTX, Int>) :
    GameRules.Value<IntegerValue<CTX>, CTX, Int>(type) {
    private val value = AtomicInteger()

    override fun updateFromArgument(
        sender: CommandSender,
        args: CommandArguments,
        name: String,
        key: GameRules.Key<IntegerValue<CTX>, CTX, Int>,
    ) {
        val newValue = args.get(name) as Int
        value.set(newValue)
    }

    override fun get() = value.get()
    override fun self() = this

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

    override fun serialize() = value.get().toString()
    override fun copy() = IntegerValue(type).also { it.value.set(this.value.get()) }


    override fun setFrom(
        other: IntegerValue<CTX>,
        context: CTX,
    ) {
        this.value.set(other.value.get())
        onChanged(context)
    }

    companion object {
        private val log = logger()

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