/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.surf.api.minestom.command.commandapi.argument

import com.mojang.brigadier.arguments.ArgumentType
import dev.slne.surf.api.minestom.command.commandapi.argument.parser.FixedSetParser
import dev.slne.surf.api.minestom.command.commandapi.argument.parser.ParticleParser
import dev.slne.surf.api.minestom.command.commandapi.argument.parser.ResourceLocationParser
import dev.slne.surf.api.minestom.command.commandapi.argument.parser.TimeParser
import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import net.minestom.server.color.TeamColor
import net.minestom.server.entity.GameMode
import net.minestom.server.particle.Particle
import java.time.Duration

class ResourceLocationArgument(nodeName: String) : Argument<Key>(nodeName) {
    override val kind = ArgumentKind.ResourceLocation
    override val rawType: ArgumentType<Key> = ResourceLocationParser

    override fun stringify(value: Key): String = value.asString()
}

/**
 * A duration parsed from vanilla's suffixed time syntax (`50d`, `25s`, `75t`, or a bare tick
 * count).
 */
class TimeArgument(nodeName: String) : Argument<Duration>(nodeName) {
    override val kind = ArgumentKind.Time
    override val rawType: ArgumentType<Duration> = TimeParser

    override fun stringify(value: Duration): String = "${value.toMillis() / MinecraftServer.TICK_MS}t"
}

/**
 * A team color parsed from its vanilla name (`black`, `dark_blue`, ..., `white`); matching is
 * case-sensitive, and suggestions are offered case-insensitively.
 */
class TeamColorArgument(nodeName: String) : Argument<TeamColor>(nodeName) {
    override val kind = ArgumentKind.TeamColor
    override val rawType: ArgumentType<TeamColor> = FixedSetParser(
        TeamColor.entries.associateBy(TeamColor::toString),
    )

    override fun stringify(value: TeamColor): String = value.toString()
}

/**
 * A particle resolved by its namespaced key.
 *
 * Only option-free particles (`flame`, `smoke`, and similar) are accepted; a particle whose kind
 * carries options (`dust`, `block`, `vibration`, and similar) has no syntax to supply those options
 * here and is rejected as unsupported.
 */
class ParticleArgument(nodeName: String) : Argument<Particle>(nodeName) {
    override val kind = ArgumentKind.Particle
    override val rawType: ArgumentType<Particle> = ParticleParser

    override fun stringify(value: Particle): String = value.name()
}

/**
 * A player game mode parsed from its vanilla name (`survival`, `creative`, `adventure`, or
 * `spectator`); matching is case-sensitive, and suggestions are offered case-insensitively.
 */
class GameModeArgument(nodeName: String) : Argument<GameMode>(nodeName) {
    override val kind = ArgumentKind.GameMode
    override val rawType: ArgumentType<GameMode> = FixedSetParser(
        GameMode.entries.associateBy { mode -> mode.name.lowercase() },
    )

    override fun stringify(value: GameMode): String = value.name.lowercase()
}
