package dev.slne.surf.surfapi.bukkit.server.scoreboard

import dev.slne.surf.surfapi.bukkit.api.scoreboard.ObsoleteScoreboardApi
import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfScoreboard
import dev.slne.surf.surfapi.bukkit.api.surfBukkitApi
import net.kyori.adventure.text.Component
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.FramedSidebarAnimation
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.SidebarAnimation
import org.bukkit.entity.Player

@ObsoleteScoreboardApi
open class SurfScoreboardImpl(
    protected val title: Component,
    protected val maxLines: Int,
    protected val sidebarComponent: SidebarComponent,
    protected val animations: List<SidebarAnimation<Component>>
) : SurfScoreboard {

    protected var scoreboard: Sidebar? = null
    protected var sidebarLayout: ComponentSidebarLayout? = null
    protected var enabled: Boolean = false

    override fun addViewer(viewer: Player) {
        check(enabled) { "Scoreboard is not enabled. Did you forget to call enable()?" }

        scoreboard!!.addPlayer(viewer)
    }

    override fun removeViewer(viewer: Player) {
        check(enabled) { "Scoreboard is not enabled. Did you forget to call enable()?" }

        scoreboard!!.removePlayer(viewer)
    }

    override fun enable() {
        check(!enabled) { "Scoreboard is already enabled" }

        scoreboard = surfBukkitApi.scoreboardLibrary().createSidebar(maxLines)
        sidebarLayout = ComponentSidebarLayout(
            SidebarComponent.staticLine(title),
            sidebarComponent
        ).also { it.apply(scoreboard!!) }

        enabled = true
    }

    override fun disable() {
        check(enabled) { "Scoreboard is not enabled. Did you forget to call enable()?" }

        scoreboard!!.close()
        animations.forEach { (it as? FramedSidebarAnimation<Component>)?.switchFrame(0) }

        this.scoreboard = null
        sidebarLayout = null
        enabled = false
    }

    override fun update() {
        check(enabled) { "Scoreboard is not enabled. Did you forget to call enable()?" }

        animations.forEach { it.nextFrame() }
        sidebarLayout!!.apply(scoreboard!!)
    }
}
