package dev.slne.surf.surfapi.bukkit.api.gamerules.values

import com.google.common.util.concurrent.AtomicDouble
import dev.jorel.commandapi.arguments.DoubleArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.slne.surf.surfapi.bukkit.api.gamerules.GameRules
import dev.slne.surf.surfapi.core.api.util.logger
import org.bukkit.command.CommandSender
import javax.annotation.concurrent.ThreadSafe

@ThreadSafe
class DoubleValue<CTX : Any>(type: GameRules.Type<DoubleValue<CTX>, CTX, Double>) :
    GameRules.Value<DoubleValue<CTX>, CTX, Double>(type) {

    private val value = AtomicDouble()

    override fun updateFromArgument(
        sender: CommandSender,
        args: CommandArguments,
        name: String,
        key: GameRules.Key<DoubleValue<CTX>, CTX, Double>,
    ) {
        val newValue = args.get(name) as Double
        value.set(newValue)
    }

    override fun get() = value.get()
    override fun self() = this

    override fun deserialize(value: String) {
        val deserialized = value.toDoubleOrNull()
        if (deserialized != null) {
            this.value.set(deserialized)
        } else {
            log.atWarning()
                .log("Failed to deserialize DoubleValue from string: '$value'. Using default value 0.0.")
            this.value.set(0.0)
        }
    }

    override fun serialize() = value.get().toString()
    override fun copy() = DoubleValue(type).also { it.value.set(this.value.get()) }

    override fun setFrom(
        other: DoubleValue<CTX>,
        context: CTX,
    ) {
        this.value.set(other.value.get())
        onChanged(context)
    }

    companion object {
        private val log = logger()

        fun <CTX : Any> create(
            defaultValue: Double,
            min: Double = Double.MIN_VALUE,
            max: Double = Double.MAX_VALUE,
            onChange: (CTX, DoubleValue<CTX>) -> Unit = { _, _ -> },
        ) = GameRules.Type(
            { name -> DoubleArgument(name, min, max) },
            { type -> DoubleValue(type).also { it.value.set(defaultValue) } },
            onChange,
            { visitor, key, type -> visitor.visitDouble(key, type) },
        )
    }
}