package dev.slne.surf.api.minestom.command.commandapi.argument.parser

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minestom.server.MinecraftServer
import net.minestom.server.component.DataComponents
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.tag.Tag
import net.minestom.server.utils.Range
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadLocalRandom

/**
 * Reads a vanilla-style entity selector: `@a`, `@e`, `@p`, `@r` and `@s`, each with their implied
 * limit and player-only default, an optional bracketed `key=value` option list, or a bare player
 * name or UUID.
 *
 * [maxResults] is the ceiling this argument allows; a selector whose own limit (implied by its
 * type or overridden by the `limit` option) exceeds it is rejected at parse time rather than
 * silently truncated, mirroring vanilla's single-target `EntityArgument`. [playersOnly] rejects a
 * selector that could include a non-player entity, except a self selector (`@s`), which is always
 * accepted and then filtered normally.
 *
 * `nbt` is rejected outright: expressing it would require SNBT, which this port does not parse.
 * `scores`, `advancements` and `predicate` have their grammar consumed - so a malformed value is
 * still reported, and later options in the same bracket list still parse correctly - but are then
 * rejected too, since Minestom has no per-entity scoreboard objectives, advancement progress or
 * loot-condition registry to resolve them against. Every other listed option resolves against a
 * real Minestom equivalent.
 */
internal class EntitySelectorParser(
    private val maxResults: Int,
    private val playersOnly: Boolean,
) : ArgumentType<EntitySelector> {

    override fun parse(reader: StringReader): EntitySelector {
        val start = reader.cursor
        val state = MutableSelector()

        if (reader.canRead() && reader.peek() == '@') {
            reader.skip()
            parseSelectorType(reader, state)
        } else {
            parseNameOrUuid(reader, state)
        }
        finalizePredicates(state)

        val selector = state.toSelector(maxResults)
        if (selector.limit > maxResults) {
            reader.cursor = start
            throw (if (playersOnly) ERROR_NOT_SINGLE_PLAYER else ERROR_NOT_SINGLE_ENTITY).createWithContext(
                reader
            )
        }
        if (selector.includesEntities && playersOnly && !selector.self) {
            reader.cursor = start
            throw ERROR_ONLY_PLAYERS_ALLOWED.createWithContext(reader)
        }
        return selector
    }

    /**
     * Offers the names of the online players followed by the selectors this argument accepts.
     *
     * A selector whose implied limit exceeds [maxResults] is left out, as is `@e` on a players-only
     * argument, so every offered selector is one the parser would accept.
     */
    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val players = MinecraftServer.getConnectionManager().onlinePlayers
        val values = ObjectArrayList<String>(players.size + ENTITY_SELECTORS.size)

        players.mapTo(values, Player::getUsername)
        values.sortWith(String.CASE_INSENSITIVE_ORDER)

        val selectors = if (playersOnly) PLAYER_SELECTORS else ENTITY_SELECTORS
        selectors.forEach { selector ->
            if (maxResults > 1 || selector in SINGLE_SELECTORS) values += selector
        }

        return builder.suggestMatching(values)
    }

    private companion object {
        val SINGLE_SELECTORS: ObjectList<String> = ObjectList.of("@p", "@r", "@s")
        val PLAYER_SELECTORS: ObjectList<String> = ObjectList.of("@p", "@r", "@a", "@s")
        val ENTITY_SELECTORS: ObjectList<String> = ObjectList.of("@p", "@r", "@a", "@e", "@s")
    }
}

/** Mutable accumulator for the selector being built; frozen into an [EntitySelector] once parsed. */
private class MutableSelector {
    var limit: Int = 1
    var includesEntities: Boolean = false
    var self: Boolean = false
    var playerName: String? = null
    var uuid: UUID? = null
    var sorter: ((Pos) -> Comparator<Entity>)? = null
    var worldLimited: Boolean = false
    var x: Double? = null
    var y: Double? = null
    var z: Double? = null
    var dx: Double? = null
    var dy: Double? = null
    var dz: Double? = null
    var distance: Range.Float? = null
    val predicates: MutableList<(Pos, Entity) -> Boolean> = mutableListOf()

