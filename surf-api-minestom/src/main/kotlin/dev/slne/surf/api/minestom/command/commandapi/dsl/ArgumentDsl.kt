package dev.slne.surf.api.minestom.command.commandapi.dsl

import dev.slne.surf.api.minestom.command.commandapi.CommandAPICommand
import dev.slne.surf.api.minestom.command.commandapi.CommandTree
import dev.slne.surf.api.minestom.command.commandapi.argument.*
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.key.Key
import net.minestom.server.color.TeamColor
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.particle.Particle
import net.minestom.server.potion.PotionEffect
import net.minestom.server.registry.Registry
import net.minestom.server.registry.RegistryKey
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.Range
import net.minestom.server.world.biome.Biome
import java.time.Duration
import java.util.*

inline fun CommandAPICommand.booleanArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Boolean>.() -> Unit = {},
): CommandAPICommand = withArguments(
    BooleanArgument(nodeName).setOptional(optional = optional).apply(block),
)

inline fun CommandAPICommand.integerArgument(
    nodeName: String,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
    optional: Boolean = false,
    block: Argument<Int>.() -> Unit = {},
): CommandAPICommand = withArguments(
    IntegerArgument(nodeName, min, max).setOptional(optional).apply(block),
)

inline fun CommandAPICommand.stringArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<String>.() -> Unit = {},
): CommandAPICommand = withArguments(
    StringArgument(nodeName).setOptional(optional).apply(block),
)

inline fun CommandAPICommand.textArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<String>.() -> Unit = {},
): CommandAPICommand = withArguments(
    TextArgument(nodeName).setOptional(optional).apply(block),
)

inline fun CommandAPICommand.greedyStringArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<String>.() -> Unit = {},
): CommandAPICommand = withArguments(
    GreedyStringArgument(nodeName).setOptional(optional).apply(block),
)

inline fun CommandAPICommand.signedMessageArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<SignedMessage>.() -> Unit = {},
): CommandAPICommand = withArguments(
    SignedMessageArgument(nodeName).setOptional(optional).apply(block),
)

inline fun CommandAPICommand.literalArgument(
    nodeName: String,
    literal: String = nodeName,
    block: Argument<String>.() -> Unit = {},
): CommandAPICommand = withArguments(LiteralArgument(nodeName, literal).apply(block))

inline fun CommandAPICommand.playerArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Player>.() -> Unit = {},
): CommandAPICommand = withArguments(
    PlayerArgument(nodeName).setOptional(optional).apply(block),
)

inline fun CommandAPICommand.playersArgument(
    nodeName: String,
    allowEmpty: Boolean = false,
    optional: Boolean = false,
    block: Argument<List<Player>>.() -> Unit = {},
): CommandAPICommand = withArguments(
    PlayersArgument(nodeName, allowEmpty).setOptional(optional).apply(block),
)

inline fun CommandAPICommand.entityArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Entity>.() -> Unit = {},
): CommandAPICommand = withArguments(
    EntityArgument(nodeName).setOptional(optional).apply(block),
)

inline fun CommandAPICommand.entitiesArgument(
    nodeName: String,
    allowEmpty: Boolean = false,
    optional: Boolean = false,
    block: Argument<List<Entity>>.() -> Unit = {},
): CommandAPICommand = withArguments(
    EntitiesArgument(nodeName, allowEmpty).setOptional(optional).apply(block),
)

inline fun CommandAPICommand.entityTypeArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<EntityType>.() -> Unit = {},
): CommandAPICommand = withArguments(
    EntityTypeArgument(nodeName).setOptional(optional).apply(block),
)

