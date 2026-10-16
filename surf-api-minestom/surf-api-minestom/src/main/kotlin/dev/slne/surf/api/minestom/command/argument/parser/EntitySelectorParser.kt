package dev.slne.surf.api.minestom.command.argument.parser

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
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minestom.server.MinecraftServer
import net.minestom.server.component.DataComponents
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.*
import net.minestom.server.tag.Tag
import net.minestom.server.utils.Range
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadLocalRandom

/**
 * Parses a vanilla entity selector: `@a`, `@e`, `@n`, `@p`, `@r` or `@s` with an optional
 * `[key=value,...]` option list, or a bare player name or UUID.
 *
 * A selector whose limit exceeds [maxResults] fails at parse time instead of being truncated. The
 * limit is implied by the selector type or set with the `limit` option. With [playersOnly], a
 * selector that can match non-player entities fails, except `@s`. `@e` and `@n` skip dead living
 * entities.
 *
 * Supported options are `x`, `y`, `z`, `dx`, `dy`, `dz`, `distance`, `limit`, `sort`, `name`,
 * `type` (including `#tag`), `tag`, `team`, `gamemode`, `level`, `x_rotation` and `y_rotation`.
 * `nbt` is rejected because SNBT is not parsed. `scores`, `advancements` and `predicate` are
 * rejected because Minestom has no matching data source; their value is read first, so a
 * malformed value is reported before the unsupported option.
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
     * Suggests the names of the online players, then the selectors this argument accepts.
     *
     * Selectors whose implied limit exceeds [maxResults] are omitted, as is `@e` when [playersOnly].
     */
    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val players = MinecraftServer.getConnectionManager().onlinePlayers
        val values = ObjectArrayList<String>(ENTITY_SELECTORS.size)

        val prefix = builder.remaining
        players.forEach { player ->
            val username = player.username
            if (username.startsWith(prefix, ignoreCase = true)) values += username
        }
        values.sortWith(String.CASE_INSENSITIVE_ORDER)

        val selectors = if (playersOnly) PLAYER_SELECTORS else ENTITY_SELECTORS
        selectors.forEach { selector ->
            if (maxResults > 1 || selector in SINGLE_SELECTORS) values += selector
        }

        return builder.suggestMatching(values)
    }

    private companion object {
        val SINGLE_SELECTORS: ObjectList<String> = ObjectList.of("@n", "@p", "@r", "@s")
        val PLAYER_SELECTORS: ObjectList<String> = ObjectList.of("@p", "@r", "@a", "@s")
        val ENTITY_SELECTORS: ObjectList<String> = ObjectList.of("@p", "@r", "@a", "@e", "@n", "@s")
    }
}

/**
 * Mutable state of a selector while it is parsed; [toSelector] converts it into an [EntitySelector].
 */
private class MutableSelector {
    var limit: Int = 1
    var includesEntities: Boolean = false
    var self: Boolean = false
    var playerName: String? = null
    var uuid: UUID? = null
    var sorter: ((Pos) -> Comparator<Entity>)? = null
    var worldLimited: Boolean = false

    /**
     * Values of the `x`, `y`, `z`, `dx`, `dy` and `dz` options by option name.
     */
    val coordinates: MutableMap<String, Double> = mutableMapOf()
    var distance: Range.Float? = null
    val predicates: MutableList<(Pos, Entity) -> Boolean> = mutableListOf()

    /**
     * Options already used among those allowed at most once:
     * `x y z dx dy dz distance limit sort level x_rotation y_rotation`.
     */
    val singleUseSeen: MutableSet<String> = mutableSetOf()
    private val invertableStates: MutableMap<String, InvertableState> = mutableMapOf()

    /**
     * Returns the usage state of the invertable option [key], created on first use.
     */
    fun invertableStateFor(key: String): InvertableState =
        invertableStates.getOrPut(key) { InvertableState() }

    fun toSelector(maxResults: Int): EntitySelector = EntitySelector(
        maxResults = maxResults,
        includesEntities = includesEntities,
        predicates = predicates.toList(),
        sorter = sorter,
        limit = limit,
        position = originOf(coordinates),
        self = self,
        playerName = playerName,
        uuid = uuid,
        worldLimited = worldLimited,
    )
}

/**
 * Tracks the use of an option that accepts negated (`!`) values, mirroring vanilla's
 * `InvertableSetOptionState`.
 *
 * Such an option may be given once with a positive value or any number of times with negated
 * values. Mixing the two, or repeating a positive value, is not allowed. A tag counts as a negated
 * value for this rule and may be given once.
 */
