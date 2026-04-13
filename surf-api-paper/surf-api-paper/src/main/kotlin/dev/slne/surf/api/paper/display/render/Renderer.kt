package dev.slne.surf.api.paper.display.render

import dev.slne.surf.api.paper.display.element.*
import dev.slne.surf.api.paper.display.style.*

import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage

/**
 * Handles layout computation and painting of the element tree onto a [Canvas].
 *
 * Uses a border-box model similar to CSS:
 * - Element width/height includes padding and border
 * - Margin is outside the element bounds
 * - Children are stacked vertically (column) or horizontally (row) based on [FlexDirection]
 */
object Renderer {

    fun render(root: Element, canvas: Canvas) {
        layout(root, 0, 0, canvas.width, canvas.height)
        paint(root, canvas, 0, 0)
    }

    // --- LAYOUT (border-box model) ---

    private fun layout(node: Element, x: Int, y: Int, availableWidth: Int, availableHeight: Int = Int.MAX_VALUE) {
        if (!node.style.visible) {
            node.bounds = Rect(x, y, 0, 0)
            return
        }

        val s = node.style
        val bw = s.border?.width ?: 0

        node.bounds.x = x + s.margin.left
        node.bounds.y = y + s.margin.top
        node.bounds.width = s.width ?: (availableWidth - s.margin.horizontal)

        val contentWidth = maxOf(0, node.bounds.width - s.padding.horizontal - bw * 2)
        var contentHeight = 0

        // Text content
        if (node is Label && node.text.isNotEmpty()) {
            node.wrappedLines = wrapText(node.text, maxOf(1, contentWidth), s.fontSize)
            val (tw, th) = measureWrappedText(node.wrappedLines, s.fontSize)
            node.textWidth = tw
            node.textHeight = th
            contentHeight += th
        }

        // Image content
        if (node is ImageElement) {
            contentHeight += node.source.height
        }

        // Shape content
        if (node is ShapeElement) {
            contentHeight += node.shape.height
        }

        // Children layout
        val childStartY = contentHeight
        when (s.flexDirection) {
            FlexDirection.COLUMN -> layoutColumn(node, contentWidth, s.gap, childStartY).also { contentHeight += it }
            FlexDirection.ROW -> layoutRow(node, contentWidth, s.gap, childStartY).also { contentHeight += it }
        }

        node.bounds.height = s.height ?: (contentHeight + s.padding.vertical + bw * 2)
    }

    private fun layoutColumn(node: Element, contentWidth: Int, gap: Int, startY: Int): Int {
        val s = node.style
        val bw = s.border?.width ?: 0

        var cursorY = startY
        for ((index, child) in node.children.withIndex()) {
            layout(child, 0, cursorY, contentWidth)
            cursorY += child.style.margin.top + child.bounds.height + child.style.margin.bottom
            if (index < node.children.size - 1 && gap > 0) {
                cursorY += gap
            }
        }
        val totalChildrenHeight = if (node.children.isNotEmpty()) cursorY - startY else 0

        if (s.justifyContent != JustifyContent.START && node.children.isNotEmpty()) {
            val availableContentHeight = if (s.height != null) {
                maxOf(0, s.height!! - s.padding.vertical - bw * 2 - startY)
            } else {
                totalChildrenHeight
            }
            val extraSpace = maxOf(0, availableContentHeight - totalChildrenHeight)
            if (extraSpace > 0) {
                when (s.justifyContent) {
                    JustifyContent.CENTER -> {
                        val offset = extraSpace / 2
                        for (child in node.children) {
                            child.bounds.y += offset
                        }
                    }
                    JustifyContent.END -> {
                        for (child in node.children) {
                            child.bounds.y += extraSpace
                        }
                    }
                    JustifyContent.SPACE_BETWEEN -> {
                        if (node.children.size > 1) {
                            val spaceBetween = extraSpace / (node.children.size - 1)
                            for ((i, child) in node.children.withIndex()) {
                                child.bounds.y += spaceBetween * i
                            }
                        }
                    }
                    JustifyContent.SPACE_AROUND -> {
                        val spaceAround = extraSpace / (node.children.size * 2)
                        for ((i, child) in node.children.withIndex()) {
                            child.bounds.y += spaceAround * (2 * i + 1)
                        }
                    }
                    else -> {}
                }
            }
        }

        if (s.alignItems != AlignItems.START && s.alignItems != AlignItems.STRETCH && node.children.isNotEmpty()) {
            for (child in node.children) {
                val childWidth = child.bounds.width
                val crossOffset = when (s.alignItems) {
                    AlignItems.CENTER -> maxOf(0, (contentWidth - childWidth) / 2)
                    AlignItems.END -> maxOf(0, contentWidth - childWidth)
                    else -> 0
                }
                if (crossOffset > 0) {
                    child.bounds.x += crossOffset
                }
            }
        }

        return totalChildrenHeight
    }

