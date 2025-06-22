package dev.slne.surf.surfapi.bukkit.api.gamerules

import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.executors.CommandArguments
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import dev.slne.surf.surfapi.core.api.util.synchronize
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap
import net.querz.nbt.tag.CompoundTag
import org.bukkit.command.CommandSender
import java.util.concurrent.atomic.AtomicInteger

abstract class GameRules<CTX : Any>() {

    private val lastGameRuleIndex = AtomicInteger(0)
    protected val gameRuleTypes =
        Object2ObjectRBTreeMap<Key<*, CTX>, Type<*, CTX>>(compareBy { it.id }).synchronize()

    protected fun register(name: String, description: String, type: Type<*, CTX>): Key<*, CTX> {
        val key = createKey(name, description)
        require(
            gameRuleTypes.putIfAbsent(
                key,
                type
            ) == null
        ) { "Game rule with id '${key.id}' already exists." }
        return key
    }

    private fun <T : Value<T, CTX>> createKey(id: String, description: String): Key<T, CTX> {
        return Key(id, description, lastGameRuleIndex.getAndIncrement())
    }

    inner class RuleSet(rules: Object2ObjectMap<Key<*, CTX>, Value<*, CTX>>) {
        private val rules: Object2ObjectMap<Key<*, CTX>, Value<*, CTX>> = rules.synchronize()

        private val gameruleArray = arrayOfNulls<Value<*, CTX>>(lastGameRuleIndex.get()).apply {
            for ((key, value) in rules.object2ObjectEntrySet()) {
                this[key.gameRuleIndex] = value
            }
        }.requireNoNulls()

        constructor(values: CompoundTag) : this() {
            loadFromTag(values)
        }

        constructor() : this(
            gameRuleTypes.object2ObjectEntrySet()
            .associateTo(mutableObject2ObjectMapOf()) { it.key to it.value.createRule() }
            .freeze()
        )

        fun <T: Value<T, CTX>> getRule(key: Key<T, CTX>): T {
            return gameruleArray[key.gameRuleIndex] as T
        }

        fun createTag(): CompoundTag {
            val tag = CompoundTag()
            for ((key, value) in rules.object2ObjectEntrySet()) {
                tag.putString(key.id, value.serialize())
            }
            return tag
        }

        private fun loadFromTag(tag: CompoundTag) {
            for ((key, value) in rules.object2ObjectEntrySet()) {
                val serialized = tag.getString(key.id)
                value.deserialize(serialized)
            }
        }

        fun visitGameRuleTypes(visitor: GameRuleTypeVisitor<CTX>) {
            for ((key, value) in gameRuleTypes.object2ObjectEntrySet()) {

            }
        }

        private fun <T: Value<T, CTX>> callVisitorCap(visitor: GameRuleTypeVisitor<CTX>, key: Key<*, CTX>, type: Type<*, CTX>) {
            visitor.visit(key as Key<T, CTX>, type as Type<T, CTX>)
            type.callVisitor(visitor, key)
        }
    }

    data class Key<T : Value<T, CTX>, CTX : Any>(
        val id: String,
        val description: String,
        val gameRuleIndex: Int,
    )

    @ConsistentCopyVisibility
    data class Type<T : Value<T, CTX>, CTX : Any> private constructor(
        private val argumentCreator: (name: String) -> Argument<*>,
        private val ruleFactory: (Type<T, CTX>) -> T,
        internal val changeCallback: (CTX, T) -> Unit,
        private val visitorCaller: VisitorCaller<T, CTX>,
    ) {

        fun createArgument(name: String): Argument<*> {
            return argumentCreator(name)
        }

        fun createRule(): T {
            return ruleFactory(this)
        }

        fun callVisitor(visitor: GameRuleTypeVisitor<CTX>, key: Key<T>) {
            visitorCaller.call(visitor, key, this)
        }
    }

    abstract class Value<SELF : Value<SELF, CTX>, CTX : Any>(protected val type: Type<SELF, CTX>) {
        protected abstract fun updateFromArgument(
            sender: CommandSender,
            args: CommandArguments,
            name: String,
            key: Key<SELF>,
        )

        protected abstract fun self(): SELF

        fun setFromArgument(
            sender: CommandSender,
            args: CommandArguments,
            name: String,
            key: Key<SELF>,
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

        override fun toString(): String {
            return serialize()
        }

        protected abstract fun copy(): SELF
        abstract fun setFrom(other: SELF, context: CTX): SELF
    }

    interface GameRuleTypeVisitor<CTX : Any> {
        fun <T : Value<T>> visit(key: Key<T>, type: Type<T, CTX>) = Unit
    }

    private interface VisitorCaller<T : Value<T>, CTX : Any> {
        fun call(visitor: GameRuleTypeVisitor<CTX>, key: Key<T>, type: Type<T, CTX>)
    }
}