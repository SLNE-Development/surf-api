package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.CommandTree
import dev.slne.surf.api.minestom.command.argument.EntitiesArgument
import dev.slne.surf.api.minestom.command.argument.EntityArgument
import dev.slne.surf.api.minestom.command.argument.EntityTypeArgument
import dev.slne.surf.api.minestom.command.argument.PlayerArgument
import dev.slne.surf.api.minestom.command.argument.PlayersArgument
import dev.slne.surf.api.minestom.command.argument.StringArgument
import dev.slne.surf.api.minestom.command.argument.map
import dev.slne.surf.api.minestom.command.dsl.entitiesArgument
import dev.slne.surf.api.minestom.command.dsl.entityArgument
import dev.slne.surf.api.minestom.command.dsl.entityTypeArgument
import dev.slne.surf.api.minestom.command.dsl.playerArgument
import dev.slne.surf.api.minestom.command.dsl.playersArgument
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.minestom.command.exception.CommandValidationException
import dev.slne.surf.api.minestom.command.executor.CommandArguments
import dev.slne.surf.api.minestom.command.dsl.anyExecutionInfo
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.suggestion.ArgumentSuggestions
import dev.slne.surf.api.minestom.command.suggestion.SafeSuggestions
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minestom.server.command.ArgumentParserType
import net.minestom.server.command.builder.CommandResult
import net.minestom.server.command.builder.arguments.minecraft.SuggestionType
import net.minestom.server.command.builder.exception.ArgumentSyntaxException
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.network.packet.server.play.SystemChatPacket
import net.minestom.server.network.player.GameProfile
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestConnection
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

@EnvTest
class EntityArgumentIntegrationTest {
    @Test
    fun `entity selectors declare the vanilla entity parser and entity type asks the client`() {
        listOf(
            PlayerArgument("player").toDefinition() to byteArrayOf(3),
            PlayersArgument("players").toDefinition() to byteArrayOf(2),
            EntityArgument("entity").toDefinition() to byteArrayOf(1),
            EntitiesArgument("entities").toDefinition() to byteArrayOf(0),
        ).forEach { (definition, properties) ->
            val declaration = definition.declaration()
            assertEquals(ArgumentParserType.ENTITY, declaration.parser, definition.nodeName)
            assertArrayEquals(properties, declaration.properties, definition.nodeName)
        }

        val entityType = EntityTypeArgument("type").toDefinition().declaration()
        assertEquals(ArgumentParserType.RESOURCE_LOCATION, entityType.parser)
        assertEquals(SuggestionType.SUMMONABLE_ENTITIES.identifier, entityType.suggestionsType)
    }

    @Test
    fun `an argument with a provider tells the client to ask the server`() {
        val declaration = StringArgument("value")
            .replaceSuggestions(ArgumentSuggestions.strings("one"))
            .toDefinition()
            .declaration()

        assertEquals(SuggestionType.ASK_SERVER.identifier, declaration.suggestionsType)
    }

    @Test
    fun `entity type include modes are rejected when native summonable entries cannot be reproduced`() {
        val unsafe = EntityTypeArgument("type")
            .includeSuggestions(ArgumentSuggestions.strings("minecraft:zombie"))
        val safe = EntityTypeArgument("type")
            .includeSafeSuggestions(SafeSuggestions.suggest(EntityType.ZOMBIE))
        val mapped = EntityTypeArgument("type")
            .map(EntityType::name)
            .includeSuggestions(ArgumentSuggestions.strings("minecraft:zombie"))

        listOf(unsafe, safe, mapped).forEach { argument ->
            val failure = assertThrows(CommandValidationException::class.java) {
                MinestomCommandCompiler().compile(
                    CommandAPICommand("entity-type-include")
                        .withArguments(argument)
                        .anyExecutor { _, _ -> }
                        .toDefinition(),
                    namespace = null,
                )
            }
            assertEquals(
                "EntityType argument 'type' cannot include custom suggestions because " +
                    "Minestom does not expose the native summonable entity set; use built-ins or replace suggestions",
                failure.message,
            )
        }
    }

