package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.CommandTree
import dev.slne.surf.api.minestom.command.argument.BiomeArgument
import dev.slne.surf.api.minestom.command.argument.EnchantmentArgument
import dev.slne.surf.api.minestom.command.argument.InstanceArgument
import dev.slne.surf.api.minestom.command.argument.PotionEffectArgument
import dev.slne.surf.api.minestom.command.argument.ResourceArgument
import dev.slne.surf.api.minestom.command.argument.SoundArgument
import dev.slne.surf.api.minestom.command.argument.StringArgument
import dev.slne.surf.api.minestom.command.dsl.biomeArgument
import dev.slne.surf.api.minestom.command.dsl.enchantmentArgument
import dev.slne.surf.api.minestom.command.dsl.instanceArgument
import dev.slne.surf.api.minestom.command.dsl.potionEffectArgument
import dev.slne.surf.api.minestom.command.dsl.resourceArgument
import dev.slne.surf.api.minestom.command.dsl.soundArgument
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.suggestion.ArgumentSuggestions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import net.minestom.server.command.ArgumentParserType
import net.minestom.server.command.CommandManager
import net.minestom.server.command.builder.CommandResult
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.event.EventNode
import net.minestom.server.instance.Instance
import net.minestom.server.network.packet.client.play.ClientTabCompletePacket
import net.minestom.server.network.packet.server.play.TabCompletePacket
import net.minestom.server.network.player.GameProfile
import net.minestom.server.potion.PotionEffect
import net.minestom.server.registry.BuiltinRegistries
import net.minestom.server.registry.DynamicRegistry
import net.minestom.server.sound.SoundEvent
import net.minestom.server.world.DimensionType
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestConnection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CompletableFuture

@OptIn(ExperimentalCoroutinesApi::class)
@EnvTest
class RegistryArgumentIntegrationTest {
    @Test
    fun `sound argument resolves builtin sounds and rejects unknown keys`(env: Env) {
        val sender = env.process().command().consoleSender
        val compiled = SoundArgument("sound").toDefinition()

        val pickup = SoundEvent.fromKey("minecraft:entity.experience_orb.pickup")
        assertEquals(pickup, compiled.read(sender, "minecraft:entity.experience_orb.pickup"))
        val failure = assertThrows(CommandSyntaxException::class.java) {
            compiled.read(sender, "minecraft:not_a_real_sound")
        }
        assertEquals("Unknown sound event 'minecraft:not_a_real_sound'", failure.rawMessage.string)

        assertEquals(ArgumentParserType.RESOURCE_LOCATION, compiled.declaration().parser)
        assertNull(compiled.declaration().properties)
        assertEquals(
            "minecraft:entity.experience_orb.pickup",
            SoundArgument("sound").toDefinition().stringify(pickup!!),
        )
    }

    @Test
    fun `potion effect argument resolves builtin effects and rejects unknown keys`(env: Env) {
        val sender = env.process().command().consoleSender
        val compiled = PotionEffectArgument("effect").toDefinition()

        val speed = PotionEffect.fromKey("minecraft:speed")
        assertEquals(speed, compiled.read(sender, "minecraft:speed"))
        val failure = assertThrows(CommandSyntaxException::class.java) {
            compiled.read(sender, "minecraft:not_a_real_effect")
        }
        assertEquals("Unknown potion effect 'minecraft:not_a_real_effect'", failure.rawMessage.string)

        assertEquals(ArgumentParserType.RESOURCE_LOCATION, compiled.declaration().parser)
        assertNull(compiled.declaration().properties)
        assertEquals("minecraft:speed", PotionEffectArgument("effect").toDefinition().stringify(speed!!))
    }

