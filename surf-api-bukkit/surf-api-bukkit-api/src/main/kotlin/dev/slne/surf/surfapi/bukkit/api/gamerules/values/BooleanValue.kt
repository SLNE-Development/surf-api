package dev.slne.surf.surfapi.bukkit.api.gamerules.values

import dev.jorel.commandapi.arguments.BooleanArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.slne.surf.surfapi.bukkit.api.gamerules.GameRules
import org.bukkit.command.CommandSender
import java.util.concurrent.atomic.AtomicBoolean

class BooleanValue<CTX : Any>(type: GameRules.Type<BooleanValue<CTX>, CTX, Boolean>) :
    GameRules.Value<BooleanValue<CTX>, CTX, Boolean>(type) {
    private val value = AtomicBoolean()

    override fun updateFromArgument(
        sender: CommandSender,
        args: CommandArguments,
        name: String,
        key: GameRules.Key<BooleanValue<CTX>, CTX, Boolean>,
    ) {
        val value = args.get(name) as Boolean
        this.value.set(value)
    }

    override fun get() = value.get()
    override fun self() = this

    override fun deserialize(value: String) {
        this.value.set(value.toBoolean())
    }

    override fun serialize() = value.get().toString()
    override fun copy() = BooleanValue(type).also { it.value.set(this.value.get()) }

    override fun setFrom(
        other: BooleanValue<CTX>,
        context: CTX,
    ) {
        this.value.set(other.value.get())
        onChanged(context)
    }

    companion object {
        fun <CTX : Any> create(
            defaultValue: Boolean,
            onChange: (CTX, BooleanValue<CTX>) -> Unit = { _, _ -> },
        ) = GameRules.Type(
            ::BooleanArgument,
            { type -> BooleanValue(type).also { it.value.set(defaultValue) } },
            onChange,
            { visitor, key, type -> visitor.visitBoolean(key, type) },
        )
    }
}