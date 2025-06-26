package dev.slne.surf.surfapi.bukkit.api.gamerules

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.Argument
import net.kyori.adventure.nbt.CompoundBinaryTag
import org.bukkit.command.CommandSender
import java.nio.file.Path

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

    fun <T : Value<T, CTX, V>, V> getRule(key: Key<T, CTX, V>): T {
        return ruleSet.getRule(key)
    }

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