    @Test
    fun `biome argument resolves dynamic registry keys and rejects unknown ones`(env: Env) {
        val sender = env.process().command().consoleSender
        val compiled = BiomeArgument("biome").toDefinition()
        val identifier = BuiltinRegistries.BIOME.name()

        val plains = env.process().registries().biome().getKey(Key.key("minecraft:plains"))
        assertEquals(plains, compiled.read(sender, "minecraft:plains"))
        val failure = assertThrows(CommandSyntaxException::class.java) {
            compiled.read(sender, "minecraft:not_a_real_biome")
        }
        assertEquals("Unknown biome 'minecraft:not_a_real_biome'", failure.rawMessage.string)
        val malformed = assertThrows(CommandSyntaxException::class.java) {
            compiled.read(sender, "Not_Valid!")
        }
        assertEquals("Invalid identifier", malformed.rawMessage.string)

        assertEquals(ArgumentParserType.RESOURCE, compiled.declaration().parser)
        assertEquals(
            ArgumentType.Resource("expected", identifier).nodeProperties()!!.toList(),
            compiled.declaration().properties!!.toList(),
        )
        assertEquals("minecraft:plains", BiomeArgument("biome").toDefinition().stringify(plains!!))
    }

    @Test
    fun `enchantment argument resolves dynamic registry keys and rejects unknown ones`(env: Env) {
        val sender = env.process().command().consoleSender
        val compiled = EnchantmentArgument("enchant").toDefinition()
        val identifier = BuiltinRegistries.ENCHANTMENT.name()

        val sharpness = env.process().registries().enchantment().getKey(Key.key("minecraft:sharpness"))
        assertEquals(sharpness, compiled.read(sender, "minecraft:sharpness"))
        val failure = assertThrows(CommandSyntaxException::class.java) {
            compiled.read(sender, "minecraft:not_a_real_enchantment")
        }
        assertEquals("Unknown enchantment 'minecraft:not_a_real_enchantment'", failure.rawMessage.string)

        assertEquals(ArgumentParserType.RESOURCE, compiled.declaration().parser)
        assertEquals(
            ArgumentType.Resource("expected", identifier).nodeProperties()!!.toList(),
            compiled.declaration().properties!!.toList(),
        )
    }

    @Test
    fun `generic resource argument resolves against a caller supplied registry and observes later registrations`(
        env: Env,
    ) {
        val sender = env.process().command().consoleSender
        val registry = DynamicRegistry.fromMap<String>(Key.key("test:samples"))
        val compiled = ResourceArgument("sample", "test:sample", registry).toDefinition()

        val malformed = assertThrows(CommandSyntaxException::class.java) {
            compiled.read(sender, "Not_Valid!")
        }
        assertEquals("Invalid identifier", malformed.rawMessage.string)
        val missing = assertThrows(CommandSyntaxException::class.java) {
            compiled.read(sender, "test:missing")
        }
        assertEquals("Unknown test:sample 'test:missing'", missing.rawMessage.string)

        val key = registry.register("test:late_entry", "late-value")
        assertEquals(key, compiled.read(sender, "test:late_entry"))

        assertEquals(ArgumentParserType.RESOURCE, compiled.declaration().parser)
        assertEquals(
            ArgumentType.Resource("expected", "test:sample").nodeProperties()!!.toList(),
            compiled.declaration().properties!!.toList(),
        )
    }

