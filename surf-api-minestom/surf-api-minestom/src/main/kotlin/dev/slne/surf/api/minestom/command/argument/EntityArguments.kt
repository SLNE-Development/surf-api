/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command.argument

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.slne.surf.api.minestom.command.argument.parser.EntitySelectorParser
import dev.slne.surf.api.minestom.command.argument.parser.RegistryParser
import net.minestom.server.command.CommandSender
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * A single online player, given by name or by a selector.
 *
 * Selectors that can match more than one player, such as `@a`, fail at parse time. A selector that
 * matches no player fails with `No entity matched the selector`. Parsing requires a command source.
 */
class PlayerArgument(nodeName: String) : Argument<Player>(nodeName) {
    override val kind = ArgumentKind.Player
    override val rawType: ArgumentType<Player> =
        SingleTargetType(maxResults = 1, playersOnly = true) { entities -> entities.filterIsInstance<Player>().singleOrNull() }

    override fun stringify(value: Player): String = value.username
}

/**
 * The online players matched by a player name or selector.
 *
 * Selectors that can match non-player entities, such as `@e`, fail at parse time. Without
 * [allowEmpty], a selector that matches no player fails with `No entities matched the selector`;
 * with it, the result is an empty list. Parsing requires a command source.
 */
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

/**
 * A single entity, given by player name, UUID or selector.
 *
 * Selectors that can match more than one entity, such as `@e`, fail at parse time. A selector that
 * matches no entity fails with `No entity matched the selector`. Parsing requires a command
 * source.
 */
class EntityArgument(nodeName: String) : Argument<Entity>(nodeName) {
    override val kind = ArgumentKind.Entity
    override val rawType: ArgumentType<Entity> =
        SingleTargetType(maxResults = 1, playersOnly = false) { entities -> entities.singleOrNull() }

    override fun stringify(value: Entity): String = value.uuid.toString()
}

/**
 * The entities matched by a player name, UUID or selector.
 *
 * Without [allowEmpty], a selector that matches no entity fails with
 * `No entities matched the selector`; with it, the result is an empty list. Parsing requires a
 * command source.
 */
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

/**
 * An entity type given by its namespaced key; the namespace defaults to `minecraft`.
 */
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
 * Parses an entity selector and resolves the matched entities to a value of type [R].
 *
 * Fails when [resolve] returns `null`, reporting [noMatch], and when no [CommandSender] source is
 * available.
 */
private abstract class TargetType<R : Any>(
    maxResults: Int,
    playersOnly: Boolean,
    private val noMatch: SimpleCommandExceptionType,
) : ArgumentType<R> {
    private val selector = EntitySelectorParser(maxResults, playersOnly)

    /**
     * Converts the entities the selector matched to the parsed value, or `null` when there is no
     * acceptable match.
     */
    protected abstract fun resolve(entities: List<Entity>): R?

    override fun parse(reader: StringReader): R = throw SELECTOR_NEEDS_SENDER.createWithContext(reader)

    override fun <S> parse(reader: StringReader, source: S): R {
        val start = reader.cursor
        val parsed = selector.parse(reader)
        val sender = source as? CommandSender ?: run {
            reader.cursor = start
            throw SELECTOR_NEEDS_SENDER.createWithContext(reader)
        }

        return resolve(parsed.find(sender)) ?: run {
            reader.cursor = start
            throw noMatch.createWithContext(reader)
        }
    }

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> = selector.listSuggestions(context, builder)
}

/**
 * Resolves an entity selector to a single entity of type [T].
 *
 * Fails when the selector can match more than [maxResults] entities or when [select] returns
 * `null`.
 */
private class SingleTargetType<T : Entity>(
    maxResults: Int,
    playersOnly: Boolean,
    private val select: (List<Entity>) -> T?,
) : TargetType<T>(maxResults, playersOnly, NO_MATCH) {
    override fun resolve(entities: List<Entity>): T? = select(entities)
}

/**
 * Resolves an entity selector to the matching entities of type [T].
 *
 * Fails on an empty result unless [allowEmpty] is set.
 */
private class MultiTargetType<T : Entity>(
    maxResults: Int,
    playersOnly: Boolean,
    private val allowEmpty: Boolean,
    private val select: (List<Entity>) -> List<T>,
) : TargetType<List<T>>(maxResults, playersOnly, NO_MATCHES) {
    override fun resolve(entities: List<Entity>): List<T>? =
        select(entities).takeIf { results -> allowEmpty || results.isNotEmpty() }
}
