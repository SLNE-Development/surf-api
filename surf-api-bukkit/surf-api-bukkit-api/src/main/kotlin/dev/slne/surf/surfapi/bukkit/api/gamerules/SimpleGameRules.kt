package dev.slne.surf.surfapi.bukkit.api.gamerules

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.Argument
import net.kyori.adventure.nbt.CompoundBinaryTag
import org.bukkit.command.CommandSender
import java.nio.file.Path

/**
 * Convenience wrapper for the common case where a plugin needs **one global
 * rule-set** (or one per world) and does **not** support dynamic registries.
 *
 * The first access to [ruleSet] initialises the backing instance by delegating
 * to the protected factory methods in [GameRules].  Serialization
 * constructors are provided for loading from NBT tags or files.
 *
 * @see GameRules for the full registry/command API.
 */
abstract class SimpleGameRules<CTX : Any> : GameRules<CTX> {
    private val ruleSetLazy: Lazy<RuleSet>
    val ruleSet get() = ruleSetLazy.value

    constructor() {
        ruleSetLazy = lazy { createRuleSet() }
    }

    constructor(tag: CompoundBinaryTag) {
        ruleSetLazy = lazy { createRuleSet(tag) }
    }

    constructor(file: Path) {
        ruleSetLazy = lazy { loadRuleSet(file) }
    }

    /** Returns the strongly-typed value object for the given [key]. */
    fun <T : Value<T, CTX, V>, V> getRule(key: Key<T, CTX, V>): T {
        return ruleSet.getRule(key)
    }

    /** Shorthand for `getRule(key).get()`. */
    fun <T : Value<T, CTX, V>, V> getValue(key: Key<T, CTX, V>): V {
        return ruleSet.getValue(key)
    }

    operator fun <T : Value<T, CTX, V>, V> get(key: Key<T, CTX, V>): T {
        return ruleSet[key]
    }

    fun addToCommandTree(
        tree: CommandTree,
        getContext: (CommandSender) -> CTX,
    ) {
        addToCommandTree(tree, { ruleSet }, getContext)
    }

    fun addToCommandTree(
        argument: Argument<*>,
        getContext: (CommandSender) -> CTX,
    ) {
        addToCommandTree(argument, { ruleSet }, getContext)
    }
}