    @Test
    fun `instance argument resolves by uuid and unique dimension name and rejects ambiguous shared names`(
        env: Env,
    ) {
        val sender = env.process().command().consoleSender
        val compiled = InstanceArgument("instance").toDefinition()
        val overworld = env.process().instance().createInstanceContainer(DimensionType.OVERWORLD)
        val nether = env.process().instance().createInstanceContainer(DimensionType.THE_NETHER)
        try {
            assertEquals(overworld, compiled.read(sender, overworld.uuid.toString()))
            assertEquals(nether, compiled.read(sender, nether.uuid.toString()))
            assertEquals(overworld, compiled.read(sender, "minecraft:overworld"))
            assertEquals(nether, compiled.read(sender, "minecraft:the_nether"))
            val notFound = assertThrows(CommandSyntaxException::class.java) {
                compiled.read(sender, "not-a-real-instance")
            }
            assertEquals(
                "Unknown or ambiguous instance 'not-a-real-instance'; use its UUID",
                notFound.rawMessage.string,
            )

            val secondOverworld = env.process().instance().createInstanceContainer(DimensionType.OVERWORLD)
            try {
                val ambiguous = assertThrows(CommandSyntaxException::class.java) {
                    compiled.read(sender, "minecraft:overworld")
                }
                assertEquals(
                    "Unknown or ambiguous instance 'minecraft:overworld'; use its UUID",
                    ambiguous.rawMessage.string,
                )
                assertEquals(overworld, compiled.read(sender, overworld.uuid.toString()))
                assertEquals(secondOverworld, compiled.read(sender, secondOverworld.uuid.toString()))
                assertEquals(nether, compiled.read(sender, "minecraft:the_nether"))
            } finally {
                env.destroyInstance(secondOverworld)
            }
        } finally {
            env.destroyInstance(nether)
            env.destroyInstance(overworld)
        }

        assertEquals(ArgumentParserType.STRING, compiled.declaration().parser)
        assertEquals(
            ArgumentType.Word("expected").nodeProperties()!!.toList(),
            compiled.declaration().properties!!.toList(),
        )
    }

    @Test
    fun `partial keys produce namespaced suggestions for sound biome and generic resource arguments`(
        env: Env,
    ) = runTest {
        withBackend(env) {
            CommandAPICommand("sound-cmd")
                .withArguments(SoundArgument("value"))
                .anyExecutor { _, _ -> }
                .register()
            CommandAPICommand("biome-cmd")
                .withArguments(BiomeArgument("value"))
                .anyExecutor { _, _ -> }
                .register()
            val registry = DynamicRegistry.fromMap<String>(
                Key.key("test:samples-2"),
                java.util.Map.entry(Key.key("test:alpha"), "alpha"),
                java.util.Map.entry(Key.key("test:beta"), "beta"),
            )
            CommandAPICommand("resource-cmd")
                .withArguments(ResourceArgument("value", "test:sample-2", registry))
                .anyExecutor { _, _ -> }
                .register()

            val soundPackets = connection.trackIncoming(TabCompletePacket::class.java)
            send(1, "/sound-cmd minecraft:entity.experience_orb.pi")
            runCurrent()
            soundPackets.assertSingle { packet ->
                assertTrue("minecraft:entity.experience_orb.pickup" in packet.matches.map { it.match })
            }

            val biomePackets = connection.trackIncoming(TabCompletePacket::class.java)
            send(2, "/biome-cmd minecraft:pla")
            runCurrent()
            biomePackets.assertSingle { packet ->
                assertEquals(listOf("minecraft:plains"), packet.matches.map { it.match })
            }

            val resourcePackets = connection.trackIncoming(TabCompletePacket::class.java)
            send(3, "/resource-cmd test:a")
            runCurrent()
            resourcePackets.assertSingle { packet ->
                assertEquals(listOf("test:alpha"), packet.matches.map { it.match })
            }
        }
    }

