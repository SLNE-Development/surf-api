package dev.slne.surf.surfapi.core.api.minimessage

import dev.slne.surf.surfapi.core.api.messages.Colors
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag

/**
 * Holds a preconfigured [MiniMessage] instance with custom color tags and prefix support for the Surf API.
 *
 * This object provides a centralized MiniMessage parser that includes custom tags for consistent
 * color theming throughout the application. All custom tags are registered at initialization and
 * map to predefined colors from [Colors].
 *
 * ## Available Custom Tags
 *
 * ### Color Tags
 * - `<primary>` - Primary theme color
 * - `<secondary>` - Secondary theme color
 * - `<info>` - Information message color
 * - `<success>` - Success message color
 * - `<warning>` - Warning message color
 * - `<error>` - Error message color
 * - `<variable_key>` - Variable key color
 * - `<variable_value>` - Variable value color
 * - `<spacer>` - Spacer color
 * - `<dark_spacer>` - Dark spacer color
 * - `<prefix_color>` - Prefix text color
 *
 * ### Prefix Tag
 * The `<prefix>` tag is a self-closing tag that inserts a predefined prefix component.
 * It supports optional types:
 * - `<prefix>` - Default prefix
 * - `<prefix:info>` - Info prefix
 * - `<prefix:success>` - Success prefix
 * - `<prefix:warning>` - Warning prefix
 * - `<prefix:error>` - Error prefix
 *
 * @see Colors
 */
object SurfMiniMessageHolder {
    private val minimessage = MiniMessage.builder()
        .editTags { tagBuilder ->
            val tags = mapOf(
                "primary" to Colors.PRIMARY,
                "secondary" to Colors.SECONDARY,
                "info" to Colors.INFO,
                "success" to Colors.SUCCESS,
                "warning" to Colors.WARNING,
                "error" to Colors.ERROR,
                "variable_key" to Colors.VARIABLE_KEY,
                "variable_value" to Colors.VARIABLE_VALUE,
                "spacer" to Colors.SPACER,
                "dark_spacer" to Colors.DARK_SPACER,
                "prefix_color" to Colors.PREFIX_COLOR,
            )

            tags.forEach { (tag, color) ->
                tagBuilder.tag(tag) { _, _ -> colorTag(color) }
            }

            tagBuilder
                .tag("prefix") { queue, context ->
                    val prefix = when (val type = queue.peek()?.lowerValue()) {
                        null -> Colors.PREFIX
                        "info" -> Colors.INFO_PREFIX
                        "success" -> Colors.SUCCESS_PREFIX
                        "warning" -> Colors.WARNING_PREFIX
                        "error" -> Colors.ERROR_PREFIX
                        else -> throw context.newException("Unknown prefix type: $type", queue)
                    }

                    Tag.selfClosingInserting(prefix)
                }
        }
        .build()

    /**
     * Creates a color styling tag for the given [TextColor].
     *
     * @param color The text color to apply
     * @return A [Tag] that applies the color styling
     */
    private fun colorTag(color: TextColor) = Tag.styling { it.color(color) }

    /**
     * Returns the preconfigured [MiniMessage] instance.
     *
     * @return The MiniMessage parser with custom Surf API tags
     */
    fun miniMessage() = minimessage
}

/**
 * Convenience property for accessing the preconfigured [MiniMessage] instance.
 *
 * @see SurfMiniMessageHolder
 */
val miniMessage get() = SurfMiniMessageHolder.miniMessage()