package dev.slne.surf.api.paper.display.style

import dev.slne.surf.api.paper.display.argb
import dev.slne.surf.api.paper.display.cursor.CursorStyle

/**
 * CSS-like style properties for elements.
 */
class Style {
    /** Fixed width in pixels. `null` = auto (fill available width). */
    var width: Int? = null

    /** Fixed height in pixels. `null` = auto (fit content). */
    var height: Int? = null

    /** Background color (ARGB). `null` = transparent. */
    var backgroundColor: Int? = null

    /** Foreground/text color (ARGB). */
    var color: Int = argb(0xFFFFFF)

    /** Inner spacing between border and content. */
    var padding: Insets = Insets.ZERO

    /** Outer spacing around the element. */
    var margin: Insets = Insets.ZERO

    /** Border definition. `null` = no border. */
    var border: Border? = null

    /** Horizontal text alignment. */
    var textAlign: TextAlign = TextAlign.LEFT

    /** Vertical content alignment. */
    var verticalAlign: VerticalAlign = VerticalAlign.TOP

    /** Overflow behavior. */
    var overflow: Overflow = Overflow.HIDDEN

    /** Font size for text rendering. */
    var fontSize: Int = 12

    /** Whether this element is visible. */
    var visible: Boolean = true

    /** Layout direction for children (column = vertical, row = horizontal). */
    var flexDirection: FlexDirection = FlexDirection.COLUMN

    /** Spacing between children in pixels. */
    var gap: Int = 0

    /** Alignment of children along the main axis. */
    var justifyContent: JustifyContent = JustifyContent.START

    /** Alignment of children along the cross axis. */
    var alignItems: AlignItems = AlignItems.START

    /** Border radius for rounded corners (0 = sharp corners). */
    var borderRadius: Int = 0

    /** Opacity from 0.0 (invisible) to 1.0 (fully opaque). */
    var opacity: Float = 1.0f

    /** Cursor style when hovering over this element. `null` = inherit from parent. */
    var cursor: CursorStyle? = null
}
