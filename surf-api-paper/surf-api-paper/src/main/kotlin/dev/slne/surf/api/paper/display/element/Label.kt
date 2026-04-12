package dev.slne.surf.api.paper.display.element

/**
 * A text label element (like HTML `<span>` or `<p>`).
 */
class Label(var text: String) : Element() {
    internal var wrappedLines = listOf<String>()
    internal var textWidth = 0
    internal var textHeight = 0
}
