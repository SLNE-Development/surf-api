package dev.slne.surf.surfapi.bukkit.api.gamerules

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.CommandExecutor
import dev.slne.surf.surfapi.bukkit.api.gamerules.values.*
import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import dev.slne.surf.surfapi.core.api.util.synchronize
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap
import net.kyori.adventure.nbt.BinaryTagIO
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.nbt.StringBinaryTag
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KProperty
import kotlin.time.Duration

abstract class GameRules<CTX : Any> {

    private val lastGameRuleIndex = AtomicInteger(0)
    protected val gameRuleTypes =
        Object2ObjectRBTreeMap<Key<*, CTX, *>, Type<*, CTX, *>>(compareBy { it.id }).synchronize()

    @Volatile
    private var frozen = false

    protected fun <T : Value<T, CTX, V>, V> register(
        name: String,
        description: String,
        type: Type<T, CTX, V>,
    ): Key<T, CTX, V> {
        require(!frozen) { "Cannot register new game rules after the GameRules instance has been frozen." }

        val key: Key<T, CTX, V> = createKey(name, description)
        require(
            gameRuleTypes.putIfAbsent(
                key,
                type
            ) == null
        ) { "Game rule with id '${key.id}' already exists." }
        return key
    }

    private fun <T : Value<T, CTX, V>, V> createKey(
        id: String,
        description: String,
    ): Key<T, CTX, V> {
        return Key(id, description, lastGameRuleIndex.getAndIncrement())
    }

    fun createRuleSet(): RuleSet = RuleSet().also { frozen = true }
    fun createRuleSet(values: CompoundBinaryTag): RuleSet = RuleSet(values).also { frozen = true }
    fun loadRuleSet(file: Path): RuleSet = RuleSet(file).also { frozen = true }

    fun addToCommandTree(
        tree: CommandTree,
        getRuleSet: (CommandSender) -> RuleSet,
        getContext: (CommandSender) -> CTX,
    ): Unit = with(tree) {
        val ruleSet = createRuleSet()
        ruleSet.visitGameRuleTypes(object : GameRuleTypeVisitor<CTX> {
            override fun <T : Value<T, CTX, V>, V> visit(
                key: Key<T, CTX, V>,
                type: Type<T, CTX, V>,
            ) {
                then(
                    LiteralArgument.of(key.id)
                        .executes(CommandExecutor { sender, _ ->
                            queryRule(sender, key, getRuleSet(sender))
                        })
                        .then(
                            type.createArgument("value").executes(CommandExecutor { sender, args ->
                                setRule(sender, key, getRuleSet(sender), getContext(sender), args)
                            })
                        )
                )
            }
        })
    }

    fun addToCommandTree(
        argument: Argument<*>,
        getRuleSet: (CommandSender) -> RuleSet,
        getContext: (CommandSender) -> CTX,
    ): Unit = with(argument) {
        val ruleSet = createRuleSet()
        ruleSet.visitGameRuleTypes(object : GameRuleTypeVisitor<CTX> {
            override fun <T : Value<T, CTX, V>, V> visit(
                key: Key<T, CTX, V>,
                type: Type<T, CTX, V>,
            ) {
                then(
                    LiteralArgument.of(key.id)
                        .executes(CommandExecutor { sender, _ ->
                            queryRule(sender, key, getRuleSet(sender))
                        })
                        .then(
                            type.createArgument("value").executes(CommandExecutor { sender, args ->
                                setRule(sender, key, getRuleSet(sender), getContext(sender), args)
                            })
                        )
                )
            }
        })
    }

    private fun <T : Value<T, CTX, V>, V> setRule(
        sender: CommandSender,
        key: Key<T, CTX, V>,
        rules: RuleSet,
        context: CTX,
        args: CommandArguments,
    ) {
        val rule = rules.getRule(key)
        rule.setFromArgument(sender, args, "value", key, context)
        sender.sendText {
            appendPrefix()
            success("Die Spielregel ")
            append {
                variableValue(key.id)
                hoverEvent(Component.text(key.description, Colors.INFO))
            }
            success(" wurde auf ")
            variableValue(rule.displayValue())
            success(" gesetzt.")
        }
    }

    private fun queryRule(sender: CommandSender, rule: Key<*, CTX, *>, rules: RuleSet) {
        sender.sendText {
            appendPrefix()
            success("Die Spielregel ")
            append {
                variableValue(rule.id)
                hoverEvent(Component.text(rule.description, Colors.INFO))
            }
            success(" ist auf ")
            variableValue(rules[rule].displayValue())
            success(" gesetzt.")
        }
    }

