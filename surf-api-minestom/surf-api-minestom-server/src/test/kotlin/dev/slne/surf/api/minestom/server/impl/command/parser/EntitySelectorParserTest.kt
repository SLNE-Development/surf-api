package dev.slne.surf.api.minestom.server.impl.command.parser

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.minestom.command.argument.EntitiesArgument
import dev.slne.surf.api.minestom.command.argument.EntityArgument
import dev.slne.surf.api.minestom.command.argument.PlayerArgument
import dev.slne.surf.api.minestom.command.argument.PlayersArgument
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.network.player.GameProfile
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestConnection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * The parser and selector are `internal` to the api module (see [ScalarParsersTest] for why), so
 * every case here drives them through [PlayerArgument], [PlayersArgument], [EntityArgument] and
 * [EntitiesArgument]'s `rawType`, asserting on the entities actually resolved rather than the
 * selector's internal fields.
 */
@EnvTest
class EntitySelectorParserTest {
    @Test
    fun `at-s selects the sender`(env: Env) {
        withPlayers(env, 2) { _, players ->
            val raw = EntityArgument("target").toDefinition().rawType
            assertSame(players[0], raw.parse(StringReader("@s"), players[0]))
        }
    }

    @Test
    fun `at-p yields exactly one even with several players online`(env: Env) {
        withPlayers(env, 3) { _, players ->
            val raw = EntitiesArgument("targets").toDefinition().rawType
            val result = raw.parse(StringReader("@p"), players[0])
            assertEquals(1, result.size)
        }
    }

    @Test
    fun `limit option caps the results of at-e`(env: Env) {
        withPlayers(env, 3) { _, players ->
            val raw = EntitiesArgument("targets").toDefinition().rawType
            val result = raw.parse(StringReader("@e[limit=2]"), players[0])
            assertEquals(2, result.size)
        }
    }

    @Test
    fun `sort=nearest orders results by distance to the sender`(env: Env) {
        withPlayers(env, 1) { instance, players ->
            val near = Entity(EntityType.ZOMBIE)
            val far = Entity(EntityType.ZOMBIE)
            near.setInstance(instance, Pos(2.0, 0.0, 0.0)).join()
            far.setInstance(instance, Pos(20.0, 0.0, 0.0)).join()
            try {
                val raw = EntitiesArgument("targets").toDefinition().rawType
                val result = raw.parse(StringReader("@e[type=minecraft:zombie,sort=nearest]"), players[0])
                assertEquals(listOf(near, far), result)
            } finally {
                near.remove()
                far.remove()
            }
        }
    }

    @Test
    fun `a single-entity argument rejects a selector that can match many`(env: Env) {
        withPlayers(env, 2) { _, players ->
            val raw = EntityArgument("target").toDefinition().rawType
            val failure = assertThrows(CommandSyntaxException::class.java) {
                raw.parse(StringReader("@e"), players[0])
            }
            assertEquals(0, failure.cursor)
        }
    }

    @Test
    fun `an unknown option is rejected at the option's start`(env: Env) {
        withPlayers(env, 1) { _, players ->
            val raw = EntitiesArgument("targets").toDefinition().rawType
            val input = "@e[nonsense=1]"
            val failure = assertThrows(CommandSyntaxException::class.java) {
                raw.parse(StringReader(input), players[0])
            }
            assertEquals(input.indexOf("nonsense"), failure.cursor)
        }
    }

    @Test
    fun `nbt is rejected by name as unsupported rather than as an unknown option`(env: Env) {
        withPlayers(env, 1) { _, players ->
            val raw = EntitiesArgument("targets").toDefinition().rawType
            val input = "@e[nbt={foo:1}]"
            val failure = assertThrows(CommandSyntaxException::class.java) {
                raw.parse(StringReader(input), players[0])
            }
            assertTrue(failure.message?.contains("unsupported", ignoreCase = true) == true, failure.message)
            assertEquals(input.indexOf("nbt"), failure.cursor)
        }
    }

    @Test
    fun `a bare name resolves to the matching player`(env: Env) {
        withPlayers(env, 2) { _, players ->
            val raw = PlayerArgument("target").toDefinition().rawType
            assertSame(players[1], raw.parse(StringReader(players[1].username), players[0]))
        }
    }

    @Test
    fun `a bare uuid resolves to the matching entity`(env: Env) {
        withPlayers(env, 1) { instance, players ->
            val zombie = Entity(EntityType.ZOMBIE)
            zombie.setInstance(instance, Pos(3.0, 0.0, 0.0)).join()
            try {
                val raw = EntityArgument("target").toDefinition().rawType
                assertSame(zombie, raw.parse(StringReader(zombie.uuid.toString()), players[0]))
            } finally {
                zombie.remove()
            }
        }
    }

