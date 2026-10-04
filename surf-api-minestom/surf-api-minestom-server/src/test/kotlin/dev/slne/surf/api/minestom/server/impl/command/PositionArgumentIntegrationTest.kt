package dev.slne.surf.api.minestom.server.impl.command

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.argument.MultiLiteralArgument
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.CommandTree
import dev.slne.surf.api.minestom.command.argument.AngleArgument
import dev.slne.surf.api.minestom.command.argument.Axis
import dev.slne.surf.api.minestom.command.argument.AxisArgument
import dev.slne.surf.api.minestom.command.argument.BlockPositionArgument
import dev.slne.surf.api.minestom.command.argument.Position2DArgument
import dev.slne.surf.api.minestom.command.argument.PositionArgument
import dev.slne.surf.api.minestom.command.argument.Rotation
import dev.slne.surf.api.minestom.command.argument.RotationArgument
import dev.slne.surf.api.minestom.command.argument.StringArgument
import dev.slne.surf.api.minestom.command.dsl.angleArgument
import dev.slne.surf.api.minestom.command.dsl.axisArgument
import dev.slne.surf.api.minestom.command.dsl.blockPositionArgument
import dev.slne.surf.api.minestom.command.dsl.location2DArgument
import dev.slne.surf.api.minestom.command.dsl.locationArgument
import dev.slne.surf.api.minestom.command.dsl.rotationArgument
import dev.slne.surf.api.minestom.command.dsl.stringArgument
import dev.slne.surf.api.minestom.command.exception.CommandValidationException
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.suggestion.ArgumentSuggestions
import net.minestom.server.command.ArgumentParserType
import net.minestom.server.command.builder.CommandResult
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.network.player.GameProfile
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
import java.util.concurrent.atomic.AtomicReference

@EnvTest
class PositionArgumentIntegrationTest {
    @Test
    fun `absolute relative and local positions resolve against the sender position`(env: Env) {
        withPlayerPlatform(env, Pos(10.0, 20.0, 30.0, 0f, 0f)) { player ->
            val position = PositionArgument("pos").toDefinition()
            val position2D = Position2DArgument("pos").toDefinition()
            val blockPosition = BlockPositionArgument("pos").toDefinition()

            assertEquals(Vec(1.0, 2.0, 3.0), position.read(player, "1 2 3"))
            assertEquals(Vec(11.0, 22.0, 33.0), position.read(player, "~1 ~2 ~3"))
            assertEquals(Vec(10.0, 20.0, 30.0), position.read(player, "~ ~ ~"))
            assertEquals(Vec(10.0, 20.0, 30.0), position.read(player, "^0 ^0 ^0"))
            assertVecEquals(Vec(10.0, 20.0, 31.0), position.read(player, "^0 ^0 ^1"))

            assertEquals(Vec(1.0, 0.0, 2.0), position2D.read(player, "1 2"))
            assertEquals(Vec(10.0, 0.0, 30.0), position2D.read(player, "~ ~"))

            assertEquals(Vec(1.0, 2.0, 3.0), blockPosition.read(player, "1 2 3"))
            assertEquals(Vec(11.0, 20.0, 30.0), blockPosition.read(player, "~1 ~0 ~0"))

            assertEquals(ArgumentParserType.VEC3, position.declaration().parser)
            assertEquals(ArgumentParserType.VEC2, position2D.declaration().parser)
            assertEquals(ArgumentParserType.BLOCK_POS, blockPosition.declaration().parser)
        }
    }

    @Test
    fun `block positions reject decimal absolute values but allow decimal relative offsets`(env: Env) {
        withPlayerPlatform(env, Pos(10.0, 20.0, 30.0, 0f, 0f)) { player ->
            val compiled = BlockPositionArgument("pos").toDefinition()

            assertThrows(CommandSyntaxException::class.java) { compiled.read(player, "1.5 2 3") }
            assertEquals(Vec(11.5, 20.0, 30.0), compiled.read(player, "~1.5 ~0 ~0"))
        }
    }