    private fun layoutRow(node: Element, contentWidth: Int, gap: Int, startY: Int): Int {
        val s = node.style

        var cursorX = 0
        var maxHeight = 0
        for ((index, child) in node.children.withIndex()) {
            val intrinsicWidth = when {
                child.style.width != null -> child.style.width
                child is ShapeElement -> child.shape.width
                child is ImageElement -> child.source.width
                else -> null
            }
            val childAvailableWidth = intrinsicWidth ?: maxOf(1, contentWidth - cursorX)
            layout(child, cursorX, startY, childAvailableWidth)

            if (child.style.width == null && child !is ShapeElement && child !is ImageElement) {
                val intrinsicW = computeIntrinsicWidth(child)
                if (intrinsicW < child.bounds.width) {
                    child.bounds.width = intrinsicW
                }
            }

            cursorX += child.style.margin.left + child.bounds.width + child.style.margin.right
            if (index < node.children.size - 1 && gap > 0) {
                cursorX += gap
            }
            maxHeight = maxOf(maxHeight, child.style.margin.top + child.bounds.height + child.style.margin.bottom)
        }
        val totalChildrenWidth = cursorX

        if (s.justifyContent != JustifyContent.START && node.children.isNotEmpty()) {
            val extraSpace = maxOf(0, contentWidth - totalChildrenWidth)
            if (extraSpace > 0) {
                when (s.justifyContent) {
                    JustifyContent.CENTER -> {
                        val offset = extraSpace / 2
                        for (child in node.children) {
                            child.bounds.x += offset
                        }
                    }
                    JustifyContent.END -> {
                        for (child in node.children) {
                            child.bounds.x += extraSpace
                        }
                    }
                    JustifyContent.SPACE_BETWEEN -> {
                        if (node.children.size > 1) {
                            val spaceBetween = extraSpace / (node.children.size - 1)
                            for ((i, child) in node.children.withIndex()) {
                                child.bounds.x += spaceBetween * i
                            }
                        }
                    }
                    JustifyContent.SPACE_AROUND -> {
                        val spaceAround = extraSpace / (node.children.size * 2)
                        for ((i, child) in node.children.withIndex()) {
                            child.bounds.x += spaceAround * (2 * i + 1)
                        }
                    }
                    else -> {}
                }
            }
        }

        if (s.alignItems != AlignItems.START && s.alignItems != AlignItems.STRETCH && node.children.isNotEmpty()) {
            for (child in node.children) {
                val childHeight = child.bounds.height
                val crossOffset = when (s.alignItems) {
                    AlignItems.CENTER -> maxOf(0, (maxHeight - childHeight) / 2)
                    AlignItems.END -> maxOf(0, maxHeight - childHeight)
                    else -> 0
                }
                if (crossOffset > 0) {
                    child.bounds.y += crossOffset
                }
            }
        }

        return maxHeight
    }

    private fun computeIntrinsicWidth(node: Element): Int {
        val s = node.style
        val bw = s.border?.width ?: 0
        var contentWidth = 0

        if (node is Label) {
            contentWidth = maxOf(contentWidth, node.textWidth)
        }

        if (node is ShapeElement) {
            contentWidth = maxOf(contentWidth, node.shape.width)
        }

        if (node is ImageElement) {
            contentWidth = maxOf(contentWidth, node.source.width)
        }

        if (s.flexDirection == FlexDirection.ROW) {
            var total = 0
            for ((i, child) in node.children.withIndex()) {
                total += child.style.margin.horizontal + child.bounds.width
                if (i < node.children.size - 1) total += s.gap
            }
            contentWidth = maxOf(contentWidth, total)
        } else {
            for (child in node.children) {
                contentWidth = maxOf(contentWidth, child.style.margin.horizontal + child.bounds.width)
            }
        }

        return contentWidth + s.padding.horizontal + bw * 2
    }

    // --- PAINT ---