    /** Options that may appear at most once: `x y z dx dy dz distance limit sort level`. */
    val singleUseSeen: MutableSet<String> = mutableSetOf()
    private val invertableStates: MutableMap<String, InvertableState> = mutableMapOf()

    /** The `name`/`type`/`team`/`gamemode` tracking state for [key], created on first use. */
    fun invertableStateFor(key: String): InvertableState =
        invertableStates.getOrPut(key) { InvertableState() }

    fun toSelector(maxResults: Int): EntitySelector = EntitySelector(
        maxResults = maxResults,
        includesEntities = includesEntities,
        predicates = predicates.toList(),
        sorter = sorter,
        limit = limit,
        self = self,
        playerName = playerName,
        uuid = uuid,
        worldLimited = worldLimited,
    )
}

/**
 * Mirrors vanilla's `InvertableSetOptionState`: an option using this may be set once as a positive
 * value, or any number of times as a negative (`!`) value, but never mixed - a positive after a
 * negative, or any repeat after a positive, is rejected.
 */
private class InvertableState {
    private var limitation = Limitation.NONE

    fun canParseElement(inverted: Boolean): Boolean =
        if (inverted) limitation != Limitation.SINGLE else limitation == Limitation.NONE

    fun markParsedElement(inverted: Boolean) {
        limitation = if (inverted) Limitation.MULTIPLE else Limitation.SINGLE
    }

    private enum class Limitation { NONE, SINGLE, MULTIPLE }
}

private fun parseNameOrUuid(reader: StringReader, state: MutableSelector) {
    val start = reader.cursor
    val name = reader.readString()
    val uuid = runCatching { UUID.fromString(name) }.getOrNull()

    if (uuid != null) {
        state.uuid = uuid
        state.includesEntities = true
    } else {
        if (name.isEmpty() || name.length > 16) {
            reader.cursor = start
            throw ERROR_INVALID_NAME_OR_UUID.createWithContext(reader)
        }
        state.playerName = name
        state.includesEntities = false
    }
    state.limit = 1
}

private fun parseSelectorType(reader: StringReader, state: MutableSelector) {
    if (!reader.canRead()) throw ERROR_MISSING_SELECTOR_TYPE.createWithContext(reader)
    val typeStart = reader.cursor
    val type = reader.read()

    when (type) {
        'a' -> {
            state.limit = Int.MAX_VALUE
            state.includesEntities = false
        }

        'e' -> {
            state.limit = Int.MAX_VALUE
            state.includesEntities = true
        }

        'p' -> {
            state.limit = 1
            state.includesEntities = false
            state.sorter = ::nearestComparator
        }

        'r' -> {
            state.limit = 1
            state.includesEntities = false
            state.sorter = ::randomComparator
        }

        's' -> {
            state.limit = 1
            state.includesEntities = true
            state.self = true
        }

        else -> {
            reader.cursor = typeStart
            throw ERROR_UNKNOWN_SELECTOR_TYPE.createWithContext(reader, "@$type")
        }
    }

    if (reader.canRead() && reader.peek() == '[') {
        reader.skip()
        parseOptions(reader, state)
    }
}