    @Test
    fun `instance suggestions offer every uuid and only unambiguous dimension names`(env: Env) = runTest {
        withBackend(env) {
            // withBackend's own connection spawns into an additional overworld instance, so this
            // test uses other dimensions to avoid an incidental collision with that instance's
            // dimension name.
            CommandAPICommand("instance-cmd")
                .withArguments(InstanceArgument("value"))
                .anyExecutor { _, _ -> }
                .register()

            val caves = env.process().instance().createInstanceContainer(DimensionType.OVERWORLD_CAVES)
            val end = env.process().instance().createInstanceContainer(DimensionType.THE_END)
            try {
                val packets = connection.trackIncoming(TabCompletePacket::class.java)
                send(4, "/instance-cmd ")
                runCurrent()
                packets.assertSingle { packet ->
                    val matches = packet.matches.map { it.match }
                    assertTrue(caves.uuid.toString() in matches)
                    assertTrue(end.uuid.toString() in matches)
                    assertTrue("minecraft:overworld_caves" in matches)
                    assertTrue("minecraft:the_end" in matches)
                }

                val secondCaves = env.process().instance().createInstanceContainer(DimensionType.OVERWORLD_CAVES)
                try {
                    val ambiguousPackets = connection.trackIncoming(TabCompletePacket::class.java)
                    send(5, "/instance-cmd ")
                    runCurrent()
                    ambiguousPackets.assertSingle { packet ->
                        val matches = packet.matches.map { it.match }
                        assertTrue(caves.uuid.toString() in matches)
                        assertTrue(secondCaves.uuid.toString() in matches)
                        assertTrue(end.uuid.toString() in matches)
                        assertFalse("minecraft:overworld_caves" in matches)
                        assertTrue("minecraft:the_end" in matches)
                    }
                } finally {
                    env.destroyInstance(secondCaves)
                }
            } finally {
                env.destroyInstance(end)
                env.destroyInstance(caves)
            }
        }
    }

    @Test
    fun `custom suggestion modes are permitted for every new registry backed argument`(env: Env) {
        withPlatform(env) {
            CommandAPICommand("allow-sound")
                .withArguments(SoundArgument("value").replaceSuggestions(ArgumentSuggestions.strings("extra")))
                .anyExecutor { _, _ -> }
                .register()
            CommandAPICommand("allow-potion")
                .withArguments(
                    PotionEffectArgument("value").includeSuggestions(ArgumentSuggestions.strings("extra")),
                )
                .anyExecutor { _, _ -> }
                .register()
            CommandAPICommand("allow-biome")
                .withArguments(BiomeArgument("value").replaceSuggestions(ArgumentSuggestions.strings("extra")))
                .anyExecutor { _, _ -> }
                .register()
            CommandAPICommand("allow-enchantment")
                .withArguments(
                    EnchantmentArgument("value").includeSuggestions(ArgumentSuggestions.strings("extra")),
                )
                .anyExecutor { _, _ -> }
                .register()
            CommandAPICommand("allow-instance")
                .withArguments(InstanceArgument("value").replaceSuggestions(ArgumentSuggestions.strings("extra")))
                .anyExecutor { _, _ -> }
                .register()

            assertTrue(runCommand(env.process().command().consoleSender, "allow-sound minecraft:entity.pig.ambient"))
            assertTrue(runCommand(env.process().command().consoleSender, "allow-potion minecraft:speed"))
        }
    }

    @Test
    fun `every new registry argument is reachable through the executor`(env: Env) {
        val captured = mutableMapOf<String, Any?>()
        withPlayerPlatform(env) { manager, player ->
            CommandAPICommand("sound-value-cmd").withArguments(SoundArgument("soundValue"))
                .anyExecutor { _, arguments -> captured["sound"] = arguments.get<SoundEvent>("soundValue") }
                .register()
            CommandAPICommand("potion-value-cmd").withArguments(PotionEffectArgument("potionValue"))
                .anyExecutor { _, arguments -> captured["potion"] = arguments.get<PotionEffect>("potionValue") }
                .register()
            CommandAPICommand("biome-value-cmd").withArguments(BiomeArgument("biomeValue"))
                .anyExecutor { _, arguments -> captured["biome"] = arguments.get<Any?>("biomeValue") }
                .register()

            assertTrue(runCommand(player, "sound-value-cmd minecraft:entity.experience_orb.pickup"))
            assertEquals(
                SoundEvent.fromKey("minecraft:entity.experience_orb.pickup"),
                captured["sound"],
            )

            assertTrue(runCommand(player, "potion-value-cmd minecraft:speed"))
            assertEquals(PotionEffect.fromKey("minecraft:speed"), captured["potion"])

            assertTrue(runCommand(player, "biome-value-cmd minecraft:plains"))
            assertEquals(
                env.process().registries().biome().getKey(Key.key("minecraft:plains")),
                captured["biome"],
            )

            val packet = declaredCommands(env, player)
            fun parserOf(nodeName: String) = packet.nodes.single { node -> node.name == nodeName }.parser
            assertEquals(ArgumentParserType.RESOURCE_LOCATION, parserOf("soundValue"))
            assertEquals(ArgumentParserType.RESOURCE_LOCATION, parserOf("potionValue"))
            assertEquals(ArgumentParserType.RESOURCE, parserOf("biomeValue"))
        }
    }