inline fun CommandAPICommand.locationArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Vec>.() -> Unit = {},
): CommandAPICommand = withArguments(PositionArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandAPICommand.location2DArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Vec>.() -> Unit = {},
): CommandAPICommand =
    withArguments(Position2DArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandAPICommand.blockPositionArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Vec>.() -> Unit = {},
): CommandAPICommand =
    withArguments(BlockPositionArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandAPICommand.rotationArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Rotation>.() -> Unit = {},
): CommandAPICommand = withArguments(RotationArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandAPICommand.angleArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Float>.() -> Unit = {},
): CommandAPICommand = withArguments(AngleArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandAPICommand.axisArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Set<Axis>>.() -> Unit = {},
): CommandAPICommand = withArguments(AxisArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandAPICommand.resourceLocationArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Key>.() -> Unit = {},
): CommandAPICommand =
    withArguments(ResourceLocationArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandAPICommand.timeArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Duration>.() -> Unit = {},
): CommandAPICommand = withArguments(TimeArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandAPICommand.teamColorArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<TeamColor>.() -> Unit = {},
): CommandAPICommand = withArguments(TeamColorArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandAPICommand.particleArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Particle>.() -> Unit = {},
): CommandAPICommand = withArguments(ParticleArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandAPICommand.gameModeArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<GameMode>.() -> Unit = {},
): CommandAPICommand = withArguments(GameModeArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandAPICommand.commandArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<String>.() -> Unit = {},
): CommandAPICommand = withArguments(CommandArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandAPICommand.soundArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<SoundEvent>.() -> Unit = {},
): CommandAPICommand = withArguments(SoundArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandAPICommand.potionEffectArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<PotionEffect>.() -> Unit = {},
): CommandAPICommand =
    withArguments(PotionEffectArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandAPICommand.biomeArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<RegistryKey<Biome>>.() -> Unit = {},
): CommandAPICommand = withArguments(BiomeArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandAPICommand.enchantmentArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<RegistryKey<Enchantment>>.() -> Unit = {},
): CommandAPICommand =
    withArguments(EnchantmentArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> CommandAPICommand.resourceArgument(
    nodeName: String,
    identifier: String,
    registry: Registry<T>,
    optional: Boolean = false,
    block: Argument<RegistryKey<T>>.() -> Unit = {},
): CommandAPICommand = withArguments(
    ResourceArgument(nodeName, identifier, registry).setOptional(optional).apply(block),
)

inline fun CommandAPICommand.instanceArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Instance>.() -> Unit = {},
): CommandAPICommand = withArguments(InstanceArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandAPICommand.longArgument(
    nodeName: String,
    min: Long = Long.MIN_VALUE,
    max: Long = Long.MAX_VALUE,
    optional: Boolean = false,
    block: Argument<Long>.() -> Unit = {},
): CommandAPICommand = withArguments(
    LongArgument(nodeName, min, max).setOptional(optional).apply(block),
)

inline fun CommandAPICommand.floatArgument(
    nodeName: String,
    min: Float = -Float.MAX_VALUE,
    max: Float = Float.MAX_VALUE,
    optional: Boolean = false,
    block: Argument<Float>.() -> Unit = {},
): CommandAPICommand = withArguments(
    FloatArgument(nodeName, min, max).setOptional(optional).apply(block),
)

inline fun CommandAPICommand.doubleArgument(
    nodeName: String,
    min: Double = -Double.MAX_VALUE,
    max: Double = Double.MAX_VALUE,
    optional: Boolean = false,
    block: Argument<Double>.() -> Unit = {},
): CommandAPICommand = withArguments(
    DoubleArgument(nodeName, min, max).setOptional(optional).apply(block),
)

inline fun <E : Enum<E>> CommandAPICommand.enumArgument(
    nodeName: String,
    values: Collection<E>,
    noinline formatter: (E) -> String = { value -> value.name.lowercase() },
    optional: Boolean = false,
    block: Argument<E>.() -> Unit = {},
): CommandAPICommand = withArguments(
    EnumArgument(nodeName, values, formatter).setOptional(optional).apply(block),
)

inline fun CommandAPICommand.uuidArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<UUID>.() -> Unit = {},
): CommandAPICommand = withArguments(UUIDArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandAPICommand.integerRangeArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Range.Int>.() -> Unit = {},
): CommandAPICommand =
    withArguments(IntegerRangeArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandAPICommand.floatRangeArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Range.Float>.() -> Unit = {},
): CommandAPICommand =
    withArguments(FloatRangeArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandAPICommand.multiLiteralArgument(
    nodeName: String,
    vararg literals: String,
    block: Argument<String>.() -> Unit = {},
): CommandAPICommand = withArguments(MultiLiteralArgument(nodeName, *literals).apply(block))

inline fun CommandTree.integerArgument(
    nodeName: String,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
    optional: Boolean = false,
    block: Argument<Int>.() -> Unit = {},
): CommandTree = then(IntegerArgument(nodeName, min, max).setOptional(optional).apply(block))

inline fun CommandTree.stringArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<String>.() -> Unit = {},
): CommandTree = then(StringArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.literalArgument(
    nodeName: String,
    literal: String = nodeName,
    block: Argument<String>.() -> Unit = {},
): CommandTree = then(LiteralArgument(nodeName, literal).apply(block))

inline fun CommandTree.playerArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Player>.() -> Unit = {},
): CommandTree = then(PlayerArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.playersArgument(
    nodeName: String,
    allowEmpty: Boolean = false,
    optional: Boolean = false,
    block: Argument<List<Player>>.() -> Unit = {},
): CommandTree = then(PlayersArgument(nodeName, allowEmpty).setOptional(optional).apply(block))

inline fun CommandTree.entityArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Entity>.() -> Unit = {},
): CommandTree = then(EntityArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.entitiesArgument(
    nodeName: String,
    allowEmpty: Boolean = false,
    optional: Boolean = false,
    block: Argument<List<Entity>>.() -> Unit = {},
): CommandTree = then(EntitiesArgument(nodeName, allowEmpty).setOptional(optional).apply(block))

inline fun CommandTree.entityTypeArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<EntityType>.() -> Unit = {},
): CommandTree = then(EntityTypeArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.locationArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Vec>.() -> Unit = {},
): CommandTree = then(PositionArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.location2DArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Vec>.() -> Unit = {},
): CommandTree = then(Position2DArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.blockPositionArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Vec>.() -> Unit = {},
): CommandTree = then(BlockPositionArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.rotationArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Rotation>.() -> Unit = {},
): CommandTree = then(RotationArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.angleArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Float>.() -> Unit = {},
): CommandTree = then(AngleArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.axisArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Set<Axis>>.() -> Unit = {},
): CommandTree = then(AxisArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.resourceLocationArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Key>.() -> Unit = {},
): CommandTree = then(ResourceLocationArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.timeArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Duration>.() -> Unit = {},
): CommandTree = then(TimeArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.teamColorArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<TeamColor>.() -> Unit = {},
): CommandTree = then(TeamColorArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.particleArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Particle>.() -> Unit = {},
): CommandTree = then(ParticleArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.gameModeArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<GameMode>.() -> Unit = {},
): CommandTree = then(GameModeArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.commandArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<String>.() -> Unit = {},
): CommandTree = then(CommandArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.soundArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<SoundEvent>.() -> Unit = {},
): CommandTree = then(SoundArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.potionEffectArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<PotionEffect>.() -> Unit = {},
): CommandTree = then(PotionEffectArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.biomeArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<RegistryKey<Biome>>.() -> Unit = {},
): CommandTree = then(BiomeArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.enchantmentArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<RegistryKey<Enchantment>>.() -> Unit = {},
): CommandTree = then(EnchantmentArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> CommandTree.resourceArgument(
    nodeName: String,
    identifier: String,
    registry: Registry<T>,
    optional: Boolean = false,
    block: Argument<RegistryKey<T>>.() -> Unit = {},
): CommandTree =
    then(ResourceArgument(nodeName, identifier, registry).setOptional(optional).apply(block))

inline fun CommandTree.instanceArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Instance>.() -> Unit = {},
): CommandTree = then(InstanceArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.booleanArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Boolean>.() -> Unit = {},
): CommandTree = then(BooleanArgument(nodeName).setOptional(optional = optional).apply(block))

