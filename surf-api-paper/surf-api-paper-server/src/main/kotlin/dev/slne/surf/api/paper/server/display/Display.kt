package dev.slne.surf.api.paper.server.display

import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBundle
import dev.slne.surf.api.paper.display.argb
import dev.slne.surf.api.paper.display.behavior.Clickable
import dev.slne.surf.api.paper.display.behavior.Hoverable
import dev.slne.surf.api.paper.display.behavior.InteractionContext
import dev.slne.surf.api.paper.server.display.map.DisplayMap
import dev.slne.surf.api.paper.display.document.Document
import dev.slne.surf.api.paper.display.element.Div
import dev.slne.surf.api.paper.display.element.Element
import dev.slne.surf.api.paper.server.display.frame.DisplayItemFrame
import dev.slne.surf.api.paper.display.render.Canvas
import dev.slne.surf.api.paper.display.render.Renderer
import dev.slne.surf.api.paper.server.display.user.DisplayUser
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class Display(
    val document: Document,
) {
    internal val frames = mutableListOf<DisplayItemFrame>()
    internal var session: DisplaySession? = null
    internal var viewer: UUID? = null

    val cols = ceil(document.width / 128.0).toInt()
    val rows = ceil(document.height / 128.0).toInt()

    lateinit var cardinal: CardinalInfo
        private set

    private var lastHoveredPath = emptyList<Element>()

    internal var cachedCanvas: Canvas? = null

    private var prevCursorX = -1
    private var prevCursorY = -1

    private var viewDistance: Double = FRAME_DISTANCE.toDouble()

    var webDisplay: dev.slne.surf.api.paper.server.display.web.WebDisplay? = null

    // --- Modal System ---
    private val modalStack = mutableListOf<Div>()

    val hasModal: Boolean get() = modalStack.isNotEmpty()

    fun showModal(content: Div) {
        content.style.width = document.width
        content.style.height = document.height
        modalStack.add(content)
        lastHoveredPath = emptyList()
        update()
    }

    fun dismissModal() {
        if (modalStack.isNotEmpty()) {
            modalStack.removeAt(modalStack.size - 1)
            lastHoveredPath = emptyList()
            update()
        }
    }

    fun dismissAllModals() {
        modalStack.clear()
        lastHoveredPath = emptyList()
        update()
    }

    data class CardinalInfo(
        val centerYaw: Float,
        val forwardX: Int,
        val forwardZ: Int,
        val rightX: Int,
        val rightZ: Int,
        val frameFacing: DisplayItemFrame.Direction
    )

    fun spawn(player: Player) {
        val user = DisplayUser.of(player.uniqueId)

        session?.close()
        if (frames.isNotEmpty()) {
            frames.forEach { it.despawn(user) }
            frames.clear()
        }

        cardinal = nearestCardinal(player.location.yaw)

        cachedCanvas = renderWithModals()

        val eyeLoc = player.eyeLocation
        val eyePos = Vector3d(eyeLoc.x, eyeLoc.y, eyeLoc.z)
        val newFrames = renderFrames(eyePos)
        frames.addAll(newFrames)

        val cameraEyePos = computeCenteredCameraPosition()
        viewDistance = computeViewDistance(cameraEyePos)

        val newSession = DisplaySession(user, cardinal.centerYaw, cameraEyePos)
        newSession.open()
        session = newSession
        user.session = newSession
        viewer = player.uniqueId

        user.sendPacket(WrapperPlayServerBundle())
        frames.forEach { it.spawn(user) }
        user.sendPacket(WrapperPlayServerBundle())
    }

    fun despawn(player: Player) {
        val user = DisplayUser.of(player.uniqueId)
        frames.forEach { it.despawn(user) }
        frames.clear()
        session?.close()
        session = null
        user.session = null
        viewer = null
        lastHoveredPath = emptyList()
        cachedCanvas = null
        prevCursorX = -1
        prevCursorY = -1
        modalStack.clear()

        webDisplay?.dispose()
        webDisplay = null
    }

    fun update() {
        val uuid = viewer ?: return
        val user = DisplayUser.of(uuid)
        val sess = session ?: return

        if (webDisplay == null) {
            cachedCanvas = renderWithModals()
        }
        val cached = cachedCanvas ?: return

        user.sendPacket(WrapperPlayServerBundle())
        var frameIndex = 0
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (frameIndex < frames.size) {
                    val mapData = tileDataWithCursor(cached, col, row, sess.cursorX, sess.cursorY)
                    frames[frameIndex].map.updateData(mapData)
                    frames[frameIndex].sendMapUpdate(user)
                }
                frameIndex++
            }
        }
        user.sendPacket(WrapperPlayServerBundle())
    }

    fun onCursorMove(yaw: Float, pitch: Float) {
        val uuid = viewer ?: return
        val sess = session ?: return
        val cached = cachedCanvas ?: return

        val pixel = rotationToPixel(yaw, pitch) ?: return
        val newX = pixel.first
        val newY = pixel.second

        if (newX == prevCursorX && newY == prevCursorY) return

        sess.cursorX = newX
        sess.cursorY = newY

        val tilesToUpdate = mutableSetOf<Int>()

        if (prevCursorX >= 0) {
            addCursorTiles(tilesToUpdate, prevCursorX, prevCursorY)
        }
        addCursorTiles(tilesToUpdate, newX, newY)

        prevCursorX = newX
        prevCursorY = newY

        val user = DisplayUser.of(uuid)
        user.sendPacket(WrapperPlayServerBundle())
        for (frameIndex in tilesToUpdate) {
            if (frameIndex in frames.indices) {
                val tileCol = frameIndex % cols
                val tileRow = frameIndex / cols
                val mapData = tileDataWithCursor(cached, tileCol, tileRow, newX, newY)
                frames[frameIndex].map.updateData(mapData)
                frames[frameIndex].sendMapUpdate(user)
            }
        }
        user.sendPacket(WrapperPlayServerBundle())

        webDisplay?.onCursorMove(newX, newY)

        val targetRoot = if (modalStack.isNotEmpty()) modalStack.last() else document.root
        val newPath = mutableListOf<Element>()
        collectElementPath(targetRoot, newX, newY, 0, 0, newPath)
        val newPathSet = newPath.toSet()
        val oldPathSet = lastHoveredPath.toSet()

        for (old in lastHoveredPath) {
            if (old !in newPathSet) {
                old.findBehaviors<Hoverable>().forEach { hoverable ->
                    hoverable.onExit(InteractionContext(uuid, old, newX, newY))
                }
            }
        }

        for (new in newPath) {
            if (new !in oldPathSet) {
                new.findBehaviors<Hoverable>().forEach { hoverable ->
                    hoverable.onEnter(InteractionContext(uuid, new, newX, newY))
                }
            }
        }

        lastHoveredPath = newPath
    }

    fun onClick(isLeftClick: Boolean) {
        val uuid = viewer ?: return
        val sess = session ?: return

        webDisplay?.onClick(sess.cursorX, sess.cursorY, isLeftClick)

        val targetRoot = if (modalStack.isNotEmpty()) modalStack.last() else document.root
        val path = mutableListOf<Element>()
        collectElementPath(targetRoot, sess.cursorX, sess.cursorY, 0, 0, path)

        for (element in path.asReversed()) {
            val clickables = element.findBehaviors<Clickable>()
            if (clickables.isNotEmpty()) {
                clickables.forEach { clickable ->
                    val ctx = InteractionContext(uuid, element, sess.cursorX, sess.cursorY)
                    if (isLeftClick) {
                        clickable.onClick(ctx)
                    } else {
                        clickable.onRightClick(ctx)
                    }
                }
                return
            }
        }
    }

    private fun renderWithModals(): Canvas {
        val base = document.render()
        for (modal in modalStack) {
            val modalCanvas = Canvas(document.width, document.height)
            Renderer.render(modal, modalCanvas)
            base.blend(modalCanvas, 0, 0)
        }
        return base
    }

    private fun addCursorTiles(tiles: MutableSet<Int>, cx: Int, cy: Int) {
        val cursorW = CURSOR_ARROW[0].size.coerceAtLeast(CURSOR_ARROW.maxOf { it.size })
        val cursorH = CURSOR_ARROW.size

        val minTileCol = (cx / 128).coerceIn(0, cols - 1)
        val maxTileCol = ((cx + cursorW) / 128).coerceIn(0, cols - 1)
        val minTileRow = (cy / 128).coerceIn(0, rows - 1)
        val maxTileRow = ((cy + cursorH) / 128).coerceIn(0, rows - 1)

        for (tr in minTileRow..maxTileRow) {
            for (tc in minTileCol..maxTileCol) {
                tiles.add(tr * cols + tc)
            }
        }
    }

    internal fun tileDataWithCursor(cached: Canvas, tileCol: Int, tileRow: Int, cursorX: Int, cursorY: Int): ByteArray {
        val offsetX = tileCol * 128
        val offsetY = tileRow * 128

        val tile = Canvas(128, 128)
        for (y in 0 until 128) {
            for (x in 0 until 128) {
                val px = offsetX + x
                val py = offsetY + y
                if (px < cached.width && py < cached.height) {
                    tile.pixels[y * 128 + x] = cached.pixels[py * cached.width + px]
                }
            }
        }

        val localCX = cursorX - offsetX
        val localCY = cursorY - offsetY
        drawCursor(tile, localCX, localCY)

        return tile.toMapColors(0, 0)
    }

    private fun drawCursor(canvas: Canvas, x: Int, y: Int) {
        for (row in CURSOR_ARROW.indices) {
            val line = CURSOR_ARROW[row]
            for (col in line.indices) {
                val pixel = line[col]
                if (pixel != 0) {
                    val color = if (pixel == 1) CURSOR_OUTLINE else CURSOR_FILL
                    canvas.setPixelUnclipped(x + col, y + row, color)
                }
            }
        }
    }

    private fun renderFrames(eyeLocation: Vector3d): List<DisplayItemFrame> {
        val cached = cachedCanvas ?: return emptyList()
        val newFrames = mutableListOf<DisplayItemFrame>()

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val mapData = cached.toMapColors(col * 128, row * 128)
                val mapId = nextMapId()
                val map = DisplayMap(mapId, mapData)
                val pos = framePosition(eyeLocation, col, row)
                newFrames.add(DisplayItemFrame(pos, map, cardinal.frameFacing))
            }
        }
        return newFrames
    }

    private fun computeCenteredCameraPosition(): Vector3d {
        if (frames.isEmpty()) return Vector3d.zero()

        val wallCenterX = frames.map { it.location.x + 0.5 }.average()
        val wallCenterY = frames.map { it.location.y + 0.5 }.average()
        val wallCenterZ = frames.map { it.location.z + 0.5 }.average()

        val camX = wallCenterX - cardinal.forwardX * FRAME_DISTANCE
        val camY = wallCenterY
        val camZ = wallCenterZ - cardinal.forwardZ * FRAME_DISTANCE

        return Vector3d(camX, camY, camZ)
    }

    private fun computeViewDistance(cameraEyePos: Vector3d): Double {
        if (frames.isEmpty()) return FRAME_DISTANCE.toDouble()

        val wallCenterX = frames.map { it.location.x + 0.5 }.average()
        val wallCenterZ = frames.map { it.location.z + 0.5 }.average()

        val dx = wallCenterX - cameraEyePos.x
        val dz = wallCenterZ - cameraEyePos.z

        return (dx * cardinal.forwardX + dz * cardinal.forwardZ).coerceAtLeast(1.0)
    }

    private fun rotationToPixel(yaw: Float, pitch: Float): Pair<Int, Int>? {
        var relativeYaw = yaw - cardinal.centerYaw
        if (relativeYaw > 180f) relativeYaw -= 360f
        if (relativeYaw < -180f) relativeYaw += 360f

        if (relativeYaw > 90f || relativeYaw < -90f) return null

        val halfHorizAngle = Math.toDegrees(atan(cols / 2.0 / viewDistance)).toFloat()
        val halfVertAngle = Math.toDegrees(atan(rows / 2.0 / viewDistance)).toFloat()

        val clampedYaw = relativeYaw.coerceIn(-halfHorizAngle, halfHorizAngle)
        val clampedPitch = pitch.coerceIn(-halfVertAngle, halfVertAngle)

        val normalizedX = clampedYaw / halfHorizAngle
        val normalizedY = clampedPitch / halfVertAngle

        val pixelX = ((normalizedX + 1f) / 2f * document.width).roundToInt()
            .coerceIn(0, document.width - 1)
        val pixelY = ((normalizedY + 1f) / 2f * document.height).roundToInt()
            .coerceIn(0, document.height - 1)

        return pixelX to pixelY
    }

    private fun collectElementPath(
        node: Element, px: Int, py: Int, offsetX: Int, offsetY: Int, path: MutableList<Element>
    ): Boolean {
        if (!node.style.visible) return false

        val s = node.style
        val bw = s.border?.width ?: 0
        val absX = offsetX + node.bounds.x
        val absY = offsetY + node.bounds.y

        if (px < absX || px >= absX + node.bounds.width || py < absY || py >= absY + node.bounds.height) {
            return false
        }

        path.add(node)

        val cx = absX + s.padding.left + bw
        val cy = absY + s.padding.top + bw

        for (child in node.children.reversed()) {
            if (collectElementPath(child, px, py, cx, cy, path)) {
                return true
            }
        }

        return true
    }

    private fun framePosition(eyeLocation: Vector3d, col: Int, row: Int): Vector3d {
        val dir = cardinal

        val baseX = floor(eyeLocation.x).toInt()
        val baseY = floor(eyeLocation.y).toInt()
        val baseZ = floor(eyeLocation.z).toInt()

        val colOffset = col - cols / 2
        val topRow = (rows - 1) / 2
        val rowOffset = topRow - row

        val frameX = baseX + dir.forwardX * FRAME_DISTANCE + dir.rightX * colOffset
        val frameY = baseY + rowOffset
        val frameZ = baseZ + dir.forwardZ * FRAME_DISTANCE + dir.rightZ * colOffset

        return Vector3d(frameX.toDouble(), frameY.toDouble(), frameZ.toDouble())
    }

    companion object {
        const val FRAME_DISTANCE = 2

        private val mapIdCounter = AtomicInteger(10000)
        fun nextMapId() = mapIdCounter.getAndIncrement()

        private val CURSOR_OUTLINE = argb(0, 0, 0)
        private val CURSOR_FILL = argb(255, 255, 255)

        private val CURSOR_ARROW = arrayOf(
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(1, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(1, 2, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(1, 2, 2, 2, 2, 1, 0, 0, 0, 0, 0, 0),
            intArrayOf(1, 2, 2, 2, 2, 2, 1, 0, 0, 0, 0, 0),
            intArrayOf(1, 2, 2, 2, 2, 2, 2, 1, 0, 0, 0, 0),
            intArrayOf(1, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 0),
            intArrayOf(1, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0),
            intArrayOf(1, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 0),
            intArrayOf(1, 2, 2, 1, 2, 2, 1, 0, 0, 0, 0, 0),
            intArrayOf(1, 2, 1, 0, 1, 2, 2, 1, 0, 0, 0, 0),
            intArrayOf(1, 1, 0, 0, 1, 2, 2, 1, 0, 0, 0, 0),
            intArrayOf(1, 0, 0, 0, 0, 1, 2, 2, 1, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 1, 2, 2, 1, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0),
        )

        fun nearestCardinal(yaw: Float): CardinalInfo {
            val normalized = ((yaw % 360f) + 360f) % 360f
            return when {
                normalized < 45f || normalized >= 315f -> CardinalInfo(
                    centerYaw = 0f,
                    forwardX = 0, forwardZ = 1,
                    rightX = -1, rightZ = 0,
                    frameFacing = DisplayItemFrame.Direction.NORTH
                )
                normalized < 135f -> CardinalInfo(
                    centerYaw = 90f,
                    forwardX = -1, forwardZ = 0,
                    rightX = 0, rightZ = -1,
                    frameFacing = DisplayItemFrame.Direction.EAST
                )
                normalized < 225f -> CardinalInfo(
                    centerYaw = 180f,
                    forwardX = 0, forwardZ = -1,
                    rightX = 1, rightZ = 0,
                    frameFacing = DisplayItemFrame.Direction.SOUTH
                )
                else -> CardinalInfo(
                    centerYaw = 270f,
                    forwardX = 1, forwardZ = 0,
                    rightX = 0, rightZ = 1,
                    frameFacing = DisplayItemFrame.Direction.WEST
                )
            }
        }
    }
}