private fun parseOptions(reader: StringReader, state: MutableSelector) {
    reader.skipWhitespace()
    while (reader.canRead() && reader.peek() != ']') {
        reader.skipWhitespace()
        val keyStart = reader.cursor
        val key = reader.readString()
        reader.skipWhitespace()
        if (!reader.canRead() || reader.peek() != '=') {
            reader.cursor = keyStart
            throw ERROR_EXPECTED_OPTION_VALUE.createWithContext(reader, key)
        }
        reader.skip()
        reader.skipWhitespace()
        applyOption(key, keyStart, reader, state)
        reader.skipWhitespace()
        if (reader.canRead()) {
            if (reader.peek() != ',') {
                if (reader.peek() != ']') throw ERROR_EXPECTED_END_OF_OPTIONS.createWithContext(
                    reader
                )
                break
            }
            reader.skip()
            reader.skipWhitespace()
        }
    }

    if (reader.canRead()) {
        reader.skip()
    } else {
        throw ERROR_EXPECTED_END_OF_OPTIONS.createWithContext(reader)
    }
}

private fun applyOption(key: String, keyStart: Int, reader: StringReader, state: MutableSelector) {
    val valueStart = reader.cursor
    when (key) {
        "x" -> {
            requireSingleUse(key, keyStart, reader, state)
            state.worldLimited = true
            state.x = reader.readDouble()
        }

        "y" -> {
            requireSingleUse(key, keyStart, reader, state)
            state.worldLimited = true
            state.y = reader.readDouble()
        }

        "z" -> {
            requireSingleUse(key, keyStart, reader, state)
            state.worldLimited = true
            state.z = reader.readDouble()
        }

        "dx" -> {
            requireSingleUse(key, keyStart, reader, state)
            state.worldLimited = true
            state.dx = reader.readDouble()
        }

        "dy" -> {
            requireSingleUse(key, keyStart, reader, state)
            state.worldLimited = true
            state.dy = reader.readDouble()
        }

        "dz" -> {
            requireSingleUse(key, keyStart, reader, state)
            state.worldLimited = true
            state.dz = reader.readDouble()
        }

        "distance" -> {
            requireSingleUse(key, keyStart, reader, state)
            state.worldLimited = true
            state.distance = FloatRangeParser.parse(reader)
        }

        "limit" -> {
            requireSingleUse(key, keyStart, reader, state, applicable = !state.self)
            val count = reader.readInt()
            if (count < 1) {
                reader.cursor = keyStart
                throw ERROR_LIMIT_TOO_SMALL.createWithContext(reader)
            }
            state.limit = count
        }

        "sort" -> {
            requireSingleUse(key, keyStart, reader, state, applicable = !state.self)
            val name = reader.readUnquotedString()
            state.sorter = when (name) {
                "nearest" -> ::nearestComparator
                "furthest" -> ::furthestComparator
                "random" -> ::randomComparator
                "arbitrary" -> null
                else -> {
                    reader.cursor = keyStart
                    throw ERROR_SORT_UNKNOWN.createWithContext(reader, name)
                }
            }
        }

        "name" -> {
            val inverted = shouldInvertValue(reader)
            requireInvertableUse(key, inverted, valueStart, reader, state)
            val name = reader.readString()
            state.predicates += { _, entity -> matchesName(entity, name) != inverted }
        }

        "type" -> {
            val inverted = shouldInvertValue(reader)
            requireInvertableUse(key, inverted, valueStart, reader, state)
            val typeStart = reader.cursor
            val key0 = ResourceLocationParser.readKey(reader)
            val type = EntityType.fromKey(key0) ?: run {
                reader.cursor = typeStart
                throw ERROR_ENTITY_TYPE_INVALID.createWithContext(reader, key0.asString())
            }
            if (type == EntityType.PLAYER && !inverted) {
                state.includesEntities = false
            }
            state.predicates += { _, entity -> (entity.entityType == type) != inverted }
        }

        "tag" -> {
            val inverted = shouldInvertValue(reader)
            val tag = reader.readUnquotedString()
            state.predicates += { _, entity ->
                // `?: emptyList()` guards an entity that never had ENTITY_TAGS written: the tag's
                // own default should cover this, but nothing here may rely on that to avoid an NPE
                // inside a predicate.
                val tags = entity.getTag(ENTITY_TAGS) ?: emptyList()
                (if (tag.isEmpty()) tags.isEmpty() else tags.contains(tag)) != inverted
            }
        }

        "team" -> {
            val inverted = shouldInvertValue(reader)
            requireInvertableUse(key, inverted, valueStart, reader, state)
            val team = reader.readUnquotedString()
            state.predicates += { _, entity -> (teamNameOf(entity) == team) != inverted }
        }

        "gamemode" -> {
            val inverted = shouldInvertValue(reader)
            requireInvertableUse(key, inverted, valueStart, reader, state)
            val mode = GAME_MODE_PARSER.parse(reader)
            state.includesEntities = false
            state.predicates += { _, entity -> (entity is Player && entity.gameMode == mode) != inverted }
        }

        "level" -> {
            requireSingleUse(key, keyStart, reader, state)
            val range = IntegerRangeParser.parse(reader)
            state.includesEntities = false
            state.predicates += { _, entity -> entity is Player && range.inRange(entity.level) }
        }

        "scores" -> {
            consumeScores(reader)
            reader.cursor = keyStart
            throw ERROR_UNSUPPORTED_OPTION.createWithContext(reader, key)
        }

        "advancements" -> {
            consumeAdvancements(reader)
            reader.cursor = keyStart
            throw ERROR_UNSUPPORTED_OPTION.createWithContext(reader, key)
        }

        "predicate" -> {
            shouldInvertValue(reader)
            ResourceLocationParser.readKey(reader)
            reader.cursor = keyStart
            throw ERROR_UNSUPPORTED_OPTION.createWithContext(reader, key)
        }

        "nbt" -> {
            reader.cursor = keyStart
            throw ERROR_NBT_UNSUPPORTED.createWithContext(reader)
        }

        else -> {
            reader.cursor = keyStart
            throw ERROR_UNKNOWN_OPTION.createWithContext(reader, key)
        }
    }
}