    @Test
    fun `all six registry argument DSL builders register across command tree and child argument receivers`() {
        val customRegistry = DynamicRegistry.fromMap<String>(Key.key("test:dsl-samples"))
        val command = CommandAPICommand("registry-dsl-command")
            .soundArgument("sound")
            .potionEffectArgument("potion")
            .biomeArgument("biome")
            .enchantmentArgument("enchantment")
            .resourceArgument("resource", "test:dsl-sample", customRegistry)
            .instanceArgument("instance")
            .anyExecutor { _, _ -> }
            .toDefinition()

        val tree = CommandTree("registry-dsl-tree")
            .soundArgument("sound") { anyExecutor { _, _ -> } }
            .potionEffectArgument("potion") { anyExecutor { _, _ -> } }
            .biomeArgument("biome") { anyExecutor { _, _ -> } }
            .enchantmentArgument("enchantment") { anyExecutor { _, _ -> } }
            .resourceArgument("resource", "test:dsl-sample", customRegistry) { anyExecutor { _, _ -> } }
            .instanceArgument("instance") { anyExecutor { _, _ -> } }
            .toDefinition()

        val child = CommandTree("registry-dsl-child")
            .then(
                StringArgument("prefix")
                    .soundArgument("sound") { anyExecutor { _, _ -> } }
                    .potionEffectArgument("potion") { anyExecutor { _, _ -> } }
                    .biomeArgument("biome") { anyExecutor { _, _ -> } }
                    .enchantmentArgument("enchantment") { anyExecutor { _, _ -> } }
                    .resourceArgument("resource", "test:dsl-sample", customRegistry) { anyExecutor { _, _ -> } }
                    .instanceArgument("instance") { anyExecutor { _, _ -> } },
            )
            .toDefinition()

        val expectedNames = listOf("sound", "potion", "biome", "enchantment", "resource", "instance")
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
            val connected = connect(env.createConnection(profile("Registry-${UUID.randomUUID()}")), instance)
            player = connected
            block(manager, connected)
        } finally {
            if (player?.isRemoved == false) player.remove()
            platform.close()
            CommandAPI.uninstallPlatform(platform)
            env.destroyInstance(instance)
        }
    }

    private suspend fun TestScope.withBackend(
        env: Env,
        block: suspend BackendFixture.() -> Unit,
    ) {
        val commandManager = env.process().command()
        val ownership = MinestomCommandOwnership()
        val listener = MinestomSuggestionListener(ownership)
        val platform = MinestomCommandAPIPlatform(commandManager, ownership)
        val node = EventNode.all("registry-suggestions-test")
        val instance = env.createEmptyInstance()
        var player: Player? = null
        CommandAPI.installPlatform(platform)
        listener.register(node)
        env.process().eventHandler().addChild(node)
        try {
            val connection = env.createConnection()
            val connected = connect(connection, instance)
            player = connected
            BackendFixture(connection, connected).block()
        } finally {
            if (player?.isRemoved == false) player.remove()
            platform.close()
            CommandAPI.uninstallPlatform(platform)
            env.process().eventHandler().removeChild(node)
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

    private data class BackendFixture(
        val connection: TestConnection,
        val player: Player,
    ) {
        fun send(transactionId: Int, input: String) {
            MinecraftServer.process().packetListener().processClientPacket(
                ClientTabCompletePacket(transactionId, input),
                player.playerConnection,
            )
        }
    }
}
