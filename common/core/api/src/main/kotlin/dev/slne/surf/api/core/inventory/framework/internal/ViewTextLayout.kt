package dev.slne.surf.api.core.inventory.framework.internal

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.KeybindComponent
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.TextDecoration

/**
 * A header text that has been laid out for rendering.
 *
 * @property component the component to emit, with any casing and spacing transformation applied
 * @property width the exact number of pixels [component] advances the render cursor by
 */
@InternalSurfApi
data class LaidOutViewText(val component: Component, val width: Int)

/**
 * Lays out a header text for the font it is rendered in and measures the result.
 *
 * Both the transformation and the measurement happen per Unicode **code point** and per component
 * part, so a surrogate pair counts as the single glyph it renders as and a bold word inside an
 * otherwise regular line is measured bold. The [advance] of every glyph is asked for individually,
 * which is what makes this work for the proportional vanilla font (see [DefaultFontWidths]) as well
 * as for a fixed-width resource-pack font.
 *
 * Three transformations are applied to the text of every part:
 * - [uppercase] uppercases it, for a font that only draws capitals. Note that uppercasing can
 *   change the glyph count (`ß` becomes `SS`), which is why the width is measured on the result.
 * - [charSpacing] inserts a shift glyph of that many pixels in front of every glyph, tightening
 *   (negative) or loosening (positive) the run.
 * - [spaceShift] replaces every space with a shift glyph of that many pixels. A resource-pack font
 *   built from a glyph sheet has no space glyph of its own — the sheet cell is empty, and an empty
 *   cell advances a single pixel — so the client would fall back to the missing-glyph box, which
 *   draws at its own baseline and therefore lands on the title line. Rendering the space as a shift
 *   avoids relying on the font providing one at all. Pass `null` for a font that does provide a
 *   space, such as the client font.
 *
 * The shift glyphs are emitted **inside** the part, so they render in the same font as the text —
 * that font has to provide them.
 *
 * Parts that are not plain text keep their content: a [TranslatableComponent] is measured on its
 * fallback (or its key) and a [KeybindComponent] on its keybind name, because what the client
 * renders for them depends on its language file, and none of the transformations can be applied
 * without breaking the translation.
 *
 * Shared by the per-platform header components. This is internal infrastructure — use the platform
 * header DSL instead.
 *
 * @param component the component to lay out
 * @param charSpacing pixels of spacing inserted in front of every glyph; `0` emits the text as-is
 * @param uppercase whether the text is uppercased
 * @param spaceShift pixels a space advances when rendered as a shift glyph, or `null` to emit the
 *   space itself
 * @param bold the [TextDecoration.BOLD] state inherited from the surrounding style
 * @param advance returns the pixel advance of one code point, given its bold state
 * @return the component to render together with its exact pixel width
 */
@InternalSurfApi
fun layoutViewText(
    component: Component,
    charSpacing: Int,
    uppercase: Boolean,
    spaceShift: Int? = null,
    bold: Boolean = false,
    advance: (codePoint: Int, bold: Boolean) -> Int,
): LaidOutViewText {
    val effectiveBold = when (component.decoration(TextDecoration.BOLD)) {
        TextDecoration.State.TRUE -> true
        TextDecoration.State.FALSE -> false
        TextDecoration.State.NOT_SET -> bold
    }

    val text = ownText(component)
    val laidOutText = if (component is TextComponent) {
        layoutRun(
            text = if (uppercase) text.uppercase() else text,
            charSpacing = charSpacing,
            spaceShift = spaceShift,
            bold = effectiveBold,
            advance = advance
        )
    } else {
        // Not plain text: emit it untouched, but still measure what the client will draw for it.
        LaidOutRun(text, measure(text, effectiveBold, advance))
    }

    val children = component.children().map { child ->
        layoutViewText(child, charSpacing, uppercase, spaceShift, effectiveBold, advance)
    }

    val width = laidOutText.width + children.sumOf { it.width }
    val laidOut = children.map { it.component }
    val result = if (component is TextComponent) {
        component.content(laidOutText.text).children(laidOut)
    } else {
        component.children(laidOut)
    }

    return LaidOutViewText(result, width)
}

/** One component part after transformation, with the pixels it advances the cursor by. */
private data class LaidOutRun(val text: String, val width: Int)

/**
 * Emits and measures a single run of text in one pass, applying [charSpacing] and [spaceShift].
 */
private fun layoutRun(
    text: String,
    charSpacing: Int,
    spaceShift: Int?,
    bold: Boolean,
    advance: (codePoint: Int, bold: Boolean) -> Int,
): LaidOutRun {
    if (text.isEmpty()) return LaidOutRun(text, 0)

    val spacing = if (charSpacing != 0) ShiftGlyphs.renderShift(charSpacing) else ""
    val space = if (spaceShift != null) ShiftGlyphs.renderShift(spaceShift) else ""

    var width = 0
    val emitted = StringBuilder(text.length)
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        index += Character.charCount(codePoint)

        if (charSpacing != 0) {
            emitted.append(spacing)
            width += charSpacing
        }

        if (spaceShift != null && codePoint == ' '.code) {
            emitted.append(space)
            width += spaceShift
        } else {
            emitted.appendCodePoint(codePoint)
            width += advance(codePoint, bold)
        }
    }

    return LaidOutRun(emitted.toString(), width)
}

/**
 * Returns the text [component] itself contributes, ignoring its children.
 */
private fun ownText(component: Component): String = when (component) {
    is TextComponent -> component.content()
    is TranslatableComponent -> component.fallback() ?: component.key()
    is KeybindComponent -> component.keybind()
    else -> ""
}

/**
 * Sums the [advance] of every code point in [text].
 */
private inline fun measure(
    text: String,
    bold: Boolean,
    advance: (codePoint: Int, bold: Boolean) -> Int,
): Int {
    var width = 0
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        width += advance(codePoint, bold)
        index += Character.charCount(codePoint)
    }

    return width
}
