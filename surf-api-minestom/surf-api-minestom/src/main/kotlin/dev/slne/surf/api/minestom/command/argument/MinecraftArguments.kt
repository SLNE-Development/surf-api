/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command.argument

import com.mojang.brigadier.arguments.ArgumentType
import dev.slne.surf.api.minestom.command.argument.parser.FixedSetParser
import dev.slne.surf.api.minestom.command.argument.parser.ParticleParser
import dev.slne.surf.api.minestom.command.argument.parser.ResourceLocationParser
import dev.slne.surf.api.minestom.command.argument.parser.TimeParser
import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import net.minestom.server.color.TeamColor
import net.minestom.server.entity.GameMode
import net.minestom.server.particle.Particle
import java.time.Duration

/**
 * A namespaced key such as `minecraft:stone`; the namespace defaults to `minecraft`.
 */
class ResourceLocationArgument(nodeName: String) : Argument<Key>(nodeName) {
    override val kind = ArgumentKind.ResourceLocation
    override val rawType: ArgumentType<Key> = ResourceLocationParser

    override fun stringify(value: Key): String = value.asString()
}

/**
 * A [Duration] parsed from vanilla's time syntax: a number with an optional unit of `d` (days),
 * `s` (seconds) or `t` (ticks), for example `5s`. Without a unit the number counts ticks.
 *
 * Negative values are rejected.
 */
class TimeArgument(nodeName: String) : Argument<Duration>(nodeName) {
    override val kind = ArgumentKind.Time
    override val rawType: ArgumentType<Duration> = TimeParser

    override fun stringify(value: Duration): String = "${value.toMillis() / MinecraftServer.TICK_MS}t"
}

/**
 * A [TeamColor] parsed from its name as returned by [TeamColor.toString].
 *
 * Parsing is case-sensitive; suggestions match case-insensitively.
 */
class TeamColorArgument(nodeName: String) : Argument<TeamColor>(nodeName) {
    override val kind = ArgumentKind.TeamColor
    override val rawType: ArgumentType<TeamColor> = FixedSetParser(
        TeamColor.entries.associateBy(TeamColor::toString),
    )

    override fun stringify(value: TeamColor): String = value.toString()
}

/**
 * A [Particle] given by its namespaced key.
 *
 * Only particles without options, such as `flame` or `smoke`, are accepted. Particles that take
 * options, such as `dust`, `block` or `vibration`, are rejected as unsupported.
 */
class ParticleArgument(nodeName: String) : Argument<Particle>(nodeName) {
    override val kind = ArgumentKind.Particle
    override val rawType: ArgumentType<Particle> = ParticleParser

    override fun stringify(value: Particle): String = value.name()
}

/**
 * A [GameMode] parsed from its lowercase name: `survival`, `creative`, `adventure` or
 * `spectator`.
 *
 * Parsing is case-sensitive; suggestions match case-insensitively.
 */
class GameModeArgument(nodeName: String) : Argument<GameMode>(nodeName) {
    override val kind = ArgumentKind.GameMode
    override val rawType: ArgumentType<GameMode> = FixedSetParser(
        GameMode.entries.associateBy { mode -> mode.name.lowercase() },
    )

    override fun stringify(value: GameMode): String = value.name.lowercase()
}