    @Test
    fun `native player and entity selectors resolve typed values against the execution sender`(env: Env) {
        withEntityPlatform(env) { manager, _, alpha, bravo, zombie ->
            val onePlayer = AtomicReference<Player>()
            val players = mutableListOf<List<Player>>()
            val oneEntity = AtomicReference<Entity>()
            val entities = mutableListOf<List<Entity>>()

            CommandAPICommand("one-player")
                .withArguments(PlayerArgument("target"))
                .anyExecutionInfo { info ->
                    val delegated = PlayerArguments(info.args)
                    assertSame(delegated.target, info.args.get<Player>("target"))
                    onePlayer.set(delegated.target)
                }
                .register()
            CommandAPICommand("many-players")
                .withArguments(PlayersArgument("targets"))
                .anyExecutor { _, arguments -> players += arguments.get<List<Player>>("targets") }
                .register()
            CommandAPICommand("one-entity")
                .withArguments(EntityArgument("target"))
                .anyExecutor { _, arguments -> oneEntity.set(arguments.get("target")) }
                .register()
            CommandAPICommand("many-entities")
                .withArguments(EntitiesArgument("targets"))
                .anyExecutor { _, arguments -> entities += arguments.get<List<Entity>>("targets") }
                .register()

            assertTrue(runCommand(alpha, "one-player Alpha"))
            assertSame(alpha, onePlayer.get())
            assertFalse(runCommand(alpha, "one-player ${bravo.uuid}"))
            assertTrue(runCommand(alpha, "one-player @s"))
            assertSame(alpha, onePlayer.get())

            assertTrue(runCommand(alpha, "many-players @a"))
            assertTrue(runCommand(alpha, "many-players @a"))
            assertEquals(2, players.size)
            assertEquals(players[0], players[1])
            assertEquals(setOf(alpha, bravo), players[0].toSet())

            assertTrue(runCommand(alpha, "one-entity ${zombie.uuid}"))
            assertSame(zombie, oneEntity.get())
            assertTrue(runCommand(alpha, "one-entity @s"))
            assertSame(alpha, oneEntity.get())

            assertTrue(runCommand(alpha, "many-entities @e"))
            assertTrue(runCommand(alpha, "many-entities @e"))
            assertEquals(2, entities.size)
            assertEquals(entities[0], entities[1])
            assertEquals(setOf(alpha, bravo, zombie), entities[0].toSet())
        }
    }