    @Test
    fun `two dimensional positions require exactly two components and stop at the argument boundary`(env: Env) {
        withPlayerPlatform(env, Pos(10.0, 20.0, 30.0, 0f, 0f)) { player ->
            val compiled = Position2DArgument("pos").toDefinition()

            assertThrows(CommandSyntaxException::class.java) { compiled.read(player, "1") }

            // A third component is left for whatever follows this argument rather than consumed.
            val reader = StringReader("1 2 3")
            assertEquals(Vec(1.0, 0.0, 2.0), compiled.rawType.parse(reader, player))
            assertEquals(" 3", reader.remaining)

            val capturedPos = AtomicReference<Vec>()
            val capturedTag = AtomicReference<String>()
            CommandAPICommand("boundary-2d")
                .location2DArgument("pos")
                .stringArgument("tag")
                .anyExecutor { _, arguments ->
                    capturedPos.set(arguments.get("pos"))
                    capturedTag.set(arguments.get("tag"))
                }
                .register()

            val manager = env.process().command()
            assertTrue(runCommand(player, "boundary-2d 1 2 marker"))
            assertEquals(Vec(1.0, 0.0, 2.0), capturedPos.get())
            assertEquals("marker", capturedTag.get())
        }
    }

    @Test
    fun `rotation resolves relative yaw and pitch against the sender view`(env: Env) {
        withPlayerPlatform(env, Pos(0.0, 0.0, 0.0, 45f, 10f)) { player ->
            val compiled = RotationArgument("look").toDefinition()

            assertEquals(Rotation(45f, 10f), compiled.read(player, "~ ~"))
            assertEquals(Rotation(55f, 5f), compiled.read(player, "~10 ~-5"))
            assertEquals(Rotation(90f, 0f), compiled.read(player, "90 0"))

            assertEquals(ArgumentParserType.ROTATION, compiled.declaration().parser)
            assertNull(compiled.declaration().properties)
        }
    }

    @Test
    fun `angle parses absolute and relative values and normalizes into vanillas range`(env: Env) {
        withPlayerPlatform(env, Pos(0.0, 0.0, 0.0, 45f, 10f)) { player ->
            val compiled = AngleArgument("angle").toDefinition()

            assertEquals(90f, compiled.read(player, "450"))
            assertEquals(-90f, compiled.read(player, "270"))
            assertEquals(45f, compiled.read(player, "~"))
            assertEquals(55f, compiled.read(player, "~10"))
            assertEquals(-115f, compiled.read(player, "~200"))

            assertThrows(CommandSyntaxException::class.java) { compiled.read(player, "~-") }
            assertThrows(CommandSyntaxException::class.java) { compiled.read(player, "not-a-number") }
            assertThrows(CommandSyntaxException::class.java) { compiled.read(player, "") }

            assertEquals(ArgumentParserType.ANGLE, compiled.declaration().parser)
            assertNull(compiled.declaration().properties)
        }
    }

    @Test
    fun `angle rejects non-finite absolute and relative values`(env: Env) {
        withPlayerPlatform(env, Pos(0.0, 0.0, 0.0, 45f, 10f)) { player ->
            val compiled = AngleArgument("angle").toDefinition()

            assertThrows(CommandSyntaxException::class.java) { compiled.read(player, "NaN") }
            assertThrows(CommandSyntaxException::class.java) { compiled.read(player, "Infinity") }
            assertThrows(CommandSyntaxException::class.java) { compiled.read(player, "-Infinity") }
            assertThrows(CommandSyntaxException::class.java) { compiled.read(player, "~NaN") }
            assertThrows(CommandSyntaxException::class.java) { compiled.read(player, "~Infinity") }
        }
    }

