package dev.slne.surf.api.minestom.server.impl.command.brigadier

import dev.slne.surf.api.minestom.command.argument.ArgumentDefinition
import dev.slne.surf.api.minestom.command.argument.ArgumentKind
import dev.slne.surf.api.minestom.command.argument.SuggestionMode
import net.minestom.server.command.ArgumentParserType
import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.arguments.minecraft.SuggestionType
import net.minestom.server.registry.BuiltinRegistries
import java.util.concurrent.ConcurrentHashMap

/**
 * How an argument node is declared to the client: the vanilla [parser], its serialized
 * [properties] and the [suggestionsType] that selects the source of suggestions.
 *
 * A `null` [suggestionsType] leaves the suggestions to the client's parser. Equality compares
 * [properties] by content.
 */
internal data class NodeDeclaration(
    val parser: ArgumentParserType,
    val properties: ByteArray?,
    val suggestionsType: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NodeDeclaration) return false
        return parser == other.parser &&
                properties.contentEquals(other.properties) &&
                suggestionsType == other.suggestionsType
    }

    override fun hashCode(): Int {
        var result = parser.hashCode()
        result = 31 * result + properties.contentHashCode()
        return 31 * result + suggestionsType.hashCode()
    }
}

/**
 * Derives the [NodeDeclaration] of an argument definition.
 *
 * A declaration is protocol data only; the server parses input with the argument's own Brigadier
 * type. For most kinds the parser and properties are read from an equivalent native Minestom
 * argument, and the remaining kinds name their parser directly. Declarations are cached per
 * definition and never evicted; the cache is safe for concurrent use.
 */
internal class NodeDeclarations {
    private val cache = ConcurrentHashMap<ArgumentDefinition<*>, NodeDeclaration>()

    /**
     * Returns the declaration of [definition].
     *
     * The suggestions type is `ask_server` when the definition has a custom suggestion provider or
     * its kind is one whose suggestions the client cannot produce itself, and the native one
     * otherwise.
     */
    fun of(definition: ArgumentDefinition<*>): NodeDeclaration = cache.computeIfAbsent(definition) {
        val base = baseDeclarationOf(definition)
        val server = definition.suggestions != SuggestionMode.BuiltIns ||
                computesSuggestions(definition.kind)

        if (server) base.copy(suggestionsType = SuggestionType.ASK_SERVER.identifier) else base
    }

    /**
     * Whether suggestions for [kind] must come from the server.
     *
     * These kinds are declared as `ask_server` even without a suggestion provider. Custom kinds
     * follow their base kind and list kinds their element kind.
     */
    private tailrec fun computesSuggestions(kind: ArgumentKind<*>): Boolean = when (kind) {
        ArgumentKind.Sound,
        ArgumentKind.PotionEffect,
        ArgumentKind.Biome,
        ArgumentKind.Enchantment,
        is ArgumentKind.Resource<*>,
        ArgumentKind.Instance,
        ArgumentKind.Player,
        is ArgumentKind.Players,
        ArgumentKind.Entity,
        is ArgumentKind.Entities,
        is ArgumentKind.MultiLiteral,
        is ArgumentKind.Enum<*>,
            -> true

        is ArgumentKind.Custom<*, *> -> computesSuggestions(kind.base.kind)
        is ArgumentKind.List<*> -> computesSuggestions(kind.element.kind)
        else -> false
    }

    private fun baseDeclarationOf(definition: ArgumentDefinition<*>): NodeDeclaration =
        when (val kind = definition.kind) {
            // Declared directly because the native Minestom argument would name a different parser.
            ArgumentKind.Rotation -> NodeDeclaration(ArgumentParserType.ROTATION, null)
            ArgumentKind.Angle -> NodeDeclaration(ArgumentParserType.ANGLE, null)
            ArgumentKind.Axis -> NodeDeclaration(ArgumentParserType.SWIZZLE, null)
            ArgumentKind.GameMode -> NodeDeclaration(ArgumentParserType.GAMEMODE, null)

            ArgumentKind.SignedMessage -> NodeDeclaration(ArgumentParserType.MESSAGE, null)

            // A custom argument is declared as its base.
            is ArgumentKind.Custom<*, *> -> baseDeclarationOf(kind.base)

            else -> nativeFor(definition).let { native ->
                NodeDeclaration(
                    parser = native.parser(),
                    properties = native.nodeProperties(),
                    suggestionsType = native.suggestionType()?.identifier,
                )
            }
        }

