/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command.commandapi.argument

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.slne.surf.api.minestom.command.commandapi.argument.parser.EntitySelectorParser
import dev.slne.surf.api.minestom.command.commandapi.argument.parser.RegistryParser
import net.minestom.server.command.CommandSender
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import java.util.concurrent.CompletableFuture

class PlayerArgument(nodeName: String) : Argument<Player>(nodeName) {
    override val kind = ArgumentKind.Player
    override val rawType: ArgumentType<Player> =
        SingleTargetType(maxResults = 1, playersOnly = true) { entities -> entities.filterIsInstance<Player>().singleOrNull() }

    override fun stringify(value: Player): String = value.username
}

class PlayersArgument(
    nodeName: String,
    val allowEmpty: Boolean = false,
) : Argument<List<Player>>(nodeName) {
    override val kind = ArgumentKind.Players(allowEmpty)
    override val rawType: ArgumentType<List<Player>> = MultiTargetType(
        maxResults = Int.MAX_VALUE,
        playersOnly = true,
        allowEmpty = allowEmpty,
    ) { entities -> entities.filterIsInstance<Player>() }

    override fun stringify(value: List<Player>): String =
        value.joinToString(",", transform = Player::getUsername)
}

class EntityArgument(nodeName: String) : Argument<Entity>(nodeName) {
    override val kind = ArgumentKind.Entity
    override val rawType: ArgumentType<Entity> =
        SingleTargetType(maxResults = 1, playersOnly = false) { entities -> entities.singleOrNull() }

    override fun stringify(value: Entity): String = value.uuid.toString()
}

class EntitiesArgument(
    nodeName: String,
    val allowEmpty: Boolean = false,
) : Argument<List<Entity>>(nodeName) {
    override val kind = ArgumentKind.Entities(allowEmpty)
    override val rawType: ArgumentType<List<Entity>> = MultiTargetType(
        maxResults = Int.MAX_VALUE,
        playersOnly = false,
        allowEmpty = allowEmpty,
    ) { entities -> entities }

    override fun stringify(value: List<Entity>): String =
        value.joinToString(",") { entity -> entity.uuid.toString() }
}

class EntityTypeArgument(nodeName: String) : Argument<EntityType>(nodeName) {
    override val kind = ArgumentKind.EntityType
    override val rawType: ArgumentType<EntityType> =
        RegistryParser("entity type") { key -> EntityType.fromKey(key) }

    override fun stringify(value: EntityType): String = value.key().asString()
}

private val SELECTOR_NEEDS_SENDER = SimpleCommandExceptionType(
    LiteralMessage("An entity selector requires a command source to resolve"),
)
private val NO_MATCH = SimpleCommandExceptionType(LiteralMessage("No entity matched the selector"))
private val NO_MATCHES = SimpleCommandExceptionType(LiteralMessage("No entities matched the selector"))

/**
 * Resolves an entity selector to exactly one entity of type [T], rejecting the input (rather than
 * resolving it) when the selector could structurally match more than one, or when the selector's
 * source is unavailable to resolve against.
 */
private class SingleTargetType<T : Entity>(
    maxResults: Int,
    playersOnly: Boolean,
    private val select: (List<Entity>) -> T?,
) : ArgumentType<T> {
    private val selector = EntitySelectorParser(maxResults, playersOnly)

    override fun parse(reader: StringReader): T = throw SELECTOR_NEEDS_SENDER.createWithContext(reader)

    override fun <S> parse(reader: StringReader, source: S): T {
        val start = reader.cursor
        val parsed = selector.parse(reader)
        val sender = source as? CommandSender ?: run {
            reader.cursor = start
            throw SELECTOR_NEEDS_SENDER.createWithContext(reader)
        }

        return select(parsed.find(sender)) ?: run {
            reader.cursor = start
            throw NO_MATCH.createWithContext(reader)
        }
    }

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> = selector.listSuggestions(context, builder)
}

/**
 * Resolves an entity selector to every matching entity of type [T], rejecting an empty result
 * unless [allowEmpty] permits it.
 */
private class MultiTargetType<T : Entity>(
    maxResults: Int,
    playersOnly: Boolean,
    private val allowEmpty: Boolean,
    private val select: (List<Entity>) -> List<T>,
) : ArgumentType<List<T>> {
    private val selector = EntitySelectorParser(maxResults, playersOnly)

    override fun parse(reader: StringReader): List<T> = throw SELECTOR_NEEDS_SENDER.createWithContext(reader)

    override fun <S> parse(reader: StringReader, source: S): List<T> {
        val start = reader.cursor
        val parsed = selector.parse(reader)
        val sender = source as? CommandSender ?: run {
            reader.cursor = start
            throw SELECTOR_NEEDS_SENDER.createWithContext(reader)
        }

        val results = select(parsed.find(sender))
        if (results.isEmpty() && !allowEmpty) {
            reader.cursor = start
            throw NO_MATCHES.createWithContext(reader)
        }
        return results
    }

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> = selector.listSuggestions(context, builder)
}
