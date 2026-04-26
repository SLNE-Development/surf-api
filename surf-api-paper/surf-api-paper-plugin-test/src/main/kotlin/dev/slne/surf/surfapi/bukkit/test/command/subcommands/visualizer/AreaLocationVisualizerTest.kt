package dev.slne.surf.surfapi.bukkit.test.command.subcommands.visualizer

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.*
import dev.slne.surf.api.core.util.mutableObject2ObjectMapOf
import dev.slne.surf.api.paper.nms.bridges.packets.entity.BlockDisplaySettings
import dev.slne.surf.api.paper.util.toVector3d
import dev.slne.surf.api.paper.visualizer.SurfPaperVisualizerApi
import dev.slne.surf.api.paper.visualizer.visualizer.ExperimentalVisualizerApi
import dev.slne.surf.api.paper.visualizer.visualizer.SurfVisualizerArea
import org.bukkit.Location
import org.bukkit.block.BlockType
import org.spongepowered.math.vector.Vector3f
import java.util.*

@OptIn(ExperimentalVisualizerApi::class)
class AreaLocationVisualizerTest(name: String) : CommandAPICommand(name) {
    private val visualizers = mutableObject2ObjectMapOf<UUID, SurfVisualizerArea>()

    init {
        subcommand("create") {
            locationArgument("loc1", LocationType.BLOCK_POSITION, true)
            locationArgument("loc2", LocationType.BLOCK_POSITION, true)
            locationArgument("loc3", LocationType.BLOCK_POSITION, true)
            locationArgument("loc4", LocationType.BLOCK_POSITION, true)

            playerExecutor { sender, args ->
                val loc1: Location by args
                val loc2: Location by args
                val loc3: Location by args
                val loc4: Location by args

                SurfPaperVisualizerApi.createAreaVisualizer(
                    sender.world,
                    useHighestYBlock = true,
                    initialSettings = BlockDisplaySettings {
                        blockData = BlockType.DIRT.createBlockData()
                        scale = Vector3f(1f, 5f, 1f)
                    },
                    initialEdges = listOf(
                        loc1.toVector3d(),
                        loc2.toVector3d(),
                        loc3.toVector3d(),
                        loc4.toVector3d()
                    )
                ).apply {
                    addViewer(sender)
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
                    setCornerLocations(
                        listOf(
                            loc1.toVector3d(),
                            loc2.toVector3d(),
                            loc3.toVector3d(),
                            loc4.toVector3d()
                        )
                    )
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