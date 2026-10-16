/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command.argument

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import dev.slne.surf.api.minestom.command.exception.CommandValidationException
import dev.slne.surf.api.minestom.command.executor.CommandExecutable
import dev.slne.surf.api.minestom.command.executor.ExecutorDefinition
import dev.slne.surf.api.minestom.command.suggestion.ArgumentSuggestions
import dev.slne.surf.api.minestom.command.suggestion.SafeSuggestions
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import it.unimi.dsi.fastutil.objects.*
import net.minestom.server.command.CommandSender
import net.minestom.server.utils.Range
import java.time.Duration
import java.util.*
import dev.slne.surf.api.minestom.command.argument.Axis as AxisType
import dev.slne.surf.api.minestom.command.argument.Rotation as RotationType
import net.kyori.adventure.chat.SignedMessage as AdventureSignedMessage
import net.kyori.adventure.key.Key as AdventureKey
import net.minestom.server.color.TeamColor as MinestomTeamColor
import net.minestom.server.coordinate.Vec as MinestomVec
import net.minestom.server.entity.Entity as MinestomEntity
import net.minestom.server.entity.EntityType as MinestomEntityType
import net.minestom.server.entity.GameMode as MinestomGameMode
import net.minestom.server.entity.Player as MinestomPlayer
import net.minestom.server.instance.Instance as MinestomInstance
import net.minestom.server.item.enchant.Enchantment as MinestomEnchantment
import net.minestom.server.particle.Particle as MinestomParticle
import net.minestom.server.potion.PotionEffect as MinestomPotionEffect
import net.minestom.server.registry.Registry as MinestomRegistry
import net.minestom.server.registry.RegistryKey as MinestomRegistryKey
import net.minestom.server.sound.SoundEvent as MinestomSoundEvent
import net.minestom.server.world.biome.Biome as MinestomBiome
import kotlin.Boolean as KBoolean
import kotlin.Double as KDouble
import kotlin.Enum as KEnum
import kotlin.Float as KFloat
import kotlin.Int as KInt
import kotlin.Long as KLong
import kotlin.collections.List as KList

/**
 * Describes how an argument is parsed and declared to the client; [T] is the type of the parsed
 * value.
 *
 * Kinds are consumed by the server platform and not meant to be implemented outside this module.
 */
sealed interface ArgumentKind<T> {
    data object Boolean : ArgumentKind<KBoolean>

    data class Integer(val min: KInt, val max: KInt) : ArgumentKind<KInt>

    data class Long(val min: KLong, val max: KLong) : ArgumentKind<KLong>

    data class Float(val min: KFloat, val max: KFloat) : ArgumentKind<KFloat>

    data class Double(val min: KDouble, val max: KDouble) : ArgumentKind<KDouble>

    data object Word : ArgumentKind<String>

    data object Text : ArgumentKind<String>

    data object GreedyString : ArgumentKind<String>

    data class Literal(val literal: String) : ArgumentKind<String>

    data class MultiLiteral(val literals: KList<String>) : ArgumentKind<String>

    data class Enum<E : KEnum<E>>(
        val values: EnumSet<E>,
        val formatter: (E) -> String,
    ) : ArgumentKind<E>

    data object Uuid : ArgumentKind<UUID>

    data object IntegerRange : ArgumentKind<Range.Int>

    data object FloatRange : ArgumentKind<Range.Float>

    data object Command : ArgumentKind<String>

    data object SignedMessage : ArgumentKind<AdventureSignedMessage>

    data object Player : ArgumentKind<MinestomPlayer>

    data class Players(val allowEmpty: KBoolean) : ArgumentKind<KList<MinestomPlayer>>

    data object Entity : ArgumentKind<MinestomEntity>

    data class Entities(val allowEmpty: KBoolean) : ArgumentKind<KList<MinestomEntity>>

    data object EntityType : ArgumentKind<MinestomEntityType>

    data object Position : ArgumentKind<MinestomVec>

    data object Position2D : ArgumentKind<MinestomVec>

    data object BlockPosition : ArgumentKind<MinestomVec>

    data object Rotation : ArgumentKind<RotationType>

    data object Angle : ArgumentKind<KFloat>

    data object Axis : ArgumentKind<Set<AxisType>>

    data object ResourceLocation : ArgumentKind<AdventureKey>

    data object Time : ArgumentKind<Duration>

    data object TeamColor : ArgumentKind<MinestomTeamColor>

    data object Particle : ArgumentKind<MinestomParticle>

    data object GameMode : ArgumentKind<MinestomGameMode>

    data object Sound : ArgumentKind<MinestomSoundEvent>

    data object PotionEffect : ArgumentKind<MinestomPotionEffect>

    data object Biome : ArgumentKind<MinestomRegistryKey<MinestomBiome>>

    data object Enchantment : ArgumentKind<MinestomRegistryKey<MinestomEnchantment>>