    private fun nativeFor(definition: ArgumentDefinition<*>): Argument<*> {
        val name = definition.nodeName

        return when (val kind = definition.kind) {
            ArgumentKind.Boolean -> ArgumentType.Boolean(name)
            is ArgumentKind.Integer -> ArgumentType.Integer(name).min(kind.min).max(kind.max)
            is ArgumentKind.Long -> ArgumentType.Long(name).min(kind.min).max(kind.max)
            is ArgumentKind.Float -> ArgumentType.Float(name).min(kind.min).max(kind.max)
            is ArgumentKind.Double -> ArgumentType.Double(name).min(kind.min).max(kind.max)

            ArgumentKind.Word -> ArgumentType.Word(name)
            ArgumentKind.Text -> ArgumentType.String(name)
            ArgumentKind.GreedyString -> ArgumentType.StringArray(name)
            is ArgumentKind.Literal -> ArgumentType.Literal(kind.literal)
            // Declared as a word whose accepted values the client requests from the server.
            is ArgumentKind.MultiLiteral -> ArgumentType.Word(name)
            is ArgumentKind.Enum<*> -> ArgumentType.Word(name)

            ArgumentKind.Uuid -> ArgumentType.UUID(name)
            ArgumentKind.IntegerRange -> ArgumentType.IntRange(name)
            ArgumentKind.FloatRange -> ArgumentType.FloatRange(name)
            ArgumentKind.Command -> ArgumentType.Command(name)

            ArgumentKind.Player -> ArgumentType.Entity(name).singleEntity(true).onlyPlayers(true)
            is ArgumentKind.Players -> ArgumentType.Entity(name).singleEntity(false).onlyPlayers(true)
            ArgumentKind.Entity -> ArgumentType.Entity(name).singleEntity(true).onlyPlayers(false)
            is ArgumentKind.Entities -> ArgumentType.Entity(name).singleEntity(false).onlyPlayers(false)
            ArgumentKind.EntityType -> ArgumentType.EntityType(name)

            ArgumentKind.Position -> ArgumentType.RelativeVec3(name)
            ArgumentKind.Position2D -> ArgumentType.RelativeVec2(name)
            ArgumentKind.BlockPosition -> ArgumentType.RelativeBlockPosition(name)

            ArgumentKind.ResourceLocation -> ArgumentType.ResourceLocation(name)
            ArgumentKind.Time -> ArgumentType.Time(name)
            ArgumentKind.TeamColor -> ArgumentType.TeamColor(name)
            ArgumentKind.Particle -> ArgumentType.Particle(name)

            // Sound and potion effect are declared as resource locations.
            ArgumentKind.Sound -> ArgumentType.ResourceLocation(name)
            ArgumentKind.PotionEffect -> ArgumentType.ResourceLocation(name)
            ArgumentKind.Biome -> ArgumentType.Resource(name, BuiltinRegistries.BIOME.name())
            ArgumentKind.Enchantment ->
                ArgumentType.Resource(name, BuiltinRegistries.ENCHANTMENT.name())

            is ArgumentKind.Resource<*> -> ArgumentType.Resource(name, kind.identifier)
            ArgumentKind.Instance -> ArgumentType.Word(name)

            // A list is declared as one greedy string that the server splits.
            is ArgumentKind.List<*> -> ArgumentType.StringArray(name)

            ArgumentKind.Rotation,
            ArgumentKind.Angle,
            ArgumentKind.Axis,
            ArgumentKind.GameMode,
            ArgumentKind.SignedMessage,
            is ArgumentKind.Custom<*, *>,
                -> error("Kind ${definition.kind} declares its parser directly")
        }
    }
}
