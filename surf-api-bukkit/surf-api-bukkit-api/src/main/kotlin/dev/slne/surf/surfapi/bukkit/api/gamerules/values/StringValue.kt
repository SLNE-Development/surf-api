package dev.slne.surf.surfapi.bukkit.api.gamerules.values

import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.GreedyStringArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.arguments.TextArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.slne.surf.surfapi.bukkit.api.gamerules.GameRules
import org.bukkit.command.CommandSender
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.concurrent.ThreadSafe

/**
 * Thread-safe game-rule value storing a `String`.
 */
@ThreadSafe
class StringValue<CTX : Any>(type: GameRules.Type<StringValue<CTX>, CTX, String>) :
    GameRules.Value<StringValue<CTX>, CTX, String>(type) {
    private val value = AtomicReference("")

    /** @inheritDoc */
    override fun updateFromArgument(
        sender: CommandSender,
        args: CommandArguments,
        name: String,
        key: GameRules.Key<StringValue<CTX>, CTX, String>,
    ) {
        val newValue = args.get(name) as String
        value.set(newValue)
    }

    /** @inheritDoc */
    override fun get(): String = value.get()

    override fun self() = this

    /** @inheritDoc */
    override fun deserialize(value: String) {
        this.value.set(value)
    }

    /** @inheritDoc */
    override fun serialize(): String = value.get()

    /** @inheritDoc */
    override fun copy() = StringValue(type).also { it.value.set(this.value.get()) }

    /** @inheritDoc */
    override fun setFrom(
        other: StringValue<CTX>,
        context: CTX,
    ) {
        this.value.set(other.value.get())
        onChanged(context)
    }

    companion object {
        /**
         * Creates a rule that accepts a single word.
         */
        fun <CTX: Any> createWord(
            defaultValue: String = "",
            onChange: (CTX, StringValue<CTX>) -> Unit = { _, _ -> },
        ) = create(
            { name -> StringArgument(name) },
            defaultValue,
            onChange,
        )

        /**
         * Creates a rule that accepts any non-empty string.
         */
        fun <CTX: Any> createString(
            defaultValue: String = "",
            onChange: (CTX, StringValue<CTX>) -> Unit = { _, _ -> },
        ) = create(
            { name -> TextArgument(name) },
            defaultValue,
            onChange,
        )

        /**
         * Creates a rule that captures the remainder of the command line.
         */
        fun <CTX: Any> createGreedy(
            defaultValue: String = "",
            onChange: (CTX, StringValue<CTX>) -> Unit = { _, _ -> },
        ) = create(
            { name -> GreedyStringArgument(name) },
            defaultValue,
            onChange,
        )

        /**
         * Common factory used by the convenience creation methods.
         */
        private fun <CTX : Any> create(
            argumentCreator: (name: String) -> Argument<*>,
            defaultValue: String,
            onChange: (CTX, StringValue<CTX>) -> Unit,
        ) = GameRules.Type(
            argumentCreator,
            { type -> StringValue(type).also { it.value.set(defaultValue) } },
            onChange,
            { visitor, key, type -> visitor.visitString(key, type) },
        )
    }
}