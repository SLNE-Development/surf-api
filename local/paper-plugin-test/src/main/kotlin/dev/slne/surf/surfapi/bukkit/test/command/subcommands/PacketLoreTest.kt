package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.NamespacedKeyArgument
import dev.jorel.commandapi.kotlindsl.*
import dev.slne.surf.api.messages.Colors
import dev.slne.surf.api.paper.packet.SurfPaperPacketApi
import dev.slne.surf.api.spring.surfBean
import dev.slne.surf.surfapi.bukkit.test.plugin
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import java.util.concurrent.ConcurrentHashMap

class PacketLoreTest(name: String) : CommandAPICommand(name) {
    private val keys = ConcurrentHashMap.newKeySet<NamespacedKey>()

    init {
        subcommand("create") {
            namespacedKeyArgument("key")

            anyExecutor { _, args ->
                val key: NamespacedKey by args

                keys.add(key)
                packetApi.registerPacketLoreListener(plugin, key) { loreToDisplay, _, _ ->
                    loreToDisplay.add(Component.text("Hello, world!", Colors.AQUA))
                    loreToDisplay.add(Component.text("This is a test!", Colors.AQUA))
                    loreToDisplay.add(Component.text("This is a test!", Colors.PRIMARY))
                    loreToDisplay.add(Component.text("This is a test!", Colors.SECONDARY))
                }
            }
        }

        subcommand("apply") {
            val keyArgument = NamespacedKeyArgument("key").replaceSuggestions(
                ArgumentSuggestions.strings { _ ->
                    keys.map(NamespacedKey::asString).toTypedArray()
                },
            )
            argument(keyArgument)

            playerExecutor { player, args ->
                val key: NamespacedKey by args

                player.inventory.itemInMainHand.editMeta { meta ->
                    meta.persistentDataContainer.set(key, PersistentDataType.BOOLEAN, true)
                }
                player.sendMessage("Applied packet lore!")
            }
        }
    }
}

private val packetApi by surfBean<SurfPaperPacketApi>()