    @Test
    fun `single cardinality player filtering and collection empty policy fail with components`(env: Env) {
        withEntityPlatform(env) { manager, alphaConnection, alpha, _, _ ->
            val executions = mutableListOf<String>()
            CommandAPICommand("single-player")
                .withArguments(PlayerArgument("target"))
                .anyExecutor { _, _ -> executions += "single-player" }
                .register()
            CommandAPICommand("single-entity")
                .withArguments(EntityArgument("target"))
                .anyExecutor { _, _ -> executions += "single-entity" }
                .register()
            CommandAPICommand("players-required")
                .withArguments(PlayersArgument("targets", allowEmpty = false))
                .anyExecutor { _, _ -> executions += "players-required" }
                .register()
            CommandAPICommand("players-empty")
                .withArguments(PlayersArgument("targets", allowEmpty = true))
                .anyExecutor { _, arguments ->
                    assertEquals(emptyList<Player>(), arguments.get<List<Player>>("targets"))
                    executions += "players-empty"
                }
                .register()
            CommandAPICommand("entities-required")
                .withArguments(EntitiesArgument("targets", allowEmpty = false))
                .anyExecutor { _, _ -> executions += "entities-required" }
                .register()
            CommandAPICommand("entities-empty")
                .withArguments(EntitiesArgument("targets", allowEmpty = true))
                .anyExecutor { _, arguments ->
                    assertEquals(emptyList<Entity>(), arguments.get<List<Entity>>("targets"))
                    executions += "entities-empty"
                }
                .register()

            val messages = alphaConnection.trackIncoming(SystemChatPacket::class.java)
            assertFalse(runCommandReporting(alpha, "single-player Missing"))
            assertFalse(runCommandReporting(alpha, "single-entity ${UUID.randomUUID()}"))
            assertFalse(runCommandReporting(alpha, "single-player @p[gamemode=spectator]"))
            assertFalse(runCommandReporting(alpha, "single-player @p[limit=2]"))
            assertFalse(runCommandReporting(alpha, "single-player @a"))
            assertFalse(runCommandReporting(alpha, "single-entity @e"))
            assertFalse(runCommandReporting(alpha, "players-required @e"))
            assertFalse(runCommandReporting(alpha, "players-required @a[gamemode=spectator]"))
            assertTrue(runCommandReporting(alpha, "players-empty @a[gamemode=spectator]"))
            assertFalse(runCommandReporting(alpha, "entities-required @e[type=minecraft:creeper]"))
            assertTrue(runCommandReporting(alpha, "entities-empty @e[type=minecraft:creeper]"))

            assertEquals(listOf("players-empty", "entities-empty"), executions)
            val rendered = messages.collect().map { packet -> plain(packet.message) }
            assertEquals(3, rendered.count { it.contains("No entity matched the selector") })
            assertEquals(2, rendered.count { it.contains("No entities matched the selector") })
            assertEquals(2, rendered.count { it.contains("Expected a single player") })
            assertEquals(1, rendered.count { it.contains("Expected a single entity") })
            assertEquals(1, rendered.count { it.contains("Only players can be targeted") })

            val player = PlayerArgument("target").toDefinition()
            val entity = EntityArgument("target").toDefinition()

            val noTarget = assertThrows(CommandSyntaxException::class.java) {
                player.read(alpha, "Missing")
            }
            assertEquals("No entity matched the selector", noTarget.rawMessage.string)
            assertThrows(CommandSyntaxException::class.java) { player.read(alpha, "@a") }
            assertThrows(CommandSyntaxException::class.java) { entity.read(alpha, "@e") }

            // The console has no instance to resolve a UUID against, so nothing matches.
            val consoleUuid = assertThrows(CommandSyntaxException::class.java) {
                entity.read(env.process().command().consoleSender, UUID.randomUUID().toString())
            }
            assertEquals("No entity matched the selector", consoleUuid.rawMessage.string)
        }
    }