    private fun paint(node: Element, canvas: Canvas, offsetX: Int, offsetY: Int) {
        if (!node.style.visible) return

        val s = node.style
        val bw = s.border?.width ?: 0
        val absX = offsetX + node.bounds.x
        val absY = offsetY + node.bounds.y

        s.backgroundColor?.let {
            canvas.fillRect(absX, absY, node.bounds.width, node.bounds.height, it)
        }

        s.border?.let {
            canvas.drawRect(absX, absY, node.bounds.width, node.bounds.height, it.color, it.width)
        }

        val cx = absX + s.padding.left + bw
        val cy = absY + s.padding.top + bw
        val cw = maxOf(0, node.bounds.width - s.padding.horizontal - bw * 2)

        if (node is Label && node.wrappedLines.isNotEmpty()) {
            val textImage = renderMultilineText(node.wrappedLines, s.fontSize, s.color)
            val textX = when (s.textAlign) {
                TextAlign.LEFT -> cx
                TextAlign.CENTER -> cx + (cw - node.textWidth) / 2
                TextAlign.RIGHT -> cx + cw - node.textWidth
            }
            drawBufferedImage(textImage, canvas, textX, cy)
        }

        if (node is ImageElement) {
            canvas.place(node.source, cx, cy)
        }

        if (node is ShapeElement) {
            node.shape.paint(canvas, cx, cy, s.color)
        }

        val ch = maxOf(0, node.bounds.height - s.padding.vertical - bw * 2)
        canvas.pushClip(cx, cy, cw, ch)
        for (child in node.children) {
            paint(child, canvas, cx, cy)
        }
        canvas.popClip()
    }

    // --- TEXT ---

    private fun wrapText(text: String, maxWidth: Int, fontSize: Int): List<String> {
        val lines = mutableListOf<String>()
        for (line in text.split("\n")) {
            if (line.isEmpty()) {
                lines.add("")
                continue
            }
            val words = line.split(" ")
            val current = StringBuilder()
            for (word in words) {
                val test = if (current.isEmpty()) word else "$current $word"
                val (w, _) = measureText(test, fontSize)
                if (w > maxWidth && current.isNotEmpty()) {
                    lines.add(current.toString())
                    current.clear().append(word)
                } else {
                    current.clear().append(test)
                }
            }
            if (current.isNotEmpty()) lines.add(current.toString())
        }
        return lines.ifEmpty { listOf("") }
    }

    private fun measureText(text: String, fontSize: Int): Pair<Int, Int> {
        val font = Font(Font.SANS_SERIF, Font.PLAIN, fontSize)
        val temp = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val g = temp.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF)
        g.font = font
        val m = g.fontMetrics
        val result = m.stringWidth(text) to m.height
        g.dispose()
        return result
    }

    private fun measureWrappedText(lines: List<String>, fontSize: Int): Pair<Int, Int> {
        val lineHeight = measureText("Ag", fontSize).second
        var maxWidth = 0
        for (line in lines) {
            if (line.isNotEmpty()) {
                maxWidth = maxOf(maxWidth, measureText(line, fontSize).first)
            }
        }
        return maxWidth to (lineHeight * lines.size)
    }

    private fun renderMultilineText(lines: List<String>, fontSize: Int, color: Int): BufferedImage {
        val font = Font(Font.SANS_SERIF, Font.PLAIN, fontSize)

        val temp = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val tg = temp.createGraphics()
        tg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF)
        tg.font = font
        val lineHeight = tg.fontMetrics.height
        var maxWidth = 0
        for (line in lines) {
            if (line.isNotEmpty()) {
                maxWidth = maxOf(maxWidth, tg.fontMetrics.stringWidth(line))
            }
        }
        tg.dispose()

        val w = maxOf(1, maxWidth)
        val h = maxOf(1, lineHeight * lines.size)

        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF)
        g.font = font
        g.color = java.awt.Color((color shr 16) and 0xFF, (color shr 8) and 0xFF, color and 0xFF)
        val ascent = g.fontMetrics.ascent

        for ((i, line) in lines.withIndex()) {
            if (line.isNotEmpty()) {
                g.drawString(line, 0, i * lineHeight + ascent)
            }
        }
        g.dispose()

        return img
    }

    private fun drawBufferedImage(img: BufferedImage, canvas: Canvas, destX: Int, destY: Int) {
        for (y in 0 until img.height) {
            for (x in 0 until img.width) {
                val pixel = img.getRGB(x, y)
                if ((pixel ushr 24) > 0) {
                    canvas.setPixel(destX + x, destY + y, pixel or (0xFF shl 24))
                }
            }
        }
    }
}
