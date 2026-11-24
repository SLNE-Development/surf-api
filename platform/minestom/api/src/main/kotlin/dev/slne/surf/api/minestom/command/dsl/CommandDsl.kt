package dev.slne.surf.api.minestom.command.dsl

import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.CommandTree
import dev.slne.surf.api.minestom.command.RegisteredCommand

inline fun commandAPICommand(
    name: String,
    block: CommandAPICommand.() -> Unit = {},
): RegisteredCommand = CommandAPICommand(name).apply(block).register()

inline fun commandAPICommand(
    name: String,
    namespace: String,
    block: CommandAPICommand.() -> Unit = {},
): RegisteredCommand = CommandAPICommand(name).apply(block).register(namespace)

inline fun commandTree(
    name: String,
    block: CommandTree.() -> Unit = {},
): RegisteredCommand = CommandTree(name).apply(block).register()

inline fun commandTree(
    name: String,
    namespace: String,
    block: CommandTree.() -> Unit = {},
): RegisteredCommand = CommandTree(name).apply(block).register(namespace)

inline fun subcommand(
    name: String,
    block: CommandAPICommand.() -> Unit = {},
): CommandAPICommand = CommandAPICommand(name).apply(block)
