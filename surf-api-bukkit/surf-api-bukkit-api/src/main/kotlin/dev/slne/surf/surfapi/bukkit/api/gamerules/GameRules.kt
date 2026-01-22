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

/**
 * Base class for all Surf **game-rule registries**.
 *
 * A concrete implementation supplies a set of supported rule *types*
 * (boolean, integer, string, …), then calls [register] for each rule it wishes
 * to expose.  Once the registry is **frozen**—that is, after the first
 * [createRuleSet] / [loadRuleSet] call—no further rules can be added.
 *
 * @param CTX  A *context* object passed to every change-callback.  Typical use
 *             cases include a plugin’s main class, a world reference, or
 *             per-match state container.
 *
 * ## Thread safety
 * *  **Registration phase:** Not thread-safe.  Register rules only during
 *    plugin initialisation, *before* players can interact with commands.
 * *  **Runtime phase:** All reads/writes to rule *values* are thread-safe
 *    thanks to the atomic primitives used by individual [Value] subclasses.
 *
 * ## Command integration
 * Use [addToCommandTree] to attach the entire rule hierarchy to an existing
 * CommandAPI tree or argument chain:
 *
 * ```kotlin
 * val ruleSets = mutableObject2ObjectMapOf<UUID, GameRules<Any>.RuleSet>()
 *
 * commandTree("tree") {
 *     gameRules.addToCommandTree(
 *         this,
 *         getRuleSet = { sender ->
 *             if (sender !is Player) throw CommandAPI.failWithString("This command can only be used by players.")
 *             ruleSets.computeIfAbsent(sender.uniqueId) { gameRules.createRuleSet() }
 *         },
 *         getContext = { Any() }
 *     )
 * }
 * ```
 */
abstract class GameRules<CTX : Any> {

    private val lastGameRuleIndex = AtomicInteger(0)
    protected val gameRuleTypes =
        Object2ObjectRBTreeMap<Key<*, CTX, *>, Type<*, CTX, *>>(compareBy { it.id }).synchronize()

    @Volatile
    private var frozen = false

    /**
     * Registers a new game rule *type* and returns its strongly-typed key.
     *
     * The returned [Key] is later used to query or mutate values in a
     * [RuleSet].
     * Registration **must** happen before the registry is frozen
     * (see [frozen]); otherwise an [IllegalStateException] is thrown.
     *
     * @param name         Machine-readable identifier (shown in commands).
     * @param description  Human-readable tooltip used in hover text.
     * @param type         Value factory + CommandAPI argument + change-callback.
     * @receiver           Only visible to subclasses; encourages a
     *                     “`registerAll()` in init‐block” style.
     *
     * @throws IllegalStateException If called after any rule-set was created.
     * @throws IllegalArgumentException If another rule with identical id exists.
     */
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

    /**
     * Creates a new [RuleSet] populated with the default values of every
     * registered rule type.  After the first invocation the entire registry
     * becomes frozen and no further rule types may be registered.
     */
    fun createRuleSet(): RuleSet = RuleSet().also { frozen = true }

    /**
     * Deserializes a [RuleSet] from the provided [CompoundBinaryTag].  The
     * resulting instance is fully initialised and the registry is frozen on
     * first creation.
     */
    fun createRuleSet(values: CompoundBinaryTag): RuleSet = RuleSet(values).also { frozen = true }

    /**
     * Loads a [RuleSet] from the given NBT [file].  This behaves the same as
     * [createRuleSet] but reads the serialized values from disk.
     */
    fun loadRuleSet(file: Path): RuleSet = RuleSet(file).also { frozen = true }

