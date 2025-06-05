package dev.slne.surf.surfapi.bukkit.test.command.subcommands.visualizer

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.*
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.BlockDisplaySettings
import dev.slne.surf.surfapi.bukkit.api.visualizer.surfVisualizerApi
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.ExperimentalVisualizerApi
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizerArea
import dev.slne.surf.surfapi.core.api.random.RandomSelector
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import org.bukkit.Location
import org.bukkit.Registry
import org.bukkit.entity.Player
import java.util.*

@OptIn(ExperimentalVisualizerApi::class)
@Suppress("UnstableApiUsage")
class AreaLocationVisualizerTest(name: String) : CommandAPICommand(name) {
    private val visualizers = mutableObject2ObjectMapOf<UUID, SurfVisualizerArea>()

    init {
        val randomBlock = RandomSelector.fromIterable(Registry.BLOCK)

        subcommand("create") {
            locationArgument("loc1", LocationType.BLOCK_POSITION, true)
            locationArgument("loc2", LocationType.BLOCK_POSITION, true)
            locationArgument("loc3", LocationType.BLOCK_POSITION, true)
            locationArgument("loc4", LocationType.BLOCK_POSITION, true)

            anyExecutor { sender, args ->
                val loc1: Location by args
                val loc2: Location by args
                val loc3: Location by args
                val loc4: Location by args

                surfVisualizerApi.createAreaVisualizer(
                    initialSettings = BlockDisplaySettings {
                        randomBlock.pick().createBlockData()
                    },
                    initialEdges = listOf(loc1, loc2, loc3, loc4)
                ).apply {
                    if (sender is Player) {
                        addViewer(sender)
                    }

                    startVisualizing()
                    visualizers[uid] = this
                }
            }
        }

        subcommand("changeEdges") {
            uuidArgument("uid") {
                includeSuggestions(ArgumentSuggestions.stringCollection { visualizers.keys.map { it.toString() } })
            }
            locationArgument("loc1", LocationType.BLOCK_POSITION, true)
            locationArgument("loc2", LocationType.BLOCK_POSITION, true)
            locationArgument("loc3", LocationType.BLOCK_POSITION, true)
            locationArgument("loc4", LocationType.BLOCK_POSITION, true)

            anyExecutor { sender, args ->
                val uid: UUID by args
                val loc1: Location by args
                val loc2: Location by args
                val loc3: Location by args
                val loc4: Location by args

                visualizers[uid]?.apply {
                    setCornerLocations(listOf(loc1, loc2, loc3, loc4))
                    println("Visualizer edges updated for UID: $uid")
                } ?: run {
                    println("No visualizer found with UID: $uid")
                }
            }
        }

        subcommand("delete") {
            uuidArgument("uid") {
                includeSuggestions(ArgumentSuggestions.stringCollection { visualizers.keys.map { it.toString() } })
            }

            anyExecutor { sender, args ->
                val uid: UUID by args
                if (visualizers.remove(uid) != null) {
                    println("Visualizer with UID $uid deleted successfully.")
                } else {
                    println("No visualizer found with UID: $uid")
                }
            }
        }
    }
}