/**
 * Rejects a repeated use of a single-use option (`x y z dx dy dz distance limit sort level`), and,
 * for `limit`/`sort`, a use on a self selector (`@s`) at all - both mirror vanilla's per-option
 * `canUse` gate, checked before the value is read so a repeat never even attempts to parse it.
 */
private fun requireSingleUse(
    key: String,
    keyStart: Int,
    reader: StringReader,
    state: MutableSelector,
    applicable: Boolean = true,
) {
    if (!applicable || !state.singleUseSeen.add(key)) {
        reader.cursor = keyStart
        throw ERROR_INAPPLICABLE_OPTION.createWithContext(reader, key)
    }
}

/**
 * Rejects a `name`/`type`/`team`/`gamemode` use that would mix a positive value with any other
 * value, or repeat a positive value - see [InvertableState]. Vanilla checks this at different
 * points relative to reading the value depending on the option; since a rejection here always
 * aborts the whole selector, checking immediately (before the value is read) is behaviorally
 * identical for every caller and simpler to keep consistent across the four options.
 */
private fun requireInvertableUse(
    key: String,
    inverted: Boolean,
    valueStart: Int,
    reader: StringReader,
    state: MutableSelector,
) {
    val invertableState = state.invertableStateFor(key)
    if (!invertableState.canParseElement(inverted)) {
        reader.cursor = valueStart
        throw ERROR_INAPPLICABLE_OPTION.createWithContext(reader, key)
    }
    invertableState.markParsedElement(inverted)
}

/**
 * Adds the volume-box and distance predicates once every option has been read, so both see the
 * final `x`/`y`/`z`/`dx`/`dy`/`dz`/`distance` values regardless of the order they appeared in.
 */
private fun finalizePredicates(state: MutableSelector) {
    if (state.dx != null || state.dy != null || state.dz != null) {
        state.predicates += { anchor, entity -> matchesVolume(anchor, state, entity) }
    }
    if (state.distance != null) {
        state.predicates += { anchor, entity -> matchesDistance(anchor, state, entity) }
    }
}