    /**
     * Adds *every* registered rule to the supplied [CommandTree].
     *
     * For each rule this produces:
     *
     * ```
     * /<root> <ruleId>            → prints current value
     * /<root> <ruleId> <value>    → sets value & fires callback
     * ```
     *
     * @param tree         The CommandTree to attach to.
     * @param getRuleSet   Lambda that maps a `CommandSender` to the
     *                     corresponding [RuleSet] (e.g. per-world).
     * @param getContext   Lambda that maps a `CommandSender` to the
     *                    context object of type `CTX` (e.g. per-plugin).
     * @param setValueArgumentName Name of the argument used to set the value (default: `"value"`).
     */
    fun addToCommandTree(
        tree: CommandTree,
        getRuleSet: (CommandSender) -> RuleSet,
        getContext: (CommandSender) -> CTX,
        setValueArgumentName: String = "value",
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
                            type.createArgument(setValueArgumentName)
                                .executes(CommandExecutor { sender, args ->
                                    setRule(
                                        sender,
                                        key,
                                        getRuleSet(sender),
                                        getContext(sender),
                                        args
                                    )
                                })
                        )
                )
            }
        })
    }

    /**
     * Same as the [CommandTree] overload, but continues an existing argument
     * chain.  Example:
     *
     * ```
     * CommandTree("config")
     *   .then(StringArgument("category"))
     *   .let { gameRules.addToCommandTree(it, ::ruleSet, ::context) }
     * ```
     */
    fun addToCommandTree(
        argument: Argument<*>,
        getRuleSet: (CommandSender) -> RuleSet,
        getContext: (CommandSender) -> CTX,
        setValueArgumentName: String = "value",
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
                            type.createArgument(setValueArgumentName)
                                .executes(CommandExecutor { sender, args ->
                                    setRule(
                                        sender,
                                        key,
                                        getRuleSet(sender),
                                        getContext(sender),
                                        args
                                    )
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

    /**
     * Collects one concrete value for **every** [Key] known to its parent
     * [GameRules] and offers fast O(1) access via the generated index.
     *
     * A `RuleSet` is *mutable* and **thread-safe**; updates propagate change
     * callbacks immediately.
     *
     * ### Serialization
     * * [createTag] → NBT compound with `keyId → serializedValue` pairs.
     * * Constructor with [CompoundBinaryTag] or [Path] performs the inverse.
     */
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

        /**
         * Retrieves the [Value] instance backing the specified [key].  The
         * returned object can be used to read or modify the current setting in
         * a type-safe manner.
         */
        fun <T : Value<T, CTX, V>, V> getRule(key: Key<T, CTX, V>): T {
            return gameruleArray[key.gameRuleIndex] as T
        }

        /**
         * Shortcut for obtaining the primitive value of a rule without accessing
         * the full [Value] wrapper.
         */
        fun <T : Value<T, CTX, V>, V> getValue(key: Key<T, CTX, V>): V {
            return getRule(key).get()
        }

        /**
         * Operator alias for [getRule] allowing array-like access to values.
         */
        operator fun <T : Value<T, CTX, V>, V> get(key: Key<T, CTX, V>): T {
            return getRule(key)
        }

        /**
         * Serializes this rule-set into an NBT [CompoundBinaryTag].
         * Each rule is stored as a simple `key → serializedValue` pair.
         */
        fun createTag(): CompoundBinaryTag {
            val builder = CompoundBinaryTag.builder()
            for ((key, value) in rules.object2ObjectEntrySet()) {
                builder.putString(key.id, value.serialize())
            }
            return builder.build()
        }

        /**
         * Persists this rule-set to the specified [file] in compressed NBT
         * format.
         */
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

        /**
         * Invokes the provided [visitor] for every registered rule type in this
         * rule-set.
         */
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

        /**
         * Copies all values from [other] into this rule-set and triggers the
         * appropriate change callbacks for each modified rule.
         */
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

    /**
     * Immutable **descriptor** for a single rule.
     *
     * @property id            Unique, case-sensitive identifier.
     * @property description   Localisable description shown in command hovers.
     * @property gameRuleIndex Dense, zero-based index used for fast array
     *                         look-ups inside [RuleSet]; assigned automatically
     *                         on registration order.
     */
    data class Key<T : Value<T, CTX, V>, CTX : Any, V>(
        val id: String,
        val description: String,
        val gameRuleIndex: Int,
    )

    /**
     * Meta-information and factories common to all values of the same
     * **datatype**.
     *
     * A *type* knows how to:
     *
     * * Build the associated CommandAPI [Argument] (via [createArgument]).
     * * Instantiate the concrete [Value] implementation (via [createRule]).
     * * Call the registered visitor hook for reflection-like traversal.
     *
     * @param argumentCreator  DSL lambda used to construct the command
     *                         argument *on-demand* (name → Argument).
     * @param ruleFactory      Creates a fresh [Value] with its default value.
     * @param changeCallback   Fired immediately after a value is changed via
     *                         commands or programmatic calls.
     * @param visitorCaller    Bridges the generic visitor to the
     *                         type-specific overload (see
     *                         [GameRuleTypeVisitor]).
     */
    data class Type<T : Value<T, CTX, V>, CTX : Any, V>(
        private val argumentCreator: (name: String) -> Argument<*>,
        private val ruleFactory: (Type<T, CTX, V>) -> T,
        internal val changeCallback: (CTX, T) -> Unit,
        private val visitorCaller: VisitorCaller<T, CTX, V>,
    ) {

        /**
         * Builds the CommandAPI [Argument] used to set this rule's value via
         * commands.
         */
        fun createArgument(name: String): Argument<*> {
            return argumentCreator(name)
        }

        /**
         * Creates a new [Value] instance with its default value.
         */
        fun createRule(): T {
            return ruleFactory(this)
        }

        /**
         * Dispatches the [visitor] to the type-specific callback supplied at
         * construction time.
         */
        fun callVisitor(visitor: GameRuleTypeVisitor<CTX>, key: Key<T, CTX, V>) {
            visitorCaller.call(visitor, key, this)
        }
    }

    /**
     * Runtime instance holding the **current value** of a single rule.
     *
     * Implementations must be *thread-safe*; most subclasses achieve this via
     * `java.util.concurrent.atomic.*` fields.
     *
     * @param SELF  Self-type trick enabling covariant generics while allowing
     *              fluent APIs (`return this`).
     * @see BooleanValue
     * @see DoubleValue
     * @see DurationValue
     * @see IntegerValue
     * @see LongValue
     * @see StringValue
     */
    abstract class Value<SELF : Value<SELF, CTX, V>, CTX : Any, V>(protected val type: Type<SELF, CTX, V>) {
        /**
         * Parses the command argument identified by [name] and updates this
         * value accordingly. Implementations should extract the argument from
         * [args] and perform any necessary validation.
         */
        protected abstract fun updateFromArgument(
            sender: CommandSender,
            args: CommandArguments,
            name: String,
            key: Key<SELF, CTX, V>,
        )

        /** Returns the currently stored primitive value. */
        abstract fun get(): V

        /** Self-type helper used by [onChanged]. */
        protected abstract fun self(): SELF

        operator fun getValue(thisRef: Any?, property: KProperty<*>) = get()

        /**
         * Updates this value from the command argument named [name] and fires
         * the registered change callback.
         */
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

        /** Invokes the change callback with this value. */
        fun onChanged(context: CTX) {
            type.changeCallback(context, self())
        }

        /** Restores the state from its serialized representation. */
        abstract fun deserialize(value: String)

        /** Serializes the current state to a string representation. */
        abstract fun serialize(): String

        /**
         * Returns a user-friendly representation of the value. Defaults to
         * [serialize].
         */
        open fun displayValue(): String = serialize()

        override fun toString(): String {
            return serialize()
        }

        /** Creates a deep copy of this value object. */
        protected abstract fun copy(): SELF

        /** Assigns the state from [other] to this value. */
        abstract fun setFrom(other: SELF, context: CTX)
    }

    /**
     * Visitor interface that lets callers perform **type-safe bulk operations**
     * across all registered rule types without resorting to `is` checks or
     * unchecked casts.  Every rule is first passed to [visit] (generic), then
     * to its type-specific overload (e.g. [visitDuration]).
     */
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