inline fun CommandTree.textArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<String>.() -> Unit = {},
): CommandTree = then(TextArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.greedyStringArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<String>.() -> Unit = {},
): CommandTree = then(GreedyStringArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.signedMessageArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<SignedMessage>.() -> Unit = {},
): CommandTree = then(SignedMessageArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.longArgument(
    nodeName: String,
    min: Long = Long.MIN_VALUE,
    max: Long = Long.MAX_VALUE,
    optional: Boolean = false,
    block: Argument<Long>.() -> Unit = {},
): CommandTree = then(LongArgument(nodeName, min, max).setOptional(optional).apply(block))

inline fun CommandTree.floatArgument(
    nodeName: String,
    min: Float = -Float.MAX_VALUE,
    max: Float = Float.MAX_VALUE,
    optional: Boolean = false,
    block: Argument<Float>.() -> Unit = {},
): CommandTree = then(FloatArgument(nodeName, min, max).setOptional(optional).apply(block))

inline fun CommandTree.doubleArgument(
    nodeName: String,
    min: Double = -Double.MAX_VALUE,
    max: Double = Double.MAX_VALUE,
    optional: Boolean = false,
    block: Argument<Double>.() -> Unit = {},
): CommandTree = then(DoubleArgument(nodeName, min, max).setOptional(optional).apply(block))

inline fun <E : Enum<E>> CommandTree.enumArgument(
    nodeName: String,
    values: Collection<E>,
    noinline formatter: (E) -> String = { value -> value.name.lowercase() },
    optional: Boolean = false,
    block: Argument<E>.() -> Unit = {},
): CommandTree = then(EnumArgument(nodeName, values, formatter).setOptional(optional).apply(block))

inline fun CommandTree.uuidArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<UUID>.() -> Unit = {},
): CommandTree = then(UUIDArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.integerRangeArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Range.Int>.() -> Unit = {},
): CommandTree = then(IntegerRangeArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.floatRangeArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Range.Float>.() -> Unit = {},
): CommandTree = then(FloatRangeArgument(nodeName).setOptional(optional).apply(block))

