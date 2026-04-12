package dev.slne.surf.api.paper.server.display.web

import dev.slne.surf.api.paper.server.display.Display
import dev.slne.surf.api.paper.display.document.document
import dev.slne.surf.api.paper.display.render.Canvas
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

/**
 * A display that renders a web page (HTML/CSS/JS) via JavaFX WebView
 * onto a wall of map item frames in front of the player.
 *
 * Extends the base [Display] system but replaces element-tree rendering
 * with live WebView snapshots. Mouse events are forwarded to the WebView
 * so HTML buttons, links, and JavaScript work.
 *
 * Usage:
 * ```kotlin
 * val webDisplay = WebDisplay(640, 384)
 * webDisplay.display.webDisplay = webDisplay
 * webDisplay.loadHtml("<html>...</html>")
 * DisplayManager.open(player, webDisplay.display)
 * webDisplay.startRendering()
 * ```
 */
class WebDisplay(
    val width: Int,
    val height: Int,
    renderWidth: Int = width,
    renderHeight: Int = height
) {
    val renderer = WebRenderer(width, height, renderWidth, renderHeight)

    val display: Display

    private var updateTaskId: Int = -1

    private var active = false

    init {
        val doc = document(width, height) {}
        display = Display(doc)
    }

    fun loadUrl(url: String) {
        renderer.loadUrl(url)
    }

    fun loadHtml(html: String) {
        renderer.loadHtml(html)
    }

    fun startRendering() {
        if (active) return
        active = true

        val plugin = JavaPlugin.getProvidingPlugin(javaClass)
        updateTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable {
            if (!active) return@Runnable
            try {
                val canvas = renderer.snapshot()
                updateDisplay(canvas)
            } catch (_: Exception) {
            }
        }, 10L, 1L).taskId
    }

    private fun updateDisplay(canvas: Canvas) {
        display.cachedCanvas = canvas
        display.update()
    }

    fun onCursorMove(x: Int, y: Int) {
        renderer.mouseMove(x, y)
    }

    fun onClick(x: Int, y: Int, isLeftClick: Boolean) {
        renderer.click(x, y, isLeftClick)
    }

    fun dispose() {
        active = false
        if (updateTaskId != -1) {
            Bukkit.getScheduler().cancelTask(updateTaskId)
            updateTaskId = -1
        }
        renderer.dispose()
    }
}
