package dev.slne.surf.api.paper.server.display.web

import dev.slne.surf.api.paper.display.render.Canvas
import javafx.scene.web.WebView
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.image.WritableImage
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.event.Event
import javafx.event.EventType
import java.awt.image.BufferedImage
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

/**
 * Renders a web page (HTML/CSS/JS) offscreen using JavaFX WebView
 * and provides the result as a [Canvas] for the map display system.
 *
 * The WebView renders internally at [renderWidth]×[renderHeight] (default 1280×720),
 * then the snapshot is downscaled to [width]×[height] (the map display size) for
 * readable text and properly fitting content.
 *
 * Mouse coordinates from the display are automatically scaled to the render
 * resolution before being forwarded to the WebView.
 */
class WebRenderer(
    val width: Int,
    val height: Int,
    val renderWidth: Int = 1280,
    val renderHeight: Int = 720
) {
    private val logger = Logger.getLogger("SurfDisplay-WebRenderer")
    private var webView: WebView? = null
    private var scene: Scene? = null
    private var stage: Stage? = null
    private val ready = AtomicBoolean(false)
    private val disposed = AtomicBoolean(false)

    private val scaleX: Double = renderWidth.toDouble() / width.toDouble()
    private val scaleY: Double = renderHeight.toDouble() / height.toDouble()

    init {
        JavaFxPlatform.init()

        JavaFxPlatform.runOnFxThread {
            val wv = WebView()
            wv.prefWidth = renderWidth.toDouble()
            wv.prefHeight = renderHeight.toDouble()
            wv.minWidth = renderWidth.toDouble()
            wv.minHeight = renderHeight.toDouble()
            wv.maxWidth = renderWidth.toDouble()
            wv.maxHeight = renderHeight.toDouble()

            val root = StackPane(wv)
            val sc = Scene(root, renderWidth.toDouble(), renderHeight.toDouble())
            sc.fill = Color.WHITE

            val st = Stage(StageStyle.UTILITY)
            st.scene = sc
            st.width = renderWidth.toDouble()
            st.height = renderHeight.toDouble()
            st.x = -9999.0
            st.y = -9999.0
            st.opacity = 0.0
            st.show()

            wv.engine.loadWorker.stateProperty().addListener { _, _, newState ->
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    ready.set(true)
                    logger.info("WebView page loaded successfully")
                }
            }

            wv.engine.loadWorker.exceptionProperty().addListener { _, _, ex ->
                if (ex != null) logger.warning("WebView load error: ${ex.message}")
            }

            webView = wv
            scene = sc
            stage = st
        }.join()
    }

    fun loadUrl(url: String) {
        if (disposed.get()) return
        ready.set(false)
        JavaFxPlatform.runOnFxThread { webView?.engine?.load(url) }
    }

    fun loadHtml(html: String) {
        if (disposed.get()) return
        ready.set(false)
        JavaFxPlatform.runOnFxThread { webView?.engine?.loadContent(html, "text/html") }
    }

    fun snapshot(): Canvas {
        if (disposed.get()) return Canvas(width, height)

        return try {
            JavaFxPlatform.runOnFxThread {
                val sc = scene ?: return@runOnFxThread Canvas(width, height)

                val hiRes = WritableImage(renderWidth, renderHeight)
                sc.snapshot(hiRes)
                val reader = hiRes.pixelReader

                if (renderWidth == width && renderHeight == height) {
                    val canvas = Canvas(width, height)
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            canvas.pixels[y * width + x] = reader.getArgb(x, y)
                        }
                    }
                    canvas
                } else {
                    val srcImage = BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_ARGB)
                    for (y in 0 until renderHeight) {
                        for (x in 0 until renderWidth) {
                            srcImage.setRGB(x, y, reader.getArgb(x, y))
                        }
                    }

                    val scaled = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                    val g2d = scaled.createGraphics()
                    g2d.setRenderingHint(
                        java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR
                    )
                    g2d.drawImage(srcImage, 0, 0, width, height, null)
                    g2d.dispose()

                    val canvas = Canvas(width, height)
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            canvas.pixels[y * width + x] = scaled.getRGB(x, y)
                        }
                    }
                    canvas
                }
            }.join()
        } catch (e: Exception) {
            logger.warning("WebView snapshot failed: ${e.message}")
            Canvas(width, height)
        }
    }

    fun mouseMove(x: Int, y: Int) {
        if (disposed.get()) return
        val rx = (x * scaleX).toInt().coerceIn(0, renderWidth - 1)
        val ry = (y * scaleY).toInt().coerceIn(0, renderHeight - 1)
        JavaFxPlatform.runOnFxThread {
            val wv = webView ?: return@runOnFxThread
            val event = MouseEvent(
                MouseEvent.MOUSE_MOVED,
                rx.toDouble(), ry.toDouble(),
                rx.toDouble(), ry.toDouble(),
                MouseButton.NONE, 0,
                false, false, false, false,
                false, false, false,
                false, false, false,
                null
            )
            Event.fireEvent(wv, event)
        }
    }

    fun click(x: Int, y: Int, isLeftClick: Boolean) {
        if (disposed.get()) return
        val rx = (x * scaleX).toInt().coerceIn(0, renderWidth - 1)
        val ry = (y * scaleY).toInt().coerceIn(0, renderHeight - 1)
        JavaFxPlatform.runOnFxThread {
            val wv = webView ?: return@runOnFxThread
            val button = if (isLeftClick) MouseButton.PRIMARY else MouseButton.SECONDARY
            fireMouseEvent(wv, MouseEvent.MOUSE_PRESSED, rx, ry, button, 1)
            fireMouseEvent(wv, MouseEvent.MOUSE_RELEASED, rx, ry, button, 1)
            fireMouseEvent(wv, MouseEvent.MOUSE_CLICKED, rx, ry, button, 1)
        }
    }

    private fun fireMouseEvent(
        target: WebView, type: EventType<MouseEvent>,
        x: Int, y: Int, button: MouseButton, clickCount: Int
    ) {
        val event = MouseEvent(
            type,
            x.toDouble(), y.toDouble(), x.toDouble(), y.toDouble(),
            button, clickCount,
            false, false, false, false,
            button == MouseButton.PRIMARY, false, button == MouseButton.SECONDARY,
            false, false, false, null
        )
        Event.fireEvent(target, event)
    }

    fun executeScript(script: String): CompletableFuture<Any?> {
        if (disposed.get()) return CompletableFuture.completedFuture(null)
        return JavaFxPlatform.runOnFxThread { webView?.engine?.executeScript(script) }
    }

    fun isReady(): Boolean = ready.get()

    fun dispose() {
        if (!disposed.compareAndSet(false, true)) return
        JavaFxPlatform.runOnFxThread {
            stage?.hide()
            stage?.close()
            webView?.engine?.load(null)
            webView = null
            scene = null
            stage = null
        }
    }
}