inline fun CommandTree.multiLiteralArgument(
    nodeName: String,
    vararg literals: String,
    block: Argument<String>.() -> Unit = {},
): CommandTree = then(MultiLiteralArgument(nodeName, *literals).apply(block))

inline fun <T> Argument<T>.integerArgument(
    nodeName: String,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
    optional: Boolean = false,
    block: Argument<Int>.() -> Unit = {},
): Argument<T> = then(IntegerArgument(nodeName, min, max).setOptional(optional).apply(block))

inline fun <T> Argument<T>.stringArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<String>.() -> Unit = {},
): Argument<T> = then(StringArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.literalArgument(
    nodeName: String,
    literal: String = nodeName,
    block: Argument<String>.() -> Unit = {},
): Argument<T> = then(LiteralArgument(nodeName, literal).apply(block))

inline fun <T> Argument<T>.playerArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Player>.() -> Unit = {},
): Argument<T> = then(PlayerArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.playersArgument(
    nodeName: String,
    allowEmpty: Boolean = false,
    optional: Boolean = false,
    block: Argument<List<Player>>.() -> Unit = {},
): Argument<T> = then(PlayersArgument(nodeName, allowEmpty).setOptional(optional).apply(block))

inline fun <T> Argument<T>.entityArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Entity>.() -> Unit = {},
): Argument<T> = then(EntityArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.entitiesArgument(
    nodeName: String,
    allowEmpty: Boolean = false,
    optional: Boolean = false,
    block: Argument<List<Entity>>.() -> Unit = {},
): Argument<T> = then(EntitiesArgument(nodeName, allowEmpty).setOptional(optional).apply(block))

inline fun <T> Argument<T>.entityTypeArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<EntityType>.() -> Unit = {},
): Argument<T> = then(EntityTypeArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.locationArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Vec>.() -> Unit = {},
): Argument<T> = then(PositionArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.location2DArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Vec>.() -> Unit = {},
): Argument<T> = then(Position2DArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.blockPositionArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Vec>.() -> Unit = {},
): Argument<T> = then(BlockPositionArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.rotationArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Rotation>.() -> Unit = {},
): Argument<T> = then(RotationArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.angleArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Float>.() -> Unit = {},
): Argument<T> = then(AngleArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.axisArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Set<Axis>>.() -> Unit = {},
): Argument<T> = then(AxisArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.resourceLocationArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Key>.() -> Unit = {},
): Argument<T> = then(ResourceLocationArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.timeArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Duration>.() -> Unit = {},
): Argument<T> = then(TimeArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.teamColorArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<TeamColor>.() -> Unit = {},
): Argument<T> = then(TeamColorArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.particleArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Particle>.() -> Unit = {},
): Argument<T> = then(ParticleArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.gameModeArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<GameMode>.() -> Unit = {},
): Argument<T> = then(GameModeArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.commandArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<String>.() -> Unit = {},
): Argument<T> = then(CommandArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.soundArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<SoundEvent>.() -> Unit = {},
): Argument<T> = then(SoundArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.potionEffectArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<PotionEffect>.() -> Unit = {},
): Argument<T> = then(PotionEffectArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.biomeArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<RegistryKey<Biome>>.() -> Unit = {},
): Argument<T> = then(BiomeArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.enchantmentArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<RegistryKey<Enchantment>>.() -> Unit = {},
): Argument<T> = then(EnchantmentArgument(nodeName).setOptional(optional).apply(block))

inline fun <P, T> Argument<P>.resourceArgument(
    nodeName: String,
    identifier: String,
    registry: Registry<T>,
    optional: Boolean = false,
    block: Argument<RegistryKey<T>>.() -> Unit = {},
): Argument<P> =
    then(ResourceArgument(nodeName, identifier, registry).setOptional(optional).apply(block))

inline fun <T> Argument<T>.instanceArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Instance>.() -> Unit = {},
): Argument<T> = then(InstanceArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.booleanArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Boolean>.() -> Unit = {},
): Argument<T> = then(BooleanArgument(nodeName).setOptional(optional = optional).apply(block))

