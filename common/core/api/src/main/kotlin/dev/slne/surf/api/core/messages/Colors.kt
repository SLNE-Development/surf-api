package dev.slne.surf.api.core.messages

import dev.slne.surf.api.core.messages.Colors.Companion.INFO
import dev.slne.surf.api.core.messages.Colors.Companion.PRIMARY
import dev.slne.surf.api.core.messages.adventure.buildText
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextColor.color

/**
 * Defines the standardized color palette for the Surf system.
 *
 * This interface provides a consistent visual style across all Surf plugins through predefined colors
 * for UI elements, messages, and formatting components. All colors are defined as static fields
 * in the companion object.
 */
@Suppress("unused")
interface Colors {
    companion object {
        // -------------------- Surf Colors -------------------- //

        /**
         * Primary brand color (#3b92d1).
         * Use for prominent UI elements such as titles and branded components.
         */
        @JvmField
        val PRIMARY: TextColor = color(0x3b92d1)

        /**
         * Secondary color (#5b5b5b).
         * Use for less prominent elements such as subtitles and secondary text.
         */
        @JvmField
        val SECONDARY: TextColor = color(0x5b5b5b)

        /**
         * Informational message color (#97B3F7).
         * Use for neutral system information, status updates, and toggle confirmations
         * that are not direct responses to user actions.
         */
        @JvmField
        val INFO: TextColor = color(0x97B3F7)

        /**
         * Note color (#6EA6D9).
         * Use for supplemental information, tips, or clarifications that accompany primary messages.
         * Prefer [INFO] for system messages and [PRIMARY] for branded headings.
         */
        @JvmField
        val NOTE: TextColor = color(0x6EA6D9)

        /**
         * Success message color (#65ff64).
         * Use exclusively for positive outcomes in direct response to user actions.
         */
        @JvmField
        val SUCCESS: TextColor = color(0x65ff64)

        /**
         * Warning message color (#ffa64d).
         * Use to indicate potential issues or caution users about actions that may lead to errors.
         */
        @JvmField
        val WARNING: TextColor = color(0xffa64d)

        /**
         * Error message color (#ee3d51).
         * Use for error messages and critical issues resulting from user actions or system failures.
         */
        @JvmField
        val ERROR: TextColor = color(0xee3d51)

        /**
         * Variable key color (#6B9BD1).
         * Use for keys in key-value pairs (e.g., "Name: John").
         */
        @JvmField
        val VARIABLE_KEY: TextColor = color(0x6B9BD1)

        /**
         * Variable value color (#f9c353).
         * Use to highlight dynamic values in messages (e.g., "Your property 'PROPERTY' has been sold").
         */
        @JvmField
        val VARIABLE_VALUE: TextColor = color(0xf9c353)

        /**
         * Standard spacer color (GRAY).
         * Use for visual separators such as "-", "...", and "/".
         */
        @JvmField
        val SPACER: NamedTextColor = NamedTextColor.GRAY

        /**
         * Dark spacer color (DARK_GRAY).
         * Use for darker separators such as ">>" or "|" in prefixes.
         */
        @JvmField
        val DARK_SPACER: NamedTextColor = NamedTextColor.DARK_GRAY

        // -------------------- Prefix -------------------- //

        /**
         * Default prefix color (#3b92d1).
         * Applied to all message prefixes for consistency across Surf plugins.
         */
        @JvmField
        val PREFIX_COLOR: TextColor = PRIMARY

        /**
         * The default prefix character ('»').
         */
        @Suppress("MayBeConstant")
        @JvmField
        val PREFIX_CHARACTER = '»'

        private fun buildPrefix(color: TextColor) = buildText {
            text(PREFIX_CHARACTER, color)
            appendSpace()
            darkSpacer("|")
            appendSpace()
        }

        /**
         * Default message prefix used across all Surf plugins.
         */
        @JvmField
        val PREFIX: Component = buildPrefix(PREFIX_COLOR)

        /**
         * Prefix for informational messages.
         */
        @JvmField
        val INFO_PREFIX: Component = buildPrefix(INFO)

        /**
         * Prefix for success messages.
         */
        @JvmField
        val SUCCESS_PREFIX: Component = buildPrefix(SUCCESS)

        /**
         * Prefix for warning messages.
         */
        @JvmField
        val WARNING_PREFIX: Component = buildPrefix(WARNING)

        /**
         * Prefix for error messages.
         */
        @JvmField
        val ERROR_PREFIX: Component = buildPrefix(ERROR)

        // -------------------- Default Colors -------------------- //

        /**
         * Minecraft black color.
         */
        @JvmField
        val BLACK: NamedTextColor = NamedTextColor.BLACK

        /**
         * Minecraft dark blue color.
         */
        @JvmField
        val DARK_BLUE: NamedTextColor = NamedTextColor.DARK_BLUE

        /**
         * Minecraft dark green color.
         */
        @JvmField
        val DARK_GREEN: NamedTextColor = NamedTextColor.DARK_GREEN

        /**
         * Minecraft dark aqua color.
         */
        @JvmField
        val DARK_AQUA: NamedTextColor = NamedTextColor.DARK_AQUA

        /**
         * Minecraft dark red color.
         */
        @JvmField
        val DARK_RED: NamedTextColor = NamedTextColor.DARK_RED

        /**
         * Minecraft dark purple color.
         */
        @JvmField
        val DARK_PURPLE: NamedTextColor = NamedTextColor.DARK_PURPLE

        /**
         * Minecraft gold color.
         */
        @JvmField
        val GOLD: NamedTextColor = NamedTextColor.GOLD

        /**
         * Minecraft gray color.
         */
        @JvmField
        val GRAY: NamedTextColor = NamedTextColor.GRAY

        /**
         * Minecraft dark gray color.
         */
        @JvmField
        val DARK_GRAY: NamedTextColor = NamedTextColor.DARK_GRAY

        /**
         * Minecraft blue color.
         */
        @JvmField
        val BLUE: NamedTextColor = NamedTextColor.BLUE

        /**
         * Minecraft green color.
         */
        @JvmField
        val GREEN: NamedTextColor = NamedTextColor.GREEN

        /**
         * Minecraft aqua color.
         */
        @JvmField
        val AQUA: NamedTextColor = NamedTextColor.AQUA

        /**
         * Minecraft red color.
         */
        @JvmField
        val RED: NamedTextColor = NamedTextColor.RED

        /**
         * Minecraft light purple color.
         */
        @JvmField
        val LIGHT_PURPLE: NamedTextColor = NamedTextColor.LIGHT_PURPLE

        /**
         * Minecraft yellow color.
         */
        @JvmField
        val YELLOW: NamedTextColor = NamedTextColor.YELLOW

        /**
         * Minecraft white color.
         */
        @JvmField
        val WHITE: NamedTextColor = NamedTextColor.WHITE
    }
}
