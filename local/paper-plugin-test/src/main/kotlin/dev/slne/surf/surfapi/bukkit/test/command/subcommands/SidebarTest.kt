package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.*
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.paper.sidebar.*
import dev.slne.surf.surfapi.bukkit.test.plugin
import io.papermc.paper.util.Tick
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration

class SidebarTest(name: String) : CommandAPICommand(name) {
    private var manual: SurfViewerSidebar? = null
    private var auto: SurfAutoUpdatingSidebar? = null
    private var global: SurfGlobalSidebar? = null
    private var failing: SurfAutoUpdatingSidebar? = null

    init {
        manualCommands()
        autoCommands()
        globalCommands()
        failingCommands()

        subcommand("closeall") {
            anyExecutor { sender, _ ->
                closeAll()
                sender.sendMessage("Closed all test sidebars.")
            }
        }
    }

    private fun manualCommands() = subcommand("manual") {
        subcommand("show") {
            playerExecutor { player, _ ->
                val sidebar = manual?.takeUnless { it.isClosed } ?: createManualSidebar().also { manual = it }
                sidebar.addViewer(player)
                player.sendMessage("Showing manual sidebar.")
            }
        }

        subcommand("hide") {
            playerExecutor { player, _ ->
                manual?.removeViewer(player)
                player.sendMessage("Removed you from the manual sidebar.")
            }
        }

        subcommand("update") {
            anyExecutor { sender, _ ->
                val sidebar = manual ?: return@anyExecutor sender.noSidebar("manual")
                sidebar.update()
                sender.sendMessage("Requested an update for ${sidebar.viewers.size} viewer(s).")
            }
        }

        subcommand("close") {
            anyExecutor { sender, _ ->
                manual?.close()
                manual = null
                sender.sendMessage("Closed the manual sidebar.")
            }
        }
    }

    private fun createManualSidebar(): SurfViewerSidebar {
        val updates = AtomicInteger()

        return sidebar(plugin) {
            buildTitle { primary("Manual Sidebar") }
            buildLine { info("Static line") }
            buildLine(score = Component.text("42")) { info("Static line with score") }
            emptyLine()
            buildSharedLine { info("Shared refreshes: "); variableValue(updates.incrementAndGet()) }
            buildViewerLine { viewer -> info("Viewer: "); variableValue(viewer.name) }
            animatedLine(listOf(Component.text("Frame 1"), Component.text("Frame 2"), Component.text("Frame 3")))
            separator()
        }
    }

    private fun autoCommands() = subcommand("auto") {
        subcommand("show") {
            integerArgument("ticks", 1, 200)

            playerExecutor { player, args ->
                val ticks: Int by args

                auto?.close()
                val sidebar = createAutoSidebar(ticks).also { auto = it }
                sidebar.addViewer(player)
                player.sendMessage("Showing auto-updating sidebar every $ticks tick(s). Sneak to add a line.")
            }
        }

        subcommand("hide") {
            playerExecutor { player, _ ->
                auto?.removeViewer(player)
                player.sendMessage("Removed you from the auto-updating sidebar.")
            }
        }

        subcommand("close") {
            anyExecutor { sender, _ ->
                auto?.close()
                auto = null
                sender.sendMessage("Closed the auto-updating sidebar.")
            }
        }
    }

    private fun createAutoSidebar(ticks: Int) = autoUpdatingSidebar(plugin) {
        updateInterval = Tick.of(ticks.toLong()).toKotlinDuration()
        maxLines = 12

        buildViewerTitle { viewer -> primary("Auto "); variableValue(viewer.name) }
        buildViewerLines { viewer ->
            val state = withContext(plugin.entityDispatcher(viewer)) {
                val location = viewer.location
                ViewerState(viewer.health, location.blockX, location.blockY, location.blockZ, viewer.isSneaking)
            }

            buildScoredLine({ info("Health") }, { variableValue("%.1f".format(state.health)) })
            buildScoredLine({ info("Position") }, { variableValue("${state.x} ${state.y} ${state.z}") })
            buildScoredLine({ info("Ping") }, { variableValue(viewer.ping) })
            if (state.sneaking) buildLine { error("Sneaking!") }
        }
        buildSharedLine { info("Online: "); variableValue(Bukkit.getOnlinePlayers().size) }
        buildGradientLine(Colors.PRIMARY, Colors.SPACER) { text("Gradient line") }
        lines { _ -> List(10) { SidebarLine(Component.text("Filler ${it + 1} (truncated at 12)")) } }
    }

    private data class ViewerState(
        val health: Double,
        val x: Int,
        val y: Int,
        val z: Int,
        val sneaking: Boolean,
    )

    private fun globalCommands() = subcommand("global") {
        subcommand("start") {
            integerArgument("ticks", 1, 200)

            anyExecutor { sender, args ->
                val ticks: Int by args

                global?.close()
                global = createGlobalSidebar(ticks)
                sender.sendMessage("Started the global sidebar every $ticks tick(s) for all players.")
            }
        }

        subcommand("stop") {
            anyExecutor { sender, _ ->
                global?.close()
                global = null
                sender.sendMessage("Stopped the global sidebar.")
            }
        }
    }

    private fun createGlobalSidebar(ticks: Int) = globalSidebar(plugin) {
        updateInterval = Tick.of(ticks.toLong()).toKotlinDuration()

        buildSharedTitle { primary("Global "); variableValue(LocalTime.now().format(TIME_FORMAT)) }
        buildSharedLines {
            buildScoredLine({ info("TPS") }, { variableValue("%.2f".format(Bukkit.getTPS()[0])) })
            buildScoredLine({ info("Online") }, { variableValue(Bukkit.getOnlinePlayers().size) })
        }
        emptyLine()
        buildViewerLine { viewer -> info("Hello "); variableValue(viewer.name) }
        separator()
    }

    private fun failingCommands() = subcommand("failing") {
        subcommand("show") {
            playerExecutor { player, _ ->
                val sidebar = failing?.takeUnless { it.isClosed } ?: createFailingSidebar().also { failing = it }
                sidebar.addViewer(player)
                player.sendMessage("Showing failing sidebar. Every third render throws; content must stay.")
            }
        }

        subcommand("close") {
            anyExecutor { sender, _ ->
                failing?.close()
                failing = null
                sender.sendMessage("Closed the failing sidebar.")
            }
        }
    }

    private fun createFailingSidebar(): SurfAutoUpdatingSidebar {
        val viewerRenders = AtomicInteger()
        val sharedRenders = AtomicInteger()

        return autoUpdatingSidebar(plugin) {
            updateInterval = 1.seconds

            buildTitle { error("Failing renderers") }
            line { _ ->
                val render = viewerRenders.incrementAndGet()
                if (render % 3 == 0) throw IllegalStateException("Intentional viewer failure #$render")
                Component.text("Viewer render #$render")
            }
            sharedLine {
                val render = sharedRenders.incrementAndGet()
                if (render % 3 == 0) throw IllegalStateException("Intentional shared failure #$render")
                Component.text("Shared render #$render")
            }
            buildLine { info("Static line stays") }
        }
    }

    private fun closeAll() {
        listOfNotNull(manual, auto, global, failing).forEach { it.close() }
        manual = null
        auto = null
        global = null
        failing = null
    }

    private fun CommandSender.noSidebar(kind: String) {
        sendMessage("No $kind sidebar exists. Create one with '/surfapitest sidebar $kind show'.")
    }

    companion object {
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")
    }
}