    data class Resource<T>(
        val identifier: String,
        val registry: MinestomRegistry<T>,
    ) : ArgumentKind<MinestomRegistryKey<T>>

    data object Instance : ArgumentKind<MinestomInstance>

    data class Custom<T, B>(
        val base: ArgumentDefinition<B>,
        val parser: (CustomArgumentInfo<B>) -> T,
    ) : ArgumentKind<T>

    data class List<T>(
        val element: ArgumentDefinition<T>,
        val delimiter: Char,
        val allowEmpty: KBoolean,
    ) : ArgumentKind<KList<T>>
}

/**
 * How an argument sources its suggestions.
 *
 * [BuiltIns] offers only what the argument's parser suggests. The `Include` modes add the
 * provider's entries to the built-in ones; the `Replace` modes offer only the provider's entries.
 * The `Safe` modes take typed values that are converted to text with the argument's formatter.
 */
sealed interface SuggestionMode<out T> {
    data object BuiltIns : SuggestionMode<Nothing>

    data class Include<T>(val provider: ArgumentSuggestions) : SuggestionMode<T>

    data class Replace<T>(val provider: ArgumentSuggestions) : SuggestionMode<T>

    data class IncludeSafe<T>(val provider: SafeSuggestions<T>) : SuggestionMode<T>

    data class ReplaceSafe<T>(val provider: SafeSuggestions<T>) : SuggestionMode<T>
}

/**
 * Immutable snapshot of an [Argument] that the server platform compiles into a command node.
 */
data class ArgumentDefinition<T>(
    val nodeName: String,
    val kind: ArgumentKind<T>,
    val optional: KBoolean,
    val defaultValue: ((CommandSender) -> T)?,
    val permissions: Set<String>,
    val requirements: KList<(CommandSender) -> KBoolean>,
    val suggestions: SuggestionMode<T>,
    val greedy: KBoolean,
    val rawType: ArgumentType<T>,
    val stringify: (T) -> String,
    val listDelimiter: Char? = null,
)

/**
 * Placeholder raw type for kinds that the platform builds without a parser, such as
 * [LiteralArgument] and [SignedMessageArgument].
 *
 * Parsing through it always fails.
 */
@InternalSurfApi
class UnsupportedArgumentType<T>(private val nodeName: String) : ArgumentType<T> {
    override fun parse(reader: StringReader): T = throw SimpleCommandExceptionType(
        LiteralMessage("Argument '$nodeName' has no parser"),
    ).createWithContext(reader)
}

/**
 * Base class of all command arguments.
 *
 * An argument is a node in a command: it parses one value, may be optional, carry permissions,
 * requirements and a suggestion provider, and may have nested arguments and executors. Executors
 * read the parsed value from [dev.slne.surf.api.minestom.command.executor.CommandArguments] by
 * [nodeName].
 *
 * Instances are mutable builders; the `with*`, `set*` and suggestion functions return the same
 * instance. An argument is not thread-safe while it is being configured.
 *
 * @property nodeName name under which the parsed value is available; must not be blank
 */
abstract class Argument<T>(val nodeName: String) : CommandExecutable<Argument<T>> {
    protected abstract val kind: ArgumentKind<T>

    /**
     * The Brigadier type that parses this argument's value.
     */
    protected abstract val rawType: ArgumentType<T>

    /**
     * Whether this argument consumes the rest of the command line.
     *
     * A greedy argument must be the last argument of its path; registration throws
     * [CommandValidationException] otherwise.
     */
    protected open val greedy: KBoolean = false

    protected open val listDelimiter: Char? = null

    private var optional = false
    private var defaultValue: ((CommandSender) -> T)? = null
    private val permissions = ObjectLinkedOpenHashSet<String>()
    private val requirements = ObjectArrayList<(CommandSender) -> KBoolean>()
    private var suggestions: SuggestionMode<T> = SuggestionMode.BuiltIns
    private val executors = ObjectArrayList<ExecutorDefinition>()
    private val children = ObjectArrayList<Argument<*>>()

    init {
        require(nodeName.isNotBlank()) { "Argument node name must not be blank" }
    }

    protected abstract fun stringify(value: T): String

    /**
     * Marks this argument as optional or required without assigning a default value.
     *
     * Making the argument required clears a default value. A required argument cannot follow an
     * optional one on the same path; registration throws [CommandValidationException] otherwise.
     *
     * For a [BooleanArgument], use the named form `setOptional(optional = true)` to distinguish
     * this overload from a constant default.
     */
    fun setOptional(optional: KBoolean): Argument<T> = apply {
        this.optional = optional
        if (!optional) {
            defaultValue = null
        }
    }

