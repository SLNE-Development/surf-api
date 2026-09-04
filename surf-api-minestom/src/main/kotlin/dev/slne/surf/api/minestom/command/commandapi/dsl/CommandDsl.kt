package dev.slne.surf.api.minestom.command.commandapi.dsl

import dev.slne.surf.api.minestom.command.commandapi.CommandAPICommand
import dev.slne.surf.api.minestom.command.commandapi.CommandTree

inline fun commandAPICommand(
    name: String,
    block: CommandAPICommand.() -> Unit = {},
) = CommandAPICommand(name).apply(block).register()

inline fun commandAPICommand(
    name: String,
    namespace: String,
    block: CommandAPICommand.() -> Unit = {},
) = CommandAPICommand(name).apply(block).register(namespace)

inline fun commandTree(
    name: String,
    block: CommandTree.() -> Unit = {},
) = CommandTree(name).apply(block).register()

inline fun commandTree(
    name: String,
    namespace: String,
    block: CommandTree.() -> Unit = {},
) = CommandTree(name).apply(block).register(namespace)

inline fun subcommand(
    name: String,
    block: CommandAPICommand.() -> Unit = {},
) = CommandAPICommand(name).apply(block)