/** The anchor point: the sender's position with any explicit `x`/`y`/`z` substituted per axis. */
private fun anchorOf(basePos: Pos, state: MutableSelector): Pos =
    Pos(state.x ?: basePos.x(), state.y ?: basePos.y(), state.z ?: basePos.z())

/**
 * Matches entities within the box anchored at [anchor]'s origin corner, extended by `dx`/`dy`/`dz`
 * on whichever side is positive. Each axis's non-negative side is extended by one extra block
 * (mirroring vanilla's `EntitySelectorParser.createAabb`), so `dx=0,dy=0,dz=0` selects the single
 * block the origin sits in rather than a degenerate zero-width box.
 */
private fun matchesVolume(anchor: Pos, state: MutableSelector, entity: Entity): Boolean {
    val origin = anchorOf(anchor, state)
    val dx = state.dx ?: 0.0
    val dy = state.dy ?: 0.0
    val dz = state.dz ?: 0.0
    val pos = entity.position

    return pos.x() >= origin.x() + minOf(0.0, dx) && pos.x() <= origin.x() + maxOf(0.0, dx) + 1.0 &&
            pos.y() >= origin.y() + minOf(0.0, dy) && pos.y() <= origin.y() + maxOf(
        0.0,
        dy
    ) + 1.0 &&
            pos.z() >= origin.z() + minOf(0.0, dz) && pos.z() <= origin.z() + maxOf(0.0, dz) + 1.0
}

private fun matchesDistance(anchor: Pos, state: MutableSelector, entity: Entity): Boolean {
    val range = state.distance ?: return true
    val origin = anchorOf(anchor, state)
    return range.inRange(entity.getDistance(origin).toFloat())
}

private fun matchesName(entity: Entity, name: String): Boolean = when (entity) {
    is Player -> entity.username == name
    else -> entity.get(DataComponents.CUSTOM_NAME)
        ?.let { component -> PlainTextComponentSerializer.plainText().serialize(component) } == name
}

/** The name of the team `entity` belongs to, or an empty string if it belongs to none. */
private fun teamNameOf(entity: Entity): String {
    val identifier = if (entity is Player) entity.username else entity.uuid.toString()
    return MinecraftServer.getTeamManager().teams
        .firstOrNull { team -> identifier in team.members }
        ?.teamName ?: ""
}

private fun shouldInvertValue(reader: StringReader): Boolean {
    reader.skipWhitespace()
    if (reader.canRead() && reader.peek() == '!') {
        reader.skip()
        reader.skipWhitespace()
        return true
    }
    return false
}

/** Consumes a `{name=range,...}` value without keeping it: `scores` has no backing store here. */
private fun consumeScores(reader: StringReader) {
    reader.expect('{')
    reader.skipWhitespace()
    while (reader.canRead() && reader.peek() != '}') {
        reader.skipWhitespace()
        reader.readUnquotedString()
        reader.skipWhitespace()
        reader.expect('=')
        reader.skipWhitespace()
        IntegerRangeParser.parse(reader)
        reader.skipWhitespace()
        if (reader.canRead() && reader.peek() == ',') reader.skip()
        reader.skipWhitespace()
    }
    reader.expect('}')
}

/** Consumes a `{id=bool}` / `{id={crit=bool,...}}` value without keeping it: same reasoning as [consumeScores]. */
private fun consumeAdvancements(reader: StringReader) {
    reader.expect('{')
    reader.skipWhitespace()
    while (reader.canRead() && reader.peek() != '}') {
        reader.skipWhitespace()
        ResourceLocationParser.readKey(reader)
        reader.skipWhitespace()
        reader.expect('=')
        reader.skipWhitespace()
        if (reader.canRead() && reader.peek() == '{') {
            reader.skip()
            reader.skipWhitespace()
            while (reader.canRead() && reader.peek() != '}') {
                reader.skipWhitespace()
                reader.readUnquotedString()
                reader.skipWhitespace()
                reader.expect('=')
                reader.skipWhitespace()
                reader.readBoolean()
                reader.skipWhitespace()
                if (reader.canRead() && reader.peek() == ',') reader.skip()
                reader.skipWhitespace()
            }
            reader.expect('}')
        } else {
            reader.readBoolean()
        }
        reader.skipWhitespace()
        if (reader.canRead() && reader.peek() == ',') reader.skip()
        reader.skipWhitespace()
    }
    reader.expect('}')
}