    /**
     * Makes this argument optional and uses [default] as its value when it is omitted.
     *
     * For a [BooleanArgument], use the named form `setOptional(default = true)` to distinguish
     * the value from the optionality flag overload.
     */
    fun setOptional(default: T): Argument<T> = apply {
        optional = true
        defaultValue = { default }
    }

    /**
     * Makes this argument optional and computes its value from the sender when it is omitted.
     */
    fun setOptional(default: (CommandSender) -> T): Argument<T> = apply {
        optional = true
        defaultValue = default
    }

    /**
     * Requires [permission] to use this argument and the arguments nested below it.
     *
     * Multiple calls accumulate and every permission is required.
     *
     * @throws IllegalArgumentException when [permission] is blank
     */
    fun withPermission(permission: String): Argument<T> = apply {
        val normalized = permission.trim()
        require(normalized.isNotBlank()) { "Argument permission must not be blank" }
        permissions += normalized
    }

    /**
     * Requires [requirement] to return `true` for the sender to use this argument and the arguments
     * nested below it. Multiple requirements accumulate and all must pass.
     */
    fun withRequirement(requirement: (CommandSender) -> KBoolean): Argument<T> = apply {
        requirements += requirement
    }

    /**
     * Offers only the entries of [suggestions], discarding the built-in suggestions of this
     * argument.
     *
     * Replaces any suggestion provider configured before. Literal, command, position and rotation
     * arguments do not support suggestion providers; registration throws
     * [CommandValidationException] for them.
     */
    fun replaceSuggestions(suggestions: ArgumentSuggestions): Argument<T> = apply {
        this.suggestions = SuggestionMode.Replace(suggestions)
    }

    /**
     * Offers the entries of [suggestions] in addition to the built-in suggestions of this argument.
     *
     * Replaces any suggestion provider configured before. Registration throws
     * [CommandValidationException] for [EntityTypeArgument] and for the kinds listed at
     * [replaceSuggestions].
     */
    fun includeSuggestions(suggestions: ArgumentSuggestions): Argument<T> = apply {
        this.suggestions = SuggestionMode.Include(suggestions)
    }

    /**
     * Offers only the values of [suggestions], converted to text with the formatter of this
     * argument.
     *
     * Replaces any suggestion provider configured before.
     */
    fun replaceSafeSuggestions(suggestions: SafeSuggestions<T>): Argument<T> = apply {
        this.suggestions = SuggestionMode.ReplaceSafe(suggestions)
    }

    /**
     * Offers the values of [suggestions], converted to text with the formatter of this argument,
     * in addition to the built-in suggestions.
     *
     * Replaces any suggestion provider configured before.
     */
    fun includeSafeSuggestions(suggestions: SafeSuggestions<T>): Argument<T> = apply {
        this.suggestions = SuggestionMode.IncludeSafe(suggestions)
    }

    /**
     * Adds an executor that runs when the command ends at this argument.
     */
    override fun addExecutor(definition: ExecutorDefinition): Argument<T> = apply {
        executors += definition
    }

    /**
     * Adds [child] as an argument that follows this one.
     *
     * @throws IllegalArgumentException when [child] is this argument; longer cycles are rejected
     * at registration
     */
    fun then(child: Argument<*>): Argument<T> = apply {
        require(child !== this) { "An argument cannot be its own child" }
        children += child
    }

    @InternalSurfApi
    internal fun toTreeNode(): ArgumentTreeNode = toTreeNode(ReferenceOpenHashSet())

    private fun toTreeNode(stack: MutableSet<Argument<*>>): ArgumentTreeNode {
        if (!stack.add(this)) {
            throw CommandValidationException("Argument '$nodeName' contains a child cycle")
        }
        try {
            return ArgumentTreeNode(
                argument = toDefinition(),
                executors = ObjectLists.unmodifiable(ObjectArrayList(executors)),
                children = ObjectLists.unmodifiable(children.mapTo(ObjectArrayList(children.size)) { child ->
                    child.toTreeNode(stack)
                }),
            )
        } finally {
            stack.remove(this)
        }
    }

    @InternalSurfApi
    fun toDefinition(): ArgumentDefinition<T> = ArgumentDefinition(
        nodeName = nodeName,
        kind = kind,
        optional = optional,
        defaultValue = defaultValue,
        permissions = ObjectSets.unmodifiable(ObjectLinkedOpenHashSet(permissions)),
        requirements = ObjectLists.unmodifiable(ObjectArrayList(requirements)),
        suggestions = suggestions,
        greedy = greedy,
        rawType = rawType,
        stringify = ::stringify,
        listDelimiter = listDelimiter,
    )
}

@InternalSurfApi
internal data class ArgumentTreeNode(
    val argument: ArgumentDefinition<*>,
    val executors: KList<ExecutorDefinition>,
    val children: KList<ArgumentTreeNode>,
    val permissions: Set<String> = emptySet(),
    val requirements: KList<(CommandSender) -> KBoolean> = emptyList(),
)
