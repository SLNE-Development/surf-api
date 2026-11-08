package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import com.github.shynixn.mccoroutine.folia.regionDispatcher
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.*
import dev.slne.surf.api.paper.command.executors.playerExecutorSuspend
import dev.slne.surf.api.paper.pdc.block.pdc
import dev.slne.surf.api.paper.util.chunkX
import dev.slne.surf.api.paper.util.chunkZ
import dev.slne.surf.api.paper.util.doInChunkAsync
import dev.slne.surf.surfapi.bukkit.test.plugin
import kotlinx.coroutines.withContext
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import kotlin.math.abs
import kotlin.math.max

class BlockPdcContainerTest(name: String) : CommandAPICommand(name) {
    init {
        setCommand()
        getCommand()
        listCommand()
        clearCommand()
        copyNearCommand()
        copyFarCommand()
    }

    private fun setCommand() = subcommand("set") {
        locationArgument("location", LocationType.BLOCK_POSITION)
        textArgument("key")
        greedyStringArgument("value")

        playerExecutorSuspend { sender, args ->
            val location: Location by args
            val key: String by args
            val value: String by args

            val world = location.world ?: run {
                sender.sendMessage("Location has no world.")
                return@playerExecutorSuspend
            }

            val nsKey = NamespacedKey(plugin, key)

            world.doInChunkAsync(location.chunkX, location.chunkZ) { chunk ->
                val block = chunk.getBlock(location.blockX and 15, location.blockY, location.blockZ and 15)
                block.pdc().set(nsKey, PersistentDataType.STRING, value)
            }

            sender.sendMessage("Set '$key' = '$value' on block at (${location.blockX}, ${location.blockY}, ${location.blockZ}).")
        }
    }

    private fun getCommand() = subcommand("get") {
        locationArgument("location", LocationType.BLOCK_POSITION)
        textArgument("key")

        playerExecutorSuspend { sender, args ->
            val location: Location by args
            val key: String by args

            val world = location.world ?: run {
                sender.sendMessage("Location has no world.")
                return@playerExecutorSuspend
            }

            val nsKey = NamespacedKey(plugin, key)

            val result = world.doInChunkAsync(location.chunkX, location.chunkZ) { chunk ->
                val block = chunk.getBlock(location.blockX and 15, location.blockY, location.blockZ and 15)
                block.pdc().get(nsKey, PersistentDataType.STRING)
            }

            if (result != null) {
                sender.sendMessage("Block PDC [$key] = '$result' at (${location.blockX}, ${location.blockY}, ${location.blockZ}).")
            } else {
                sender.sendMessage("No value for key '$key' on block at (${location.blockX}, ${location.blockY}, ${location.blockZ}).")
            }
        }
    }

    private fun listCommand() = subcommand("list") {
        locationArgument("location", LocationType.BLOCK_POSITION)

        playerExecutorSuspend { sender, args ->
            val location: Location by args

            val world = location.world ?: run {
                sender.sendMessage("Location has no world.")
                return@playerExecutorSuspend
            }

            val keys = world.doInChunkAsync(location.chunkX, location.chunkZ) { chunk ->
                val block = chunk.getBlock(location.blockX and 15, location.blockY, location.blockZ and 15)
                block.pdc().keys.map { it.toString() }
            }

            if (keys.isEmpty()) {
                sender.sendMessage("Block PDC at (${location.blockX}, ${location.blockY}, ${location.blockZ}) is empty.")
            } else {
                sender.sendMessage("Block PDC keys at (${location.blockX}, ${location.blockY}, ${location.blockZ}) [${keys.size}]:")
                keys.forEach { sender.sendMessage("  - $it") }
            }
        }
    }

