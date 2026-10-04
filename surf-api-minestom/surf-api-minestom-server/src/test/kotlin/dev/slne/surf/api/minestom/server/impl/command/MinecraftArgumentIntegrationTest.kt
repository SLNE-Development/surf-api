package dev.slne.surf.api.minestom.server.impl.command

import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.CommandTree
import dev.slne.surf.api.minestom.command.argument.GameModeArgument
import dev.slne.surf.api.minestom.command.argument.ParticleArgument
import dev.slne.surf.api.minestom.command.argument.ResourceLocationArgument
import dev.slne.surf.api.minestom.command.argument.StringArgument
import dev.slne.surf.api.minestom.command.argument.TeamColorArgument
import dev.slne.surf.api.minestom.command.argument.TimeArgument
import dev.slne.surf.api.minestom.command.dsl.commandArgument
import dev.slne.surf.api.minestom.command.dsl.gameModeArgument
import dev.slne.surf.api.minestom.command.dsl.particleArgument
import dev.slne.surf.api.minestom.command.dsl.resourceLocationArgument
import dev.slne.surf.api.minestom.command.dsl.teamColorArgument
import dev.slne.surf.api.minestom.command.dsl.timeArgument
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.suggestion.ArgumentSuggestions
import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.BinaryTag
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.nbt.IntBinaryTag
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minestom.server.MinecraftServer
import net.minestom.server.adventure.MinestomAdventure
import net.minestom.server.color.TeamColor
import net.minestom.server.command.ArgumentParserType
import net.minestom.server.command.CommandManager
import net.minestom.server.command.builder.CommandResult
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.component.DataComponents
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.player.GameProfile
import net.minestom.server.particle.Particle
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestConnection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture

@EnvTest
class MinecraftArgumentIntegrationTest {
    @Test
    fun `resource location argument parses namespaced keys and rejects invalid ones`(env: Env) {
        val sender = env.process().command().consoleSender
        val compiled = ResourceLocationArgument("key").toDefinition()

        assertEquals(Key.key("minecraft:diamond"), compiled.read(sender, "minecraft:diamond"))
        assertThrows(CommandSyntaxException::class.java) { compiled.read(sender, "Not_Valid!") }

        assertEquals(ArgumentParserType.RESOURCE_LOCATION, compiled.declaration().parser)
        assertNull(compiled.declaration().properties)
        assertEquals(
            "minecraft:diamond",
            ResourceLocationArgument("key").toDefinition().stringify(Key.key("minecraft:diamond")),
        )
    }

    @Test
    fun `time argument parses suffixed durations and rejects unknown suffixes`(env: Env) {
        val sender = env.process().command().consoleSender
        val compiled = TimeArgument("delay").toDefinition()

        // Vanilla counts a day as 24000 ticks, so "2d" is 40 real minutes rather than two days.
        assertEquals(Duration.ofMinutes(40), compiled.read(sender, "2d"))
        assertEquals(Duration.ofSeconds(3), compiled.read(sender, "3s"))
        assertEquals(
            Duration.ofMillis(100L * MinecraftServer.TICK_MS),
            compiled.read(sender, "100t"),
        )
        assertEquals(
            Duration.ofMillis(50L * MinecraftServer.TICK_MS),
            compiled.read(sender, "50"),
        )
        assertThrows(CommandSyntaxException::class.java) { compiled.read(sender, "5x") }
        assertThrows(CommandSyntaxException::class.java) { compiled.read(sender, "abc") }

        assertEquals(ArgumentParserType.TIME, compiled.declaration().parser)
        assertEquals(
            ArgumentType.Time("expected").nodeProperties()!!.toList(),
            compiled.declaration().properties!!.toList(),
        )
        assertEquals(
            "100t",
            TimeArgument("delay").toDefinition().stringify(Duration.ofMillis(100L * MinecraftServer.TICK_MS)),
        )
    }

    @Test
    fun `team color argument parses vanilla color names and rejects unknown colors`(env: Env) {
        val sender = env.process().command().consoleSender
        val compiled = TeamColorArgument("color").toDefinition()

        assertEquals(TeamColor.RED, compiled.read(sender, "red"))
        assertThrows(CommandSyntaxException::class.java) { compiled.read(sender, "not_a_color") }

        assertEquals(ArgumentParserType.TEAM_COLOR, compiled.declaration().parser)
        assertNull(compiled.declaration().properties)
        assertEquals("red", TeamColorArgument("color").toDefinition().stringify(TeamColor.RED))
    }

