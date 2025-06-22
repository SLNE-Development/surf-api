package dev.slne.surf.surfapi.bukkit.api.gamerules

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.DoubleArgument
import dev.jorel.commandapi.arguments.LongArgument
import dev.jorel.commandapi.arguments.TimeArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.adventure.buildText
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import net.kyori.adventure.text.Component
import net.querz.nbt.tag.CompoundTag
import org.bukkit.command.CommandSender
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class OldGameRules<CTX : Any> {

    class Key<T : Value<T>>(val id: String, val description: String, internal val index: Int) {
        override fun toString() = id
    }

    inner class Type<T : Value<T>> internal constructor(
        private val argumentFactory: (String) -> Argument<*>?,
        internal val constructor: (Type<T>) -> T,
        internal val onChanged: (CTX, T) -> Unit = { _, _ -> },
    ) {
        fun createArgument(name: String): Argument<*>? = argumentFactory(name)
        fun createInstance(): T = constructor(this)
    }

    abstract inner class Value<T : Value<T>>(protected val type: Type<T>) {
        abstract fun serialize(): String
        abstract fun deserialize(data: String)
        protected abstract fun self(): T
        abstract fun copy(): T

        abstract fun updateFromArgument(args: CommandArguments, name: String, gameRuleKey: Key<T>, ctx: CTX)
        abstract fun set(value: T, ctx: CTX)
        fun changed(ctx: CTX) = type.onChanged(ctx, self())
    }

    private val registry = mutableObject2ObjectMapOf<Key<*>, Type<*>>()

    protected fun <T : Value<T>> register(id: String, description: String, type: Type<T>): Key<T> {
        val key = Key<T>(id, description, registry.size)
        check(registry.putIfAbsent(key, type) == null) { "Rule $id already registered" }
        return key
    }

    protected fun boolean(
        id: String,
        default: Boolean,
        description: String,
        onChange: (CTX, BooleanValue) -> Unit = { _, _ -> },
    ): Key<BooleanValue> = register(id, description, BooleanValue.type(default, onChange))

    protected fun integer(
        id: String,
        default: Int,
        description: String,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        onChange: (CTX, IntegerValue) -> Unit = { _, _ -> },
    ): Key<IntegerValue> = register(id, description, IntegerValue.type(default, min, max, onChange))

    protected fun double(
        id: String,
        default: Double,
        description: String,
        min: Double = -Double.MAX_VALUE,
        max: Double = Double.MAX_VALUE,
        onChange: (CTX, DoubleValue) -> Unit = { _, _ -> },
    ): Key<DoubleValue> = register(id, description, DoubleValue.type(default, min, max, onChange))

    protected fun long(
        id: String,
        default: Long,
        description: String,
        min: Long = Long.MIN_VALUE,
        max: Long = Long.MAX_VALUE,
        onChange: (CTX, LongValue) -> Unit = { _, _ -> },
    ): Key<LongValue> = register(id, description, LongValue.type(default, min, max, onChange))

    protected fun duration(
        id: String,
        default: Duration,
        description: String,
        onChange: (CTX, DurationValue) -> Unit = { _, _ -> },
    ): Key<DurationValue> = register(id, description, DurationValue.type(default, onChange))

    /**
     * Create a fresh, mutable rule‑set instance. Each context (game round,
     * match, world, …) normally gets its own [RuleSet].
     */
    fun createRuleSet(): RuleSet = RuleSet()

    fun CommandTree.registerRules(
        queryRule: (Key<*>) -> Value<*>,
        getContext: (CommandSender) -> CTX,
        renderQuery: (Key<*>, Value<*>) -> Component = { key, value ->
            buildText {
                appendPrefix()
                success("Die Spielregel ")
                append {
                    variableValue(key.id)
                    hoverEvent(Component.text(key.description, Colors.INFO))
                }
                success(" ist auf ")
                variableValue(value.serialize())
                success(" gesetzt.")
            }
        },
        renderUpdate: (Key<*>, Value<*>) -> Component = { key, value ->
            buildText {
                appendPrefix()
                success("Die Spielregel ")
                append {
                    variableValue(key.id)
                    hoverEvent(Component.text(key.description, Colors.INFO))
                }
                success(" wurde auf ")
                variableValue(value.serialize())
                success(" aktualisiert.")
            }
        },
    ) {
        for ((key, type) in registry) {
            literalArgument(key.id) {
                anyExecutor { sender, args ->
                    val result = queryRule(key)
                    sender.sendMessage(renderQuery(key, result))
                }

                then(type.createArgument("value")).apply {
                    anyExecutor { sender, args ->
                        val value = queryRule(key)
                        value.updateFromArgument(args, "value", key, getContext(sender))

                    }
                }
            }
        }
    }

    inner class RuleSet internal constructor() {
        private val store: Array<Value<*>> = Array(registry.size) { idx ->
            @Suppress("UNCHECKED_CAST")
            val type = registry.values.elementAt(idx) as Type<Value<*>>
            type.createInstance()
        }

        /** Access a value by [Key]. */
        @Suppress("UNCHECKED_CAST")
        operator fun <T : Value<T>> get(key: Key<T>): T = store[key.index] as T

        operator fun <T : Value<T>> set(key: Key<T>, value: T, ctx: CTX) {
            value.set(value, ctx)
            value.changed(ctx)
        }

        /** Serialize all rules to NBT. (Can easily be swapped for JSON, YAML…) */
        fun toNbt(): CompoundTag = CompoundTag().also { nbt ->
            registry.keys.forEach { key ->
                nbt.putString(key.id, store[key.index].serialize())
            }
        }

        /** Load from NBT blob. */
        fun fromNbt(nbt: CompoundTag) {
            registry.keys.forEach { key ->
                nbt.getString(key.id)?.let { data ->
                    store[key.index].deserialize(data)
                }
            }
        }

        /** Iterate over *definitions* and *values* in registration order. */
        fun forEach(action: (Key<*>, Value<*>) -> Unit) {
            registry.keys.forEach { key -> action(key, store[key.index]) }
        }
    }

    inner class BooleanValue private constructor(
        type: Type<BooleanValue>,
        private var value: Boolean,
    ) :
        Value<BooleanValue>(type) {
        fun get(): Boolean = value
        fun set(v: Boolean, ctx: CTX) {
            value = v; changed(ctx)
        }

        override fun serialize() = value.toString()
        override fun deserialize(data: String) {
            value = data.toBoolean()
        }

        override fun self() = this
        override fun copy() = BooleanValue(type, value)

        companion object {
            fun <CTX : Any> type(
                default: Boolean,
                onChange: (CTX, BooleanValue) -> Unit,
            ) = Type<BooleanValue>({ BooleanArgument(it) }, { BooleanValue(it, default) }, onChange)
        }
    }

    inner class IntegerValue private constructor(type: Type<IntegerValue>, private var value: Int) :
        Value<IntegerValue>(type) {
        fun get(): Int = value
        fun set(v: Int, ctx: CTX) {
            value = v; changed(ctx)
        }

        override fun serialize() = value.toString()
        override fun deserialize(data: String) {
            value = data.toInt()
        }

        override fun self() = this
        override fun copy() = IntegerValue(type, value)

        companion object {
            fun <CTX : Any> type(
                default: Int,
                min: Int,
                max: Int,
                onChange: (CTX, IntegerValue) -> Unit,
            ) = Type<IntegerValue>(
                { IntegerArgument(it, min, max) },
                { IntegerValue(it, default) },
                onChange
            )
        }
    }

    inner class DoubleValue private constructor(
        type: Type<DoubleValue>,
        private var value: Double,
    ) :
        Value<DoubleValue>(type) {
        fun get(): Double = value
        fun set(v: Double, ctx: CTX) {
            value = v; changed(ctx)
        }

        override fun serialize() = value.toString()
        override fun deserialize(data: String) {
            value = data.toDouble()
        }

        override fun self() = this
        override fun copy() = DoubleValue(type, value)

        companion object {
            fun <CTX : Any> type(
                default: Double,
                min: Double,
                max: Double,
                onChange: (CTX, DoubleValue) -> Unit,
            ) = Type<DoubleValue>(
                { DoubleArgument(it, min, max) },
                { DoubleValue(it, default) },
                onChange
            )
        }
    }

    inner class LongValue private constructor(type: Type<LongValue>, private var value: Long) :
        Value<LongValue>(type) {
        fun get(): Long = value
        fun set(v: Long, ctx: CTX) {
            value = v; changed(ctx)
        }

        override fun serialize() = value.toString()
        override fun deserialize(data: String) {
            value = data.toLong()
        }

        override fun self() = this
        override fun copy() = LongValue(type, value)

        companion object {
            fun <CTX : Any> type(
                default: Long,
                min: Long,
                max: Long,
                onChange: (CTX, LongValue) -> Unit,
            ) = Type<LongValue>(
                { LongArgument(it, min, max) },
                { LongValue(it, default) },
                onChange
            )
        }
    }

    inner class DurationValue private constructor(
        type: Type<DurationValue>,
        private var value: Duration,
    ) :
        Value<DurationValue>(type) {
        fun get(): Duration = value
        fun set(v: Duration, ctx: CTX) {
            value = v; changed(ctx)
        }

        override fun serialize() = value.inWholeMilliseconds.toString()
        override fun deserialize(data: String) {
            value = data.toLong().milliseconds
        }

        override fun self() = this
        override fun copy() = DurationValue(type, value)

        companion object {
            fun <CTX : Any> type(
                default: Duration,
                onChange: (CTX, DurationValue) -> Unit,
            ) = Type<DurationValue>(
                { TimeArgument(it) },
                { DurationValue(it, default) },
                onChange
            )
        }
    }
}