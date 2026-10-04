package dev.slne.surf.api.minestom.server.impl.command.parser

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.minestom.command.argument.Axis
import dev.slne.surf.api.minestom.command.argument.AxisArgument
import dev.slne.surf.api.minestom.command.argument.BlockPositionArgument
import dev.slne.surf.api.minestom.command.argument.Position2DArgument
import dev.slne.surf.api.minestom.command.argument.PositionArgument
import dev.slne.surf.api.minestom.command.argument.Rotation
import dev.slne.surf.api.minestom.command.argument.RotationArgument
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.network.player.GameProfile
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * The parsers under test here are `internal` to the api module, so every case reaches them through
 * [dev.slne.surf.api.minestom.command.argument.Argument.toDefinition]'s publicly-typed
 * `rawType`, rather than importing the parser objects directly.
 *
 * A `~`/`^` component needs a sender to resolve against; calls that omit one (the bare
 * `parse(reader)` overload used throughout most of this file) must reject it rather than silently
 * substituting the origin. The two tests that spawn a player exercise the sourced overload instead,
 * to prove a relative component actually resolves against that sender.
 */
@EnvTest
class CoordinateParsersTest {
    @Test
    fun `absolute coordinates are read as three numbers`() {
        val position = PositionArgument("pos").toDefinition().rawType
        assertEquals(Vec(1.0, 2.5, -3.0), position.parse(StringReader("1 2.5 -3")))
    }

    @Test
    fun `tilde without a sender is rejected rather than resolved against the origin`() {
        val position = PositionArgument("pos").toDefinition().rawType
        val failure = assertThrows(CommandSyntaxException::class.java) {
            position.parse(StringReader("~ ~5 ~"))
        }
        assertEquals(0, failure.cursor)
    }

    @Test
    fun `caret without a sender is rejected rather than resolved against the origin's default view`() {
        val position = PositionArgument("pos").toDefinition().rawType
        val failure = assertThrows(CommandSyntaxException::class.java) {
            position.parse(StringReader("^0 ^0 ^1"))
        }
        assertEquals(0, failure.cursor)
    }

    @Test
    fun `mixing local and world coordinates is rejected at the position's start`() {
        val position = PositionArgument("pos").toDefinition().rawType

        val leadingLocal = assertThrows(CommandSyntaxException::class.java) {
            position.parse(StringReader("^1 ~2 3"))
        }
        assertEquals(0, leadingLocal.cursor)

        val trailingLocal = assertThrows(CommandSyntaxException::class.java) {
            position.parse(StringReader("1 ^2 3"))
        }
        assertEquals(0, trailingLocal.cursor)
    }

    @Test
    fun `two dimensional positions fix the middle component and stop after their two tokens`() {
        val position2D = Position2DArgument("pos").toDefinition().rawType

        val reader = StringReader("1 2 3")
        assertEquals(Vec(1.0, 0.0, 2.0), position2D.parse(reader))
        assertEquals(" 3", reader.remaining)

        assertThrows(CommandSyntaxException::class.java) { position2D.parse(StringReader("1")) }
    }

    @Test
    fun `block positions reject a decimal absolute value`() {
        val blockPosition = BlockPositionArgument("pos").toDefinition().rawType
        assertThrows(CommandSyntaxException::class.java) { blockPosition.parse(StringReader("1.5 2 3")) }
    }

    @Test
    fun `rotation reads an absolute yaw and pitch without needing a sender`() {
        val rotation = RotationArgument("look").toDefinition().rawType
        assertEquals(Rotation(45f, 10f), rotation.parse(StringReader("45 10")))
    }

    @Test
    fun `rotation tilde without a sender is rejected rather than resolved against the origin's view`() {
        val rotation = RotationArgument("look").toDefinition().rawType
        val failure = assertThrows(CommandSyntaxException::class.java) {
            rotation.parse(StringReader("~10 ~-5"))
        }
        assertEquals(0, failure.cursor)
    }

    @Test
    fun `rotation rejects a local coordinate`() {
        val rotation = RotationArgument("look").toDefinition().rawType
        assertThrows(CommandSyntaxException::class.java) { rotation.parse(StringReader("^1 2")) }
    }

    @Test
    fun `axis reads a swizzle case-insensitively and rejects a repeated letter`() {
        val axis = AxisArgument("axes").toDefinition().rawType

        assertEquals(setOf(Axis.X, Axis.Z), axis.parse(StringReader("xz")))
        assertEquals(setOf(Axis.X, Axis.Y, Axis.Z), axis.parse(StringReader("XYZ")))
        assertThrows(CommandSyntaxException::class.java) { axis.parse(StringReader("xx")) }
    }

    @Test
    fun `a sourced parse resolves tilde and caret positions against that sender, not the origin`(env: Env) {
        val instance = env.createEmptyInstance()
        val player = connect(env, instance, Pos(10.0, 20.0, 30.0, 0f, 0f))
        try {
            val position = PositionArgument("pos").toDefinition().rawType
            assertEquals(Vec(11.0, 22.0, 33.0), position.parse(StringReader("~1 ~2 ~3"), player))
            assertVecEquals(Vec(10.0, 20.0, 31.0), position.parse(StringReader("^0 ^0 ^1"), player))

            val blockPosition = BlockPositionArgument("pos").toDefinition().rawType
            assertEquals(Vec(11.5, 20.0, 30.0), blockPosition.parse(StringReader("~1.5 ~0 ~0"), player))
        } finally {
            player.remove()
            env.destroyInstance(instance)
        }
    }

    @Test
    fun `a sourced parse resolves a relative rotation against that sender's view, not the origin's`(env: Env) {
        val instance = env.createEmptyInstance()
        val player = connect(env, instance, Pos(0.0, 0.0, 0.0, 45f, 10f))
        try {
            val rotation = RotationArgument("look").toDefinition().rawType
            assertEquals(Rotation(55f, 5f), rotation.parse(StringReader("~10 ~-5"), player))
        } finally {
            player.remove()
            env.destroyInstance(instance)
        }
    }

    private fun assertVecEquals(expected: Vec, actual: Vec, epsilon: Double = 1e-9) {
        assertEquals(expected.x(), actual.x(), epsilon)
        assertEquals(expected.y(), actual.y(), epsilon)
        assertEquals(expected.z(), actual.z(), epsilon)
    }

    private fun connect(env: Env, instance: Instance, pos: Pos): Player {
        val connected = CompletableFuture<Player>()
        val profile = GameProfile(UUID.randomUUID(), "Coord-${UUID.randomUUID()}".take(16))
        Thread.startVirtualThread {
            runCatching { env.createConnection(profile).connect(instance, pos) }
                .onSuccess(connected::complete)
                .onFailure(connected::completeExceptionally)
        }
        val player = connected.join()
        check(env.tickWhile({ player.instance !== instance }, Duration.ofSeconds(1))) {
            "Player was not spawned in the requested test instance"
        }
        return player
    }
}