    @Test
    fun `particle argument parses namespaced particle keys and rejects unknown particles`(env: Env) {
        val sender = env.process().command().consoleSender
        val compiled = ParticleArgument("particle").toDefinition()

        val flame = compiled.read(sender, "minecraft:flame")
        assertEquals(Particle.fromKey("minecraft:flame"), flame)
        assertThrows(CommandSyntaxException::class.java) { compiled.read(sender, "minecraft:not_a_particle") }

        assertEquals(ArgumentParserType.PARTICLE, compiled.declaration().parser)
        assertNull(compiled.declaration().properties)
        assertEquals("minecraft:flame", ParticleArgument("particle").toDefinition().stringify(flame))
    }

    @Test
    fun `game mode argument parses the vanilla names and rejects any other casing`(env: Env) {
        val sender = env.process().command().consoleSender
        val compiled = GameModeArgument("mode").toDefinition()

        assertEquals(GameMode.SURVIVAL, compiled.read(sender, "survival"))
        assertEquals(GameMode.CREATIVE, compiled.read(sender, "creative"))
        assertThrows(CommandSyntaxException::class.java) { compiled.read(sender, "CREATIVE") }
        assertThrows(CommandSyntaxException::class.java) { compiled.read(sender, "not_a_mode") }

        assertEquals(ArgumentParserType.GAMEMODE, compiled.declaration().parser)
        assertNull(compiled.declaration().properties)
        assertEquals("survival", GameModeArgument("mode").toDefinition().stringify(GameMode.SURVIVAL))
    }

    @Test
    fun `custom suggestion modes are permitted for game mode arguments`(env: Env) {
        withPlatform(env) {
            CommandAPICommand("allow-game-mode")
                .withArguments(
                    GameModeArgument("mode").includeSuggestions(ArgumentSuggestions.strings("extra")),
                )
                .anyExecutor { _, _ -> }
                .register()

            assertTrue(runCommand(env.process().command().consoleSender, "allow-game-mode survival"))
        }
    }

    @Test
    fun `every new value argument is reachable through the executor and reports its native parser`(env: Env) {
        val captured = mutableMapOf<String, Any?>()
        withPlayerPlatform(env) { manager, player ->
            CommandAPICommand("resource-location-cmd")
                .withArguments(ResourceLocationArgument("resourceLocationValue"))
                .anyExecutor { _, arguments ->
                    captured["resource-location-cmd"] = arguments.get<Key>("resourceLocationValue")
                }
                .register()
            CommandAPICommand("time-cmd").withArguments(TimeArgument("timeValue"))
                .anyExecutor { _, arguments -> captured["time-cmd"] = arguments.get<Duration>("timeValue") }
                .register()
            CommandAPICommand("team-color-cmd").withArguments(TeamColorArgument("teamColorValue"))
                .anyExecutor { _, arguments ->
                    captured["team-color-cmd"] = arguments.get<TeamColor>("teamColorValue")
                }
                .register()
            CommandAPICommand("particle-cmd").withArguments(ParticleArgument("particleValue"))
                .anyExecutor { _, arguments -> captured["particle-cmd"] = arguments.get<Particle>("particleValue") }
                .register()
            CommandAPICommand("game-mode-cmd").withArguments(GameModeArgument("gameModeValue"))
                .anyExecutor { _, arguments -> captured["game-mode-cmd"] = arguments.get<GameMode>("gameModeValue") }
                .register()

            assertTrue(runCommand(player, "resource-location-cmd minecraft:diamond"))
            assertEquals(Key.key("minecraft:diamond"), captured["resource-location-cmd"])

            assertTrue(runCommand(player, "time-cmd 2d"))
            assertEquals(Duration.ofMinutes(40), captured["time-cmd"])

            assertTrue(runCommand(player, "team-color-cmd red"))
            assertEquals(TeamColor.RED, captured["team-color-cmd"])

            assertTrue(runCommand(player, "particle-cmd minecraft:flame"))
            assertEquals(Particle.fromKey("minecraft:flame"), captured["particle-cmd"])

            assertTrue(runCommand(player, "game-mode-cmd survival"))
            assertEquals(GameMode.SURVIVAL, captured["game-mode-cmd"])

            val packet = declaredCommands(env, player)
            fun parserOf(nodeName: String) = packet.nodes.single { node -> node.name == nodeName }.parser
            assertEquals(ArgumentParserType.RESOURCE_LOCATION, parserOf("resourceLocationValue"))
            assertEquals(ArgumentParserType.TIME, parserOf("timeValue"))
            assertEquals(ArgumentParserType.TEAM_COLOR, parserOf("teamColorValue"))
            assertEquals(ArgumentParserType.PARTICLE, parserOf("particleValue"))
            assertEquals(ArgumentParserType.GAMEMODE, parserOf("gameModeValue"))
        }
    }

