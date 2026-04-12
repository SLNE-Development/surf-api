package dev.slne.surf.api.paper.display.cursor

/**
 * Predefined cursor styles that can be applied to elements.
 * Each style maps to a horse armor texture path for visual cursor display.
 *
 * Usage:
 * ```kotlin
 * div {
 *     style { cursor = CursorStyle.POINTER }
 *     clickable { ... }
 * }
 * ```
 */
enum class CursorStyle(val texturePath: String) {
    /** Default arrow cursor. */
    DEFAULT("cursor_default"),

    /** Pointer/hand cursor for clickable elements. */
    POINTER("cursor_pointer"),

    /** Text caret cursor for text input fields. */
    TEXT("cursor_text"),

    /** Move/drag cursor for draggable elements. */
    MOVE("cursor_move"),

    /** Grab cursor for drag-and-drop. */
    GRAB("cursor_grab"),

    /** Grabbing cursor (actively dragging). */
    GRABBING("cursor_grabbing"),

    /** Not-allowed cursor for disabled elements. */
    NOT_ALLOWED("cursor_not_allowed"),

    /** Crosshair cursor for precise selection. */
    CROSSHAIR("cursor_crosshair"),

    /** Resize horizontal cursor. */
    RESIZE_HORIZONTAL("cursor_resize_horizontal"),

    /** Resize vertical cursor. */
    RESIZE_VERTICAL("cursor_resize_vertical"),

    /** Loading/busy cursor. */
    WAIT("cursor_wait"),
}