inline fun <T> Argument<T>.textArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<String>.() -> Unit = {},
): Argument<T> = then(TextArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.greedyStringArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<String>.() -> Unit = {},
): Argument<T> = then(GreedyStringArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.signedMessageArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<SignedMessage>.() -> Unit = {},
): Argument<T> = then(SignedMessageArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.longArgument(
    nodeName: String,
    min: Long = Long.MIN_VALUE,
    max: Long = Long.MAX_VALUE,
    optional: Boolean = false,
    block: Argument<Long>.() -> Unit = {},
): Argument<T> = then(LongArgument(nodeName, min, max).setOptional(optional).apply(block))

inline fun <T> Argument<T>.floatArgument(
    nodeName: String,
    min: Float = -Float.MAX_VALUE,
    max: Float = Float.MAX_VALUE,
    optional: Boolean = false,
    block: Argument<Float>.() -> Unit = {},
): Argument<T> = then(FloatArgument(nodeName, min, max).setOptional(optional).apply(block))

inline fun <T> Argument<T>.doubleArgument(
    nodeName: String,
    min: Double = -Double.MAX_VALUE,
    max: Double = Double.MAX_VALUE,
    optional: Boolean = false,
    block: Argument<Double>.() -> Unit = {},
): Argument<T> = then(DoubleArgument(nodeName, min, max).setOptional(optional).apply(block))

inline fun <T, E : Enum<E>> Argument<T>.enumArgument(
    nodeName: String,
    values: Collection<E>,
    noinline formatter: (E) -> String = { value -> value.name.lowercase() },
    optional: Boolean = false,
    block: Argument<E>.() -> Unit = {},
): Argument<T> = then(EnumArgument(nodeName, values, formatter).setOptional(optional).apply(block))

inline fun <T> Argument<T>.uuidArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<UUID>.() -> Unit = {},
): Argument<T> = then(UUIDArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.integerRangeArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Range.Int>.() -> Unit = {},
): Argument<T> = then(IntegerRangeArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.floatRangeArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<Range.Float>.() -> Unit = {},
): Argument<T> = then(FloatRangeArgument(nodeName).setOptional(optional).apply(block))

inline fun <T> Argument<T>.multiLiteralArgument(
    nodeName: String,
    vararg literals: String,
    block: Argument<String>.() -> Unit = {},
): Argument<T> = then(MultiLiteralArgument(nodeName, *literals).apply(block))

inline fun <T, B> CommandAPICommand.customArgument(
    base: Argument<B>,
    noinline parser: (CustomArgumentInfo<B>) -> T,
    optional: Boolean? = null,
    block: Argument<T>.() -> Unit = {},
): CommandAPICommand = withArguments(
    CustomArgument(base, parser = parser).apply {
        optional?.let { override ->
            setOptional(false)
            if (override) setOptional(true)
        }
        block()
    },
)

inline fun <T> CommandAPICommand.listArgument(
    nodeName: String,
    element: Argument<T>,
    delimiter: Char = ',',
    allowEmpty: Boolean = false,
    optional: Boolean = false,
    block: Argument<List<T>>.() -> Unit = {},
): CommandAPICommand = withArguments(
    ListArgument(nodeName, element, delimiter, allowEmpty).setOptional(optional).apply(block),
)

inline fun <T, B> CommandTree.customArgument(
    base: Argument<B>,
    noinline parser: (CustomArgumentInfo<B>) -> T,
    optional: Boolean? = null,
    block: Argument<T>.() -> Unit = {},
): CommandTree = then(
    CustomArgument(base, parser = parser).apply {
        optional?.let { override ->
            setOptional(false)
            if (override) setOptional(true)
        }
        block()
    },
)

inline fun <T> CommandTree.listArgument(
    nodeName: String,
    element: Argument<T>,
    delimiter: Char = ',',
    allowEmpty: Boolean = false,
    optional: Boolean = false,
    block: Argument<List<T>>.() -> Unit = {},
): CommandTree = then(
    ListArgument(nodeName, element, delimiter, allowEmpty).setOptional(optional).apply(block),
)

inline fun <P, T, B> Argument<P>.customArgument(
    base: Argument<B>,
    noinline parser: (CustomArgumentInfo<B>) -> T,
    optional: Boolean? = null,
    block: Argument<T>.() -> Unit = {},
): Argument<P> = then(
    CustomArgument(base, parser = parser).apply {
        optional?.let { override ->
            setOptional(false)
            if (override) setOptional(true)
        }
        block()
    },
)

inline fun <P, T> Argument<P>.listArgument(
    nodeName: String,
    element: Argument<T>,
    delimiter: Char = ',',
    allowEmpty: Boolean = false,
    optional: Boolean = false,
    block: Argument<List<T>>.() -> Unit = {},
): Argument<P> = then(
    ListArgument(nodeName, element, delimiter, allowEmpty).setOptional(optional).apply(block),
)
