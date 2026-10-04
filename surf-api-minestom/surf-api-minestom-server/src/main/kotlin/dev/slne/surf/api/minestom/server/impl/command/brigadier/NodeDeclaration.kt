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
 * How one argument is announced to the client: the vanilla parser id it should use, the parser's own
 * properties, and where the client should get its completions from.
 *
 * A `null` [suggestionsType] leaves the completion set to whatever the parser implies on the client.
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
 * Derives the client-facing declaration of every argument kind.
 *
 * A declaration is protocol data, not parsing: it only tells the client which vanilla parser to use
 * and how it is configured, while the server parses the input with the argument's own Brigadier
 * type. Minestom already encodes that mapping for most kinds, so the values are read off a throwaway
 * native argument rather than re-encoded by hand; the few kinds Minestom has no native argument for
 * name their vanilla parser directly.
 */
internal class NodeDeclarations {
    private val cache = ConcurrentHashMap<ArgumentDefinition<*>, NodeDeclaration>()

    fun of(definition: ArgumentDefinition<*>): NodeDeclaration = cache.computeIfAbsent(definition) {
        val base = baseDeclarationOf(definition)
        val server = definition.suggestions != SuggestionMode.BuiltIns ||
                computesSuggestions(definition.kind)

        if (server) base.copy(suggestionsType = SuggestionType.ASK_SERVER.identifier) else base
    }

    /**
     * Whether the completions of [kind] can only come from the server.
     *
     * A client fills a node in from its own registries and its own tab list, which covers most
     * kinds. It cannot know the values that only exist on this server - a live instance, a registry
     * the client has no copy of - nor the accepted spellings of a node declared as a plain string,
     * so those kinds are announced as `ask_server` even with no provider attached.
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
            // Minestom parses these on top of a differently-shaped native argument, so its own
            // declaration would name the wrong parser.
            ArgumentKind.Rotation -> NodeDeclaration(ArgumentParserType.ROTATION, null)
            ArgumentKind.Angle -> NodeDeclaration(ArgumentParserType.ANGLE, null)
            ArgumentKind.Axis -> NodeDeclaration(ArgumentParserType.SWIZZLE, null)
            ArgumentKind.GameMode -> NodeDeclaration(ArgumentParserType.GAMEMODE, null)

            ArgumentKind.SignedMessage -> NodeDeclaration(ArgumentParserType.MESSAGE, null)

            // A custom argument reads whatever its base reads, so it is announced as that base.
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
            // A fixed set of spellings is one node the server matches, not one literal node per
            // spelling, so the client is told it is a word and asks for the accepted values.
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

            // A sound and a potion effect are keys the client completes from its own registry.
            ArgumentKind.Sound -> ArgumentType.ResourceLocation(name)
            ArgumentKind.PotionEffect -> ArgumentType.ResourceLocation(name)
            ArgumentKind.Biome -> ArgumentType.Resource(name, BuiltinRegistries.BIOME.name())
            ArgumentKind.Enchantment ->
                ArgumentType.Resource(name, BuiltinRegistries.ENCHANTMENT.name())

            is ArgumentKind.Resource<*> -> ArgumentType.Resource(name, kind.identifier)
            ArgumentKind.Instance -> ArgumentType.Word(name)

            // A list reads one greedy token and splits it itself.
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