    inner class RuleSet internal constructor(rules: Object2ObjectMap<Key<*, CTX, *>, Value<*, CTX, *>>) {
        private val rules: Object2ObjectMap<Key<*, CTX, *>, Value<*, CTX, *>> = rules.synchronize()

        private val gameruleArray = arrayOfNulls<Value<*, CTX, *>>(lastGameRuleIndex.get()).apply {
            for ((key, value) in rules.object2ObjectEntrySet()) {
                this[key.gameRuleIndex] = value
            }
        }.requireNoNulls()

        internal constructor(values: CompoundBinaryTag) : this() {
            loadFromTag(values)
        }

        internal constructor(file: Path) : this(
            BinaryTagIO.unlimitedReader().read(file, BinaryTagIO.Compression.GZIP)
        )

        internal constructor() : this(
            gameRuleTypes.object2ObjectEntrySet()
                .associateTo(mutableObject2ObjectMapOf()) { it.key to it.value.createRule() }
                .freeze()
        )

        fun <T : Value<T, CTX, V>, V> getRule(key: Key<T, CTX, V>): T {
            return gameruleArray[key.gameRuleIndex] as T
        }

        fun <T : Value<T, CTX, V>, V> getValue(key: Key<T, CTX, V>): V {
            return getRule(key).get()
        }

        operator fun <T : Value<T, CTX, V>, V> get(key: Key<T, CTX, V>): T {
            return getRule(key)
        }

        fun createTag(): CompoundBinaryTag {
            val builder = CompoundBinaryTag.builder()
            for ((key, value) in rules.object2ObjectEntrySet()) {
                builder.putString(key.id, value.serialize())
            }
            return builder.build()
        }

        fun saveToFile(file: Path) {
            val tag = createTag()
            BinaryTagIO.writer()
                .write(tag, file, BinaryTagIO.Compression.GZIP)
        }

        private fun loadFromTag(tag: CompoundBinaryTag) {
            for ((key, value) in rules.object2ObjectEntrySet()) {
                val serialized = (tag.get(key.id) as? StringBinaryTag)?.value() ?: continue
                value.deserialize(serialized)
            }
        }

        fun visitGameRuleTypes(visitor: GameRuleTypeVisitor<CTX>) {
            for ((key, type) in gameRuleTypes.object2ObjectEntrySet()) {
                callVisitorCap<Nothing, Any?>(visitor, key, type)
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : Value<T, CTX, V>, V> callVisitorCap(
            visitor: GameRuleTypeVisitor<CTX>,
            key: Key<*, CTX, *>,
            type: Type<*, CTX, *>,
        ) {
            visitor.visit(key as Key<T, CTX, V>, type as Type<T, CTX, V>)
            type.callVisitor(visitor, key)
        }

        fun assignFrom(other: RuleSet, context: CTX) {
            for (key in other.rules.keys) {
                assignCap(key, other, context)
            }
        }

        private fun <T : Value<T, CTX, V>, V> assignCap(
            key: Key<T, CTX, V>,
            rules: RuleSet,
            context: CTX,
        ) {
            val rule = rules.getRule(key)
            this.getRule(key).setFrom(rule, context)
        }
    }

    data class Key<T : Value<T, CTX, V>, CTX : Any, V>(
        val id: String,
        val description: String,
        val gameRuleIndex: Int,
    )

    data class Type<T : Value<T, CTX, V>, CTX : Any, V>(
        private val argumentCreator: (name: String) -> Argument<*>,
        private val ruleFactory: (Type<T, CTX, V>) -> T,
        internal val changeCallback: (CTX, T) -> Unit,
        private val visitorCaller: VisitorCaller<T, CTX, V>,
    ) {

        fun createArgument(name: String): Argument<*> {
            return argumentCreator(name)
        }

        fun createRule(): T {
            return ruleFactory(this)
        }

        fun callVisitor(visitor: GameRuleTypeVisitor<CTX>, key: Key<T, CTX, V>) {
            visitorCaller.call(visitor, key, this)
        }
    }

    abstract class Value<SELF : Value<SELF, CTX, V>, CTX : Any, V>(protected val type: Type<SELF, CTX, V>) {
        protected abstract fun updateFromArgument(
            sender: CommandSender,
            args: CommandArguments,
            name: String,
            key: Key<SELF, CTX, V>,
        )

        abstract fun get(): V
        protected abstract fun self(): SELF
        operator fun getValue(thisRef: Any?, property: KProperty<*>) = get()

        fun setFromArgument(
            sender: CommandSender,
            args: CommandArguments,
            name: String,
            key: Key<SELF, CTX, V>,
            context: CTX,
        ) {
            updateFromArgument(sender, args, name, key)
            onChanged(context)
        }

        fun onChanged(context: CTX) {
            type.changeCallback(context, self())
        }

        abstract fun deserialize(value: String)

        abstract fun serialize(): String
        open fun displayValue(): String = serialize()

        override fun toString(): String {
            return serialize()
        }

        protected abstract fun copy(): SELF
        abstract fun setFrom(other: SELF, context: CTX)
    }

    interface GameRuleTypeVisitor<CTX : Any> {
        fun <T : Value<T, CTX, V>, V> visit(key: Key<T, CTX, V>, type: Type<T, CTX, V>) = Unit

        fun visitBoolean(
            key: Key<BooleanValue<CTX>, CTX, Boolean>,
            type: Type<BooleanValue<CTX>, CTX, Boolean>,
        ) = Unit

        fun visitInteger(
            key: Key<IntegerValue<CTX>, CTX, Int>,
            type: Type<IntegerValue<CTX>, CTX, Int>,
        ) = Unit

        fun visitLong(
            key: Key<LongValue<CTX>, CTX, Long>,
            type: Type<LongValue<CTX>, CTX, Long>,
        ) = Unit

        fun visitDouble(
            key: Key<DoubleValue<CTX>, CTX, Double>,
            type: Type<DoubleValue<CTX>, CTX, Double>,
        ) = Unit

        fun visitString(
            key: Key<StringValue<CTX>, CTX, String>,
            type: Type<StringValue<CTX>, CTX, String>,
        ) = Unit

        fun visitDuration(
            key: Key<DurationValue<CTX>, CTX, Duration>,
            type: Type<DurationValue<CTX>, CTX, Duration>,
        ) = Unit
    }

    fun interface VisitorCaller<T : Value<T, CTX, V>, CTX : Any, V> {
        fun call(visitor: GameRuleTypeVisitor<CTX>, key: Key<T, CTX, V>, type: Type<T, CTX, V>)
    }
}