private class InvertableState {
    private var limitation = Limitation.NONE
    private val tags: MutableSet<Key> = mutableSetOf()

    fun canParseElement(inverted: Boolean): Boolean =
        if (inverted) limitation != Limitation.SINGLE else limitation == Limitation.NONE

    fun markParsedElement(inverted: Boolean) {
        limitation = if (inverted) Limitation.MULTIPLE else Limitation.SINGLE
    }

    fun canParseTag(tag: Key): Boolean = limitation != Limitation.SINGLE && tag !in tags

    fun markParsedTag(tag: Key) {
        limitation = Limitation.MULTIPLE
        tags += tag
    }

    private enum class Limitation { NONE, SINGLE, MULTIPLE }
}

private fun parseNameOrUuid(reader: StringReader, state: MutableSelector) {
    val start = reader.cursor
    val name = reader.readString()
    val uuid = parseDashedUuid(name)

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
            state.predicates += ::isAlive
        }

        'n' -> {
            state.limit = 1
            state.includesEntities = true
            state.sorter = ::nearestComparator
            state.predicates += ::isAlive
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
        "x", "y", "z", "dx", "dy", "dz" -> {
            requireSingleUse(key, keyStart, reader, state)
            state.worldLimited = true
            state.coordinates[key] = reader.readDouble()
        }

        "distance" -> {
            requireSingleUse(key, keyStart, reader, state)
            val range = FloatRangeParser.parse(reader)
            if (range.hasNegativeBound()) {
                reader.cursor = valueStart
                throw ERROR_DISTANCE_NEGATIVE.createWithContext(reader)
            }
            state.worldLimited = true
            state.distance = range
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
            if (consumeTagMarker(reader)) {
                applyTypeTag(inverted, valueStart, reader, state)
                return
            }
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
            if (range.hasNegativeBound()) {
                reader.cursor = valueStart
                throw ERROR_LEVEL_NEGATIVE.createWithContext(reader)
            }
            state.includesEntities = false
            state.predicates += { _, entity -> entity is Player && range.inRange(entity.level) }
        }

        "x_rotation" -> {
            requireSingleUse(key, keyStart, reader, state)
            state.predicates += rotationPredicate(FloatRangeParser.parse(reader)) { entity -> entity.position.pitch() }
        }

        "y_rotation" -> {
            requireSingleUse(key, keyStart, reader, state)
            state.predicates += rotationPredicate(FloatRangeParser.parse(reader)) { entity -> entity.position.yaw() }
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

private fun consumeTagMarker(reader: StringReader): Boolean {
    reader.skipWhitespace()
    if (reader.canRead() && reader.peek() == '#') {
        reader.skip()
        reader.skipWhitespace()
        return true
    }
    return false
}

/**
 * Reads the tag of a `type=#tag` value and adds the matching predicate.
 *
 * A tag that does not exist in Minestom's entity type registry matches no entity, as in vanilla.
 */
private fun applyTypeTag(inverted: Boolean, valueStart: Int, reader: StringReader, state: MutableSelector) {
    val invertableState = state.invertableStateFor("type")
    val tagKey = ResourceLocationParser.readKey(reader)
    if (!invertableState.canParseTag(tagKey)) {
        reader.cursor = valueStart
        throw ERROR_INAPPLICABLE_OPTION.createWithContext(reader, "type")
    }
    invertableState.markParsedTag(tagKey)

    val tag = EntityType.staticRegistry().getTag(tagKey)
    state.predicates += { _, entity -> (tag != null && tag.contains(entity.entityType)) != inverted }
}

/**
 * Rejects a repeated single-use option and an option that is not [applicable]; `limit` and `sort`
 * are not applicable to a self selector (`@s`).
 *
 * The check runs before the value is read, so a rejected option never parses its value.
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
 * Rejects a use of `name`, `type`, `team` or `gamemode` that [InvertableState] does not allow.
 *
 * The check runs before the value is read.
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
 * Adds the volume and distance predicates after all options are read, so they see the final
 * `dx`, `dy`, `dz` and `distance` values regardless of option order.
 */
private fun finalizePredicates(state: MutableSelector) {
    val coordinates = state.coordinates
    if ("dx" in coordinates || "dy" in coordinates || "dz" in coordinates) {
        val dx = coordinates["dx"] ?: 0.0
        val dy = coordinates["dy"] ?: 0.0
        val dz = coordinates["dz"] ?: 0.0
        state.predicates += { origin, entity -> matchesVolume(origin, dx, dy, dz, entity) }
    }
    state.distance?.let { range ->
        state.predicates += { origin, entity -> range.inRange(entity.getDistance(origin).toFloat()) }
    }
}

private fun originOf(coordinates: Map<String, Double>): (Pos) -> Pos {
    if ("x" !in coordinates && "y" !in coordinates && "z" !in coordinates) return { base -> base }
    val x = coordinates["x"]
    val y = coordinates["y"]
    val z = coordinates["z"]
    return { base -> Pos(x ?: base.x(), y ?: base.y(), z ?: base.z()) }
}

/**
 * Matches entities whose bounding box intersects the `dx`/`dy`/`dz` box at [origin].
 *
 * Per axis the box spans from the origin plus `min(0, d)` to the origin plus `max(0, d) + 1`, as in
 * vanilla's `EntitySelectorParser.createAabb`, so `dx=0,dy=0,dz=0` covers the block at the origin.
 * Boxes that only touch do not intersect.
 */
private fun matchesVolume(origin: Pos, dx: Double, dy: Double, dz: Double, entity: Entity): Boolean {
    val pos = entity.position
    val box = entity.boundingBox

    return pos.x() + box.minX() < origin.x() + maxOf(0.0, dx) + 1.0 &&
            pos.x() + box.maxX() > origin.x() + minOf(0.0, dx) &&
            pos.y() + box.minY() < origin.y() + maxOf(0.0, dy) + 1.0 &&
            pos.y() + box.maxY() > origin.y() + minOf(0.0, dy) &&
            pos.z() + box.minZ() < origin.z() + maxOf(0.0, dz) + 1.0 &&
            pos.z() + box.maxZ() > origin.z() + minOf(0.0, dz)
}

private fun isAlive(@Suppress("UNUSED_PARAMETER") origin: Pos, entity: Entity): Boolean =
    !(entity is LivingEntity && entity.isDead)

/**
 * Returns a predicate matching the wrapped rotation of an entity against [range], as vanilla does.
 * An omitted bound defaults to `0` or `359`; a minimum above the maximum after wrapping selects the
 * range that crosses the `-180`/`180` boundary.
 */
private fun rotationPredicate(
    range: Range.Float,
    rotation: (Entity) -> Float,
): (Pos, Entity) -> Boolean {
    val min = wrapDegrees(range.min()?.takeIf { it != -Float.MAX_VALUE } ?: 0f)
    val max = wrapDegrees(range.max()?.takeIf { it != Float.MAX_VALUE } ?: 359f)
    return { _, entity ->
        val degrees = wrapDegrees(rotation(entity))
        if (min > max) degrees >= min || degrees <= max else degrees >= min && degrees <= max
    }
}

/**
 * Whether a bound of this range other than the omitted-bound default is negative.
 */
private fun Range.Float.hasNegativeBound(): Boolean {
    val min = min()
    val max = max()
    return (min != null && min != -Float.MAX_VALUE && min < 0f) || (max != null && max < 0f)
}

/**
 * Whether a bound of this range other than the omitted-bound default is negative.
 */
private fun Range.Int.hasNegativeBound(): Boolean {
    val min = min()
    val max = max()
    return (min != null && min != Int.MIN_VALUE && min < 0) || (max != null && max < 0)
}

private fun matchesName(entity: Entity, name: String): Boolean = when (entity) {
    is Player -> entity.username == name
    else -> entity.get(DataComponents.CUSTOM_NAME)
        ?.let { component -> PlainTextComponentSerializer.plainText().serialize(component) } == name
}

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

/**
 * Reads and discards a `scores` value of the form `{name=range,...}`.
 */
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

/**
 * Reads and discards an `advancements` value of the form `{id=bool}` or `{id={criterion=bool,...}}`.
 */
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

/**
 * Returns a comparator that orders entities randomly. Each entity keeps one random weight for the
 * lifetime of the comparator, so repeated comparisons stay consistent.
 */
private fun randomComparator(@Suppress("UNUSED_PARAMETER") anchor: Pos): Comparator<Entity> {
    val weights = IdentityHashMap<Entity, Double>()
    val random = ThreadLocalRandom.current()
    return Comparator.comparingDouble { entity -> weights.getOrPut(entity) { random.nextDouble() } }
}

private val GAME_MODE_PARSER =
    FixedSetParser(GameMode.entries.associateBy { mode -> mode.name.lowercase() })

/**
 * Entity tag holding the strings matched by the `tag` option. Nothing in this module writes it.
 */
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
private val ERROR_DISTANCE_NEGATIVE =
    SimpleCommandExceptionType(LiteralMessage("Distance cannot be negative"))
private val ERROR_LEVEL_NEGATIVE =
    SimpleCommandExceptionType(LiteralMessage("Level cannot be negative"))
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