private fun nearestComparator(anchor: Pos): Comparator<Entity> =
    Comparator.comparingDouble { entity: Entity -> entity.getDistanceSquared(anchor) }

private fun furthestComparator(anchor: Pos): Comparator<Entity> =
    nearestComparator(anchor).reversed()

/** A stable-per-invocation random order: each entity gets one random key, reused across comparisons. */
private fun randomComparator(@Suppress("UNUSED_PARAMETER") anchor: Pos): Comparator<Entity> {
    val weights = IdentityHashMap<Entity, Double>()
    val random = ThreadLocalRandom.current()
    return Comparator.comparingDouble { entity -> weights.getOrPut(entity) { random.nextDouble() } }
}

private val GAME_MODE_PARSER =
    FixedSetParser(GameMode.entries.associateBy { mode -> mode.name.lowercase() })

/** Backing store for the `tag` option; nothing writes to it yet, so it is always empty today. */
private val ENTITY_TAGS: Tag<List<String>> = Tag.String("tags").list().defaultValue(emptyList())

private val ERROR_MISSING_SELECTOR_TYPE =
    SimpleCommandExceptionType(LiteralMessage("Missing selector type"))
private val ERROR_UNKNOWN_SELECTOR_TYPE =
    DynamicCommandExceptionType { type -> LiteralMessage("Unknown selector type '$type'") }
private val ERROR_INVALID_NAME_OR_UUID =
    SimpleCommandExceptionType(LiteralMessage("Invalid player name or UUID"))
private val ERROR_EXPECTED_OPTION_VALUE =
    DynamicCommandExceptionType { key -> LiteralMessage("Expected value for option '$key'") }
private val ERROR_EXPECTED_END_OF_OPTIONS =
    SimpleCommandExceptionType(LiteralMessage("Expected ',' or ']'"))
private val ERROR_UNKNOWN_OPTION =
    DynamicCommandExceptionType { key -> LiteralMessage("Unknown option '$key'") }
private val ERROR_INAPPLICABLE_OPTION =
    DynamicCommandExceptionType { key -> LiteralMessage("Option '$key' cannot be used here") }
private val ERROR_UNSUPPORTED_OPTION = DynamicCommandExceptionType { key ->
    LiteralMessage("Option '$key' is unsupported: this command implementation has no equivalent in Minestom's model")
}
private val ERROR_NBT_UNSUPPORTED = SimpleCommandExceptionType(
    LiteralMessage("Option 'nbt' is unsupported: this command implementation cannot parse SNBT"),
)
private val ERROR_LIMIT_TOO_SMALL =
    SimpleCommandExceptionType(LiteralMessage("Limit must be at least 1"))
private val ERROR_SORT_UNKNOWN =
    DynamicCommandExceptionType { name -> LiteralMessage("Unknown sort '$name'") }
private val ERROR_ENTITY_TYPE_INVALID =
    DynamicCommandExceptionType { type -> LiteralMessage("Unknown entity type '$type'") }
private val ERROR_NOT_SINGLE_ENTITY =
    SimpleCommandExceptionType(LiteralMessage("Expected a single entity"))
private val ERROR_NOT_SINGLE_PLAYER =
    SimpleCommandExceptionType(LiteralMessage("Expected a single player"))
private val ERROR_ONLY_PLAYERS_ALLOWED =
    SimpleCommandExceptionType(LiteralMessage("Only players can be targeted"))