    @Test
    fun `entity type parsing and all Kotlin DSL receivers keep typed definitions and defaults`(env: Env) {
        withEntityPlatform(env) { manager, _, alpha, bravo, zombie ->
            val type = AtomicReference<EntityType>()
            CommandAPICommand("entity-type")
                .withArguments(EntityTypeArgument("type"))
                .anyExecutor { _, arguments -> type.set(arguments.get("type")) }
                .register()

            assertTrue(runCommand(alpha, "entity-type minecraft:zombie"))
            assertSame(EntityType.ZOMBIE, type.get())
            assertFalse(runCommand(alpha, "entity-type minecraft:not_real"))

            val command = CommandAPICommand("entity-dsl-command")
                .playerArgument("player")
                .playersArgument("players")
                .entityArgument("entity")
                .entitiesArgument("entities")
                .entityTypeArgument("type")
                .anyExecutor { _, _ -> }
                .toDefinition()
            val tree = CommandTree("entity-dsl-tree")
                .playerArgument("player") { anyExecutor { _, _ -> } }
                .playersArgument("players") { anyExecutor { _, _ -> } }
                .entityArgument("entity") { anyExecutor { _, _ -> } }
                .entitiesArgument("entities") { anyExecutor { _, _ -> } }
                .entityTypeArgument("type") {
                    setOptional(EntityType.ZOMBIE)
                    anyExecutor { _, _ -> }
                }
                .toDefinition()
            val child = CommandTree("entity-dsl-child")
                .then(
                    StringArgument("prefix")
                        .playerArgument("player") { anyExecutor { _, _ -> } }
                        .playersArgument("players") { anyExecutor { _, _ -> } }
                        .entityArgument("entity") { anyExecutor { _, _ -> } }
                        .entitiesArgument("entities") { anyExecutor { _, _ -> } }
                        .entityTypeArgument("type") {
                            setOptional(EntityType.ZOMBIE)
                            anyExecutor { _, _ -> }
                        },
                )
                .toDefinition()

            assertEquals(
                listOf("player", "players", "entity", "entities", "type"),
                command.paths.single().arguments.map { it.nodeName },
            )
            assertEquals(
                listOf(
                    listOf("player"),
                    listOf("players"),
                    listOf("entity"),
                    listOf("entities"),
                    listOf("type"),
                ),
                tree.paths.map { path -> path.arguments.map { it.nodeName } },
            )
            assertEquals(
                listOf(
                    listOf("prefix", "player"),
                    listOf("prefix", "players"),
                    listOf("prefix", "entity"),
                    listOf("prefix", "entities"),
                    listOf("prefix", "type"),
                ),
                child.paths.map { path -> path.arguments.map { it.nodeName } },
            )
            listOf(tree, child).forEach { definition ->
                val last = definition.paths.single { path ->
                    path.arguments.last().nodeName == "type"
                }.arguments.last()
                assertTrue(last.optional)
                assertSame(EntityType.ZOMBIE, last.defaultValue?.invoke(alpha))
            }

            assertEquals("Alpha", PlayerArgument("player").toDefinition().stringify(alpha))
            assertEquals("Alpha,Bravo", PlayersArgument("players").toDefinition().stringify(listOf(alpha, bravo)))
            assertEquals(zombie.uuid.toString(), EntityArgument("entity").toDefinition().stringify(zombie))
            assertEquals("minecraft:zombie", EntityTypeArgument("type").toDefinition().stringify(EntityType.ZOMBIE))
        }
    }

    private inline fun withEntityPlatform(
        env: Env,
        block: (
            net.minestom.server.command.CommandManager,
            TestConnection,
            Player,
            Player,
            Entity,
        ) -> Unit,
    ) {
        val manager = env.process().command()
        val platform = MinestomCommandAPIPlatform(manager, MinestomCommandOwnership())
        val instance = env.createEmptyInstance()
        val alphaConnection = env.createConnection(profile("Alpha"))
        var alpha: Player? = null
        var bravo: Player? = null
        var zombie: Entity? = null
        CommandAPI.installPlatform(platform)
        try {
            val connectedAlpha = connect(alphaConnection, instance)
            alpha = connectedAlpha
            check(env.tickWhile({ connectedAlpha.instance !== instance }, Duration.ofSeconds(1))) {
                "Alpha was not spawned in the requested test instance"
            }
            val connectedBravo = connect(env.createConnection(profile("Bravo")), instance)
            bravo = connectedBravo
            check(env.tickWhile({ connectedBravo.instance !== instance }, Duration.ofSeconds(1))) {
                "Bravo was not spawned in the requested test instance"
            }
            zombie = Entity(EntityType.ZOMBIE).also { entity ->
                entity.setInstance(instance, Pos(4.0, 0.0, 0.0)).join()
            }
            block(manager, alphaConnection, alpha, bravo, zombie)
        } finally {
            zombie?.remove()
            bravo?.remove()
            alpha?.remove()
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

    private fun profile(name: String) = GameProfile(UUID.randomUUID(), name)

    private fun plain(component: Component): String =
        PlainTextComponentSerializer.plainText().serialize(component)

    private class PlayerArguments(arguments: CommandArguments) {
        val target: Player by arguments
    }
}