    @Test
    fun `players argument resolves every online player and rejects an empty match`(env: Env) {
        withPlayers(env, 2) { _, players ->
            val raw = PlayersArgument("targets").toDefinition().rawType
            assertEquals(players.toSet(), raw.parse(StringReader("@a"), players[0]).toSet())

            assertThrows(CommandSyntaxException::class.java) {
                raw.parse(StringReader("@a[gamemode=creative]"), players[0])
            }
        }
    }

    @Test
    fun `resolving without a command source is rejected`(env: Env) {
        withPlayers(env, 1) { _, _ ->
            val raw = EntityArgument("target").toDefinition().rawType
            assertThrows(CommandSyntaxException::class.java) { raw.parse(StringReader("@s")) }
        }
    }

    @Test
    fun `tag option matches nothing when no entity carries it and everything for an empty tag`(env: Env) {
        withPlayers(env, 2) { _, players ->
            val allowEmpty = EntitiesArgument("targets", allowEmpty = true).toDefinition().rawType
            assertEquals(emptyList<Entity>(), allowEmpty.parse(StringReader("@e[tag=foo]"), players[0]))

            val required = EntitiesArgument("targets").toDefinition().rawType
            assertEquals(players.toSet(), required.parse(StringReader("@e[tag=]"), players[0]).toSet())
        }
    }

    @Test
    fun `team option matches nobody when no team has been created`(env: Env) {
        withPlayers(env, 1) { _, players ->
            val raw = EntitiesArgument("targets", allowEmpty = true).toDefinition().rawType
            assertEquals(emptyList<Entity>(), raw.parse(StringReader("@e[team=red]"), players[0]))
        }
    }

    @Test
    fun `dx dy dz select the single block at the origin, extended by one on the positive side`(env: Env) {
        withPlayers(env, 1) { instance, players ->
            val inBlock = Entity(EntityType.ZOMBIE)
            val outsideBlock = Entity(EntityType.ZOMBIE)
            inBlock.setInstance(instance, Pos(0.5, 0.0, 0.5)).join()
            outsideBlock.setInstance(instance, Pos(5.0, 0.0, 0.0)).join()
            try {
                val raw = EntitiesArgument("targets").toDefinition().rawType
                val result = raw.parse(StringReader("@e[type=minecraft:zombie,dx=0,dy=0,dz=0]"), players[0])
                assertEquals(listOf(inBlock), result)
            } finally {
                inBlock.remove()
                outsideBlock.remove()
            }
        }
    }

    @Test
    fun `distance option filters by distance from the sender and rejects an inverted range`(env: Env) {
        withPlayers(env, 1) { instance, players ->
            val near = Entity(EntityType.ZOMBIE)
            val far = Entity(EntityType.ZOMBIE)
            near.setInstance(instance, Pos(2.0, 0.0, 0.0)).join()
            far.setInstance(instance, Pos(20.0, 0.0, 0.0)).join()
            try {
                val raw = EntitiesArgument("targets").toDefinition().rawType
                val result = raw.parse(StringReader("@e[type=minecraft:zombie,distance=..5]"), players[0])
                assertEquals(listOf(near), result)

                val input = "@e[type=minecraft:zombie,distance=5..1]"
                val failure = assertThrows(CommandSyntaxException::class.java) {
                    raw.parse(StringReader(input), players[0])
                }
                assertEquals(input.indexOf("5..1"), failure.cursor)
            } finally {
                near.remove()
                far.remove()
            }
        }
    }

    @Test
    fun `type and tag options support negation`(env: Env) {
        withPlayers(env, 1) { instance, players ->
            val zombie = Entity(EntityType.ZOMBIE)
            zombie.setInstance(instance, Pos(2.0, 0.0, 0.0)).join()
            try {
                val raw = EntitiesArgument("targets").toDefinition().rawType

                val excludingZombies = raw.parse(StringReader("@e[type=!minecraft:zombie]"), players[0])
                assertEquals(players.toSet(), excludingZombies.toSet())

                val everyone = buildSet<Entity> {
                    addAll(players)
                    add(zombie)
                }
                val includingEveryone = raw.parse(StringReader("@e[tag=!foo]"), players[0])
                assertEquals(everyone, includingEveryone.toSet())
            } finally {
                zombie.remove()
            }
        }
    }

    @Test
    fun `scores option consumes its grammar then is rejected as unsupported`(env: Env) {
        withPlayers(env, 1) { _, players ->
            val raw = EntitiesArgument("targets").toDefinition().rawType
            val input = "@e[scores={foo=1..3}]"
            val failure = assertThrows(CommandSyntaxException::class.java) {
                raw.parse(StringReader(input), players[0])
            }
            assertTrue(failure.message?.contains("unsupported", ignoreCase = true) == true, failure.message)
            assertEquals(input.indexOf("scores"), failure.cursor)
        }
    }