    @Test
    fun `axis parses letters case-insensitively and rejects duplicates or unknown letters`(env: Env) {
        withPlayerPlatform(env, Pos.ZERO) { player ->
            val compiled = AxisArgument("axes").toDefinition()

            assertEquals(setOf(Axis.X, Axis.Y), compiled.read(player, "xy"))
            assertEquals(setOf(Axis.X, Axis.Y, Axis.Z), compiled.read(player, "XYZ"))
            assertEquals(setOf(Axis.Y), compiled.read(player, "y"))

            assertThrows(CommandSyntaxException::class.java) { compiled.read(player, "xx") }
            assertThrows(CommandSyntaxException::class.java) { compiled.read(player, "xq") }

            assertEquals(ArgumentParserType.SWIZZLE, compiled.declaration().parser)
            assertNull(compiled.declaration().properties)
        }
    }

    @Test
    fun `a suggestion-enabled multi literal is still declared as a plain word`(env: Env) {
        withPlayerPlatform(env, Pos.ZERO) {
            val compiled = MultiLiteralArgument("multi-mode", "one", "two")
                .includeSuggestions(ArgumentSuggestions.strings("one-extra"))
                .toDefinition()

            assertEquals(ArgumentParserType.STRING, compiled.declaration().parser)
            assertEquals(
                ArgumentType.Word("expected").nodeProperties()!!.toList(),
                compiled.declaration().properties!!.toList(),
            )
        }
    }

    @Test
    fun `custom suggestion modes are rejected for multi-component position and rotation kinds but permitted for angle and axis`(
        env: Env,
    ) {
        withPlayerPlatform(env) {
            val rejectedCases = listOf(
                "Position" to PositionArgument("pos").replaceSuggestions(ArgumentSuggestions.strings("~ ~ ~")),
                "Position2D" to Position2DArgument("pos").replaceSuggestions(ArgumentSuggestions.strings("~ ~")),
                "BlockPosition" to BlockPositionArgument("pos").replaceSuggestions(ArgumentSuggestions.strings("~ ~ ~")),
                "Rotation" to RotationArgument("look").replaceSuggestions(ArgumentSuggestions.strings("~ ~")),
            )
            rejectedCases.forEach { (kind, argument) ->
                val commandName = "reject-${kind.lowercase()}"
                val failure = assertThrows(CommandValidationException::class.java) {
                    CommandAPICommand(commandName)
                        .withArguments(argument)
                        .anyExecutor { _, _ -> }
                        .register()
                }
                assertEquals(
                    "$kind argument '${argument.nodeName}' cannot use custom suggestions",
                    failure.message,
                    kind,
                )
            }

            CommandAPICommand("allow-angle")
                .withArguments(AngleArgument("angle").replaceSuggestions(ArgumentSuggestions.strings("0", "90")))
                .anyExecutor { _, _ -> }
                .register()
            CommandAPICommand("allow-axis")
                .withArguments(AxisArgument("axes").includeSuggestions(ArgumentSuggestions.strings("xyz")))
                .anyExecutor { _, _ -> }
                .register()

            assertTrue(runCommand(env.process().command().consoleSender, "allow-angle 90"))
            assertTrue(runCommand(env.process().command().consoleSender, "allow-axis xz"))
        }
    }