    private fun clearCommand() = subcommand("clear") {
        locationArgument("location", LocationType.BLOCK_POSITION)

        playerExecutorSuspend { sender, args ->
            val location: Location by args

            val world = location.world ?: run {
                sender.sendMessage("Location has no world.")
                return@playerExecutorSuspend
            }

            world.doInChunkAsync(location.chunkX, location.chunkZ) { chunk ->
                val block = chunk.getBlock(location.blockX and 15, location.blockY, location.blockZ and 15)
                block.pdc().clear()
            }

            sender.sendMessage("Cleared block PDC at (${location.blockX}, ${location.blockY}, ${location.blockZ}).")
        }
    }

    private fun copyNearCommand() = subcommand("copy-near") {
        locationArgument("source", LocationType.BLOCK_POSITION)
        locationArgument("target", LocationType.BLOCK_POSITION)

        playerExecutorSuspend { sender, args ->
            val source: Location by args
            val target: Location by args

            if (!isSameWorld(sender, source, target)) {
                return@playerExecutorSuspend
            }

            val chunkDistance = chunkDistance(source, target)
            if (chunkDistance > 1) {
                sender.sendMessage("copy-near expects source and target in the same region (chunk distance <= 1), got $chunkDistance.")
                return@playerExecutorSuspend
            }

            runCopyTest(sender, source, target, "near")
        }
    }

    private fun copyFarCommand() = subcommand("copy-far") {
        locationArgument("source", LocationType.BLOCK_POSITION)
        locationArgument("target", LocationType.BLOCK_POSITION)

        playerExecutorSuspend { sender, args ->
            val source: Location by args
            val target: Location by args

            if (!isSameWorld(sender, source, target)) {
                return@playerExecutorSuspend
            }

            val chunkDistance = chunkDistance(source, target)
            if (chunkDistance < FAR_MIN_CHUNK_DISTANCE) {
                sender.sendMessage("copy-far expects blocks to be far apart (chunk distance >= $FAR_MIN_CHUNK_DISTANCE), got $chunkDistance.")
                return@playerExecutorSuspend
            }

            runCopyTest(sender, source, target, "far")
        }
    }

    private fun isSameWorld(sender: Player, source: Location, target: Location): Boolean {
        if (source.world == null || target.world == null) {
            sender.sendMessage("Both source and target must include a world.")
            return false
        }

        if (source.world != target.world) {
            sender.sendMessage("Source and target must be in the same world.")
            return false
        }

        return true
    }

    private suspend fun runCopyTest(sender: Player, source: Location, target: Location, label: String) {
        val value = "$label-copy-${System.currentTimeMillis()}"

        val result = runCatching {
            val sourcePdc = withContext(plugin.regionDispatcher(source)) {
                val sourceBlock = source.block
                val sourcePdc = sourceBlock.pdc()
                sourcePdc.set(TEST_KEY, PersistentDataType.STRING, value)
                sourcePdc
            }

            withContext(plugin.regionDispatcher(target)) {
                val targetBlock = target.block
                sourcePdc.copyTo(targetBlock)
                targetBlock.pdc().get(TEST_KEY, PersistentDataType.STRING)
            }
        }

        result.onSuccess { copiedValue ->
            if (copiedValue == value) {
                sender.sendMessage("Block PDC copy test [$label] passed. Value '$copiedValue' was copied successfully.")
            } else {
                sender.sendMessage("Block PDC copy test [$label] failed. Expected '$value', got '${copiedValue ?: "null"}'.")
            }
        }.onFailure { error ->
            sender.sendMessage("Block PDC copy test [$label] failed with exception: ${error::class.simpleName}: ${error.message}")
        }
    }

    private fun chunkDistance(source: Location, target: Location): Int {
        val dx = abs(source.chunkX - target.chunkX)
        val dz = abs(source.chunkZ - target.chunkZ)
        return max(dx, dz)
    }

    companion object {
        private const val FAR_MIN_CHUNK_DISTANCE = 32
        private val TEST_KEY = NamespacedKey(plugin, "block-pdc-copy-test")
    }
}

