@file:OptIn(ExperimentalVisualizerApi::class)

package dev.slne.surf.surfapi.bukkit.test.command.subcommands.visualizer

import dev.jorel.commandapi.CommandAPIBukkit
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.*
import dev.slne.surf.surfapi.bukkit.api.visualizer.surfVisualizerApi
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.ExperimentalVisualizerApi
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerSingleLocation
import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.adventure.buildText
import dev.slne.surf.surfapi.core.api.random.RandomSelector
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Registry
import org.bukkit.entity.Player
import java.util.*

class SingleLocationVisualizerTest(name: String) : CommandAPICommand(name) {
    private val visualizers = mutableObject2ObjectMapOf<UUID, SurfVisualizerSingleLocation>()

    init {
        val randomBlock = RandomSelector.fromIterable(Registry.BLOCK)

        subcommand("create") {
            locationArgument("location", LocationType.BLOCK_POSITION)
            anyExecutor { sender, args ->
                val location: Location by args
                val visualizer = surfVisualizerApi.createSingleLocationVisualizer(location).apply {
                    if (sender is Player) {
                        addViewer(sender)
                    }

                    settings {
                        blockData = randomBlock.pick().createBlockData()
                    }

                    startVisualizing()
                }
                visualizers[visualizer.uid] = visualizer

                println("Visualizer created with UID: ${visualizer.uid}")
                println("Visualizer blockdata: ${visualizer.settings.blockData}")
                println("Visualizer $visualizer")
            }
        }

        subcommand("changeLocation") {
            uuidArgument("uid") {
                includeSuggestions(ArgumentSuggestions.stringCollection { visualizers.keys.map { it.toString() } })
            }
            locationArgument("location", LocationType.BLOCK_POSITION)

            anyExecutor { sender, args ->
                val uid: UUID by args
                val location: Location by args
                val visualizer = visualizers[uid]

                if (visualizer == null) {
                    throw CommandAPIBukkit.failWithAdventureComponent(buildText {
                        append(Component.text("Visualizer with UID "))
                        append(Component.text(uid.toString(), Colors.VARIABLE_VALUE))
                        append(Component.text(" not found!"))
                    })
                }

                visualizer.location = location
                visualizer.settings {
                    blockData = randomBlock.pick().createBlockData()
                }

                println("Visualizer location changed to: $location")
                println("Visualizer blockdata: ${visualizer.settings.blockData}")
                println("Visualizer $visualizer")
            }
        }

        subcommand("delete") {
            uuidArgument("uid") {
                includeSuggestions(ArgumentSuggestions.stringCollection { visualizers.keys.map { it.toString() } })
            }

            anyExecutor { sender, args ->
                val uid: UUID by args
                val visualizer = visualizers.remove(uid)

                if (visualizer == null) {
                    throw CommandAPIBukkit.failWithAdventureComponent(buildText {
                        append(Component.text("Visualizer with UID "))
                        append(Component.text(uid.toString(), Colors.VARIABLE_VALUE))
                        append(Component.text(" not found!"))
                    })
                }

                visualizer.stopVisualizing()
            }
        }
    }
}