    @Test
    fun `a malformed scores value is rejected before the unsupported check`(env: Env) {
        withPlayers(env, 1) { _, players ->
            val raw = EntitiesArgument("targets").toDefinition().rawType
            val failure = assertThrows(CommandSyntaxException::class.java) {
                raw.parse(StringReader("@e[scores={foo=bar}]"), players[0])
            }
            assertFalse(failure.message?.contains("unsupported", ignoreCase = true) == true, failure.message)
        }
    }

    @Test
    fun `advancements option consumes its grammar, including nested criteria, then is rejected as unsupported`(env: Env) {
        withPlayers(env, 1) { _, players ->
            val raw = EntitiesArgument("targets").toDefinition().rawType
            val input = "@e[advancements={story/root={met_villager=true}}]"
            val failure = assertThrows(CommandSyntaxException::class.java) {
                raw.parse(StringReader(input), players[0])
            }
            assertTrue(failure.message?.contains("unsupported", ignoreCase = true) == true, failure.message)
            assertEquals(input.indexOf("advancements"), failure.cursor)
        }
    }

    @Test
    fun `a malformed advancements value is rejected before the unsupported check`(env: Env) {
        withPlayers(env, 1) { _, players ->
            val raw = EntitiesArgument("targets").toDefinition().rawType
            val failure = assertThrows(CommandSyntaxException::class.java) {
                raw.parse(StringReader("@e[advancements={story/root true}]"), players[0])
            }
            assertFalse(failure.message?.contains("unsupported", ignoreCase = true) == true, failure.message)
        }
    }

    @Test
    fun `predicate option is rejected as unsupported`(env: Env) {
        withPlayers(env, 1) { _, players ->
            val raw = EntitiesArgument("targets").toDefinition().rawType
            val input = "@e[predicate=my:condition]"
            val failure = assertThrows(CommandSyntaxException::class.java) {
                raw.parse(StringReader(input), players[0])
            }
            assertTrue(failure.message?.contains("unsupported", ignoreCase = true) == true, failure.message)
            assertEquals(input.indexOf("predicate"), failure.cursor)
        }
    }

    @Test
    fun `repeating a single-use option is rejected at the repeat's start`(env: Env) {
        withPlayers(env, 1) { _, players ->
            val raw = EntitiesArgument("targets").toDefinition().rawType
            val input = "@e[limit=1,limit=5]"
            val failure = assertThrows(CommandSyntaxException::class.java) {
                raw.parse(StringReader(input), players[0])
            }
            assertEquals(input.lastIndexOf("limit"), failure.cursor)
        }
    }

    @Test
    fun `mixing two positive values for an invertable option is rejected, naming the option`(env: Env) {
        withPlayers(env, 1) { _, players ->
            val raw = EntitiesArgument("targets").toDefinition().rawType
            val input = "@e[type=minecraft:zombie,type=minecraft:pig]"
            // Cursor position is intentionally not asserted here: vanilla rejects this at the
            // second `type` key's start (before `!` is even read), since its InvertableSetOptionState
            // check runs before the modifier does. This port's requireInvertableUse runs inside the
            // modifier and rejects at the value's start instead - a known, accepted divergence in
            // underline column, not in outcome. Pinning the column would lock that divergence in.
            val failure = assertThrows(CommandSyntaxException::class.java) {
                raw.parse(StringReader(input), players[0])
            }
            assertTrue(failure.message?.contains("type") == true, failure.message)
        }
    }

    @Test
    fun `limit and sort are inapplicable on a self selector`(env: Env) {
        withPlayers(env, 1) { _, players ->
            val raw = EntityArgument("target").toDefinition().rawType
            val input = "@s[limit=5]"
            val failure = assertThrows(CommandSyntaxException::class.java) {
                raw.parse(StringReader(input), players[0])
            }
            assertEquals(input.indexOf("limit"), failure.cursor)

            assertThrows(CommandSyntaxException::class.java) {
                raw.parse(StringReader("@s[sort=nearest]"), players[0])
            }
        }
    }

    private fun withPlayers(env: Env, count: Int, block: (Instance, List<Player>) -> Unit) {
        val instance = env.createEmptyInstance()
        val players = mutableListOf<Player>()
        try {
            repeat(count) { index ->
                val player = connect(env.createConnection(profile("Player$index")), instance)
                check(env.tickWhile({ player.instance !== instance }, Duration.ofSeconds(1))) {
                    "Player$index was not spawned in the requested test instance"
                }
                players += player
            }
            block(instance, players)
        } finally {
            players.forEach(Player::remove)
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

    private fun profile(name: String) = GameProfile(UUID.randomUUID(), name)
}