    @Test
    fun `all six position argument DSL builders register across command tree and child argument receivers`() {
        val command = CommandAPICommand("position-dsl-command")
            .locationArgument("pos")
            .location2DArgument("pos2d")
            .blockPositionArgument("blockPos")
            .rotationArgument("look")
            .angleArgument("angle")
            .axisArgument("axes")
            .anyExecutor { _, _ -> }
            .toDefinition()

        val tree = CommandTree("position-dsl-tree")
            .locationArgument("pos") { anyExecutor { _, _ -> } }
            .location2DArgument("pos2d") { anyExecutor { _, _ -> } }
            .blockPositionArgument("blockPos") { anyExecutor { _, _ -> } }
            .rotationArgument("look") { anyExecutor { _, _ -> } }
            .angleArgument("angle") { anyExecutor { _, _ -> } }
            .axisArgument("axes") { anyExecutor { _, _ -> } }
            .toDefinition()

        val child = CommandTree("position-dsl-child")
            .then(
                StringArgument("prefix")
                    .locationArgument("pos") { anyExecutor { _, _ -> } }
                    .location2DArgument("pos2d") { anyExecutor { _, _ -> } }
                    .blockPositionArgument("blockPos") { anyExecutor { _, _ -> } }
                    .rotationArgument("look") { anyExecutor { _, _ -> } }
                    .angleArgument("angle") { anyExecutor { _, _ -> } }
                    .axisArgument("axes") { anyExecutor { _, _ -> } },
            )
            .toDefinition()

        assertEquals(
            listOf("pos", "pos2d", "blockPos", "look", "angle", "axes"),
            command.paths.single().arguments.map { it.nodeName },
        )
        assertEquals(
            listOf(
                listOf("pos"),
                listOf("pos2d"),
                listOf("blockPos"),
                listOf("look"),
                listOf("angle"),
                listOf("axes"),
            ),
            tree.paths.map { path -> path.arguments.map { it.nodeName } },
        )
        assertEquals(
            listOf(
                listOf("prefix", "pos"),
                listOf("prefix", "pos2d"),
                listOf("prefix", "blockPos"),
                listOf("prefix", "look"),
                listOf("prefix", "angle"),
                listOf("prefix", "axes"),
            ),
            child.paths.map { path -> path.arguments.map { it.nodeName } },
        )

        assertEquals("1.0 2.0 3.0", PositionArgument("pos").toDefinition().stringify(Vec(1.0, 2.0, 3.0)))
        assertEquals("45.0 10.0", RotationArgument("look").toDefinition().stringify(Rotation(45f, 10f)))
        assertEquals("xy", AxisArgument("axes").toDefinition().stringify(linkedSetOf(Axis.X, Axis.Y)))
        assertEquals(
            "1 2 3",
            BlockPositionArgument("blockPos").toDefinition().stringify(Vec(1.9, 2.1, 3.0)),
        )
        assertEquals(
            "-2 0 0",
            BlockPositionArgument("blockPos").toDefinition().stringify(Vec(-1.5, 0.0, 0.0)),
        )
    }

    private fun assertVecEquals(expected: Vec, actual: Vec, epsilon: Double = 1e-9) {
        assertEquals(expected.x(), actual.x(), epsilon)
        assertEquals(expected.y(), actual.y(), epsilon)
        assertEquals(expected.z(), actual.z(), epsilon)
    }

    private inline fun withPlayerPlatform(
        env: Env,
        spawnPos: Pos = Pos.ZERO,
        block: (Player) -> Unit,
    ) {
        val platform = MinestomCommandAPIPlatform(env.process().command(), MinestomCommandOwnership())
        val instance = env.createEmptyInstance()
        var player: Player? = null
        CommandAPI.installPlatform(platform)
        try {
            val connected = connect(env.createConnection(profile("Pos-${UUID.randomUUID()}")), instance, spawnPos)
            player = connected
            check(env.tickWhile({ connected.instance !== instance }, Duration.ofSeconds(1))) {
                "Player was not spawned in the requested test instance"
            }
            block(connected)
        } finally {
            if (player?.isRemoved == false) player.remove()
            platform.close()
            CommandAPI.uninstallPlatform(platform)
            env.destroyInstance(instance)
        }
    }

    private fun connect(connection: TestConnection, instance: Instance, pos: Pos): Player {
        val connected = CompletableFuture<Player>()
        Thread.startVirtualThread {
            runCatching { connection.connect(instance, pos) }
                .onSuccess(connected::complete)
                .onFailure(connected::completeExceptionally)
        }
        return connected.join()
    }

    private fun profile(name: String) = GameProfile(UUID.randomUUID(), name.take(16))
}