    @Test
    fun `all six remaining minecraft value argument DSL builders register across command tree and child argument receivers`() {
        val command = CommandAPICommand("minecraft-dsl-command")
            .resourceLocationArgument("resourceLocation")
            .timeArgument("time")
            .teamColorArgument("teamColor")
            .particleArgument("particle")
            .gameModeArgument("gameMode")
            .commandArgument("nested")
            .anyExecutor { _, _ -> }
            .toDefinition()

        val tree = CommandTree("minecraft-dsl-tree")
            .resourceLocationArgument("resourceLocation") { anyExecutor { _, _ -> } }
            .timeArgument("time") { anyExecutor { _, _ -> } }
            .teamColorArgument("teamColor") { anyExecutor { _, _ -> } }
            .particleArgument("particle") { anyExecutor { _, _ -> } }
            .gameModeArgument("gameMode") { anyExecutor { _, _ -> } }
            .commandArgument("nested") { anyExecutor { _, _ -> } }
            .toDefinition()

        val child = CommandTree("minecraft-dsl-child")
            .then(
                StringArgument("prefix")
                                                            .resourceLocationArgument("resourceLocation") { anyExecutor { _, _ -> } }
                    .timeArgument("time") { anyExecutor { _, _ -> } }
                    .teamColorArgument("teamColor") { anyExecutor { _, _ -> } }
                    .particleArgument("particle") { anyExecutor { _, _ -> } }
                    .gameModeArgument("gameMode") { anyExecutor { _, _ -> } }
                    .commandArgument("nested") { anyExecutor { _, _ -> } },
            )
            .toDefinition()

        val expectedNames = listOf(
            "resourceLocation", "time", "teamColor", "particle", "gameMode", "nested",
        )
        assertEquals(expectedNames, command.paths.single().arguments.map { it.nodeName })
        assertEquals(
            expectedNames.map { name -> listOf(name) },
            tree.paths.map { path -> path.arguments.map { it.nodeName } },
        )
        assertEquals(
            expectedNames.map { name -> listOf("prefix", name) },
            child.paths.map { path -> path.arguments.map { it.nodeName } },
        )
    }

    private inline fun withPlatform(env: Env, block: () -> Unit) {
        val platform = MinestomCommandAPIPlatform(env.process().command(), MinestomCommandOwnership())
        CommandAPI.installPlatform(platform)
        try {
            block()
        } finally {
            platform.close()
            CommandAPI.uninstallPlatform(platform)
        }
    }

    private inline fun withPlayerPlatform(
        env: Env,
        block: (CommandManager, Player) -> Unit,
    ) {
        val manager = env.process().command()
        val platform = MinestomCommandAPIPlatform(manager, MinestomCommandOwnership())
        val instance = env.createEmptyInstance()
        var player: Player? = null
        CommandAPI.installPlatform(platform)
        try {
            val connected = connect(env.createConnection(profile("Value-${UUID.randomUUID()}")), instance)
            player = connected
            block(manager, connected)
        } finally {
            if (player?.isRemoved == false) player.remove()
            platform.close()
            CommandAPI.uninstallPlatform(platform)
            env.destroyInstance(instance)
        }
    }

    private fun connect(connection: TestConnection, instance: Instance): Player {
        val connected = CompletableFuture<Player>()
        Thread.startVirtualThread {
            runCatching { connection.connect(instance, Pos.ZERO) }
                .onSuccess(connected::complete)
                .onFailure(connected::completeExceptionally)
        }
        return connected.join()
    }

    private fun profile(name: String) = GameProfile(UUID.randomUUID(), name.take(16))
}
