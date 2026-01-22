package dev.slne.surf.surfapi.core.api.messages

import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.INFO
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.PRIMARY
import dev.slne.surf.surfapi.core.api.messages.adventure.buildText
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextColor.color

/**
 * A class that defines all the colors used in the Surf system, ensuring a consistent visual style
 * across all Surf plugins.
 * This class provides predefined colors for various UI elements, including
 * informational messages, warnings, errors, and formatting components.
 *
 * For reference,
 * see [Simons dc post](https://discord.com/channels/1094422317783851108/1096084922499862658).
 *
 * @see [Simons dc post](https://discord.com/channels/1094422317783851108/1096084922499862658)
 */
@Suppress("unused")
interface Colors {
    companion object {
        // -------------------- Surf Colors -------------------- //

        /**
         * The primary Surf color (#3b92d1).
         * Although rarely used in the system, it can be utilized
         * for elements like titles and subtitles.
         */
        @JvmField
        val PRIMARY: TextColor = color(0x3b92d1)

        /**
         * The secondary Surf color (#5b5b5b), mainly used for elements such as subtitles.
         */
        @JvmField
        val SECONDARY: TextColor = color(0x5b5b5b)

        /**
         * The info color (#40d1db).
         * Used to convey neutral information to users that is not directly
         * a result of their actions,
         * except in cases such as delayed status updates or toggle messages.
         */
        @JvmField
        val INFO: TextColor = color(0x97B3F7)

        /**
         * The note color (#6EA6D9).
         * Used for supplemental or side-note information that adds context,
         * tips, or clarifications to a primary message.
         * Prefer this for ancillary guidance rather than main content:
         * use [INFO] for neutral system messages or status updates, and [PRIMARY]
         * for branded elements such as titles or key headings.
         */
        @JvmField
        val NOTE: TextColor = color(0x6EA6D9)

        /**
         * The success color (#65ff64).
         * Indicates a positive outcome of a user action and is always
         * used in direct response to the user.
         */
        @JvmField
        val SUCCESS: TextColor = color(0x65ff64)

        /**
         * The warning color (#f9c353).
         * Used to caution users about potential issues, often serving
         * as a precursor to an error message.
         */
        @JvmField
        val WARNING: TextColor = color(0xf9c353)

        /**
         * The error (or danger) color (#ee3d51).
         * Represents error messages directed at the user,
         * often following a direct user action or warning of a critical issue.
         */
        @JvmField
        val ERROR: TextColor = color(0xee3d51)

        /**
         * The variable key color (#3b92d1).
         * Typically used for key-value pair representations,
         * such as in lists (for example, "Key 1: Value").
         */
        @JvmField
        val VARIABLE_KEY: TextColor = INFO

        /**
         * The variable value color (#f9c353).
         * Commonly used to highlight values in lists and
         * chat messages (for example, "Your property 'PROPERTY' has been sold.").
         */
        @JvmField
        val VARIABLE_VALUE: TextColor = WARNING

        /**
         * The spacer color (GRAY). Used for visual separators such as "-", "...", and "/".
         */
        @JvmField
        val SPACER: NamedTextColor = NamedTextColor.GRAY

        /**
         * The dark spacer color (DARK_GRAY).
         * Used for darker separators, such as those found in
         * prefixes like ">>" or "|".
         */
        @JvmField
        val DARK_SPACER: NamedTextColor = NamedTextColor.DARK_GRAY

        /**
         * The default prefix color (#3b92d1).
         * Applied to all prefixes for consistency across Surf plugins.
         */
        @JvmField
        val PREFIX_COLOR: TextColor = PRIMARY

        // -------------------- Default Colors -------------------- //

        /**
         * The default prefix used across all Surf plugins, ensuring a recognizable and uniform
         * identifier in messages.
         */
        @JvmField
        val PREFIX: Component = buildText {
            spacer("»")
            appendSpace()
        }

        /**
         * The default info prefix used in informational messages.
         */
        @JvmField
        val INFO_PREFIX: Component = buildText {
            spacer("[")
            info("ℹ️")
            spacer("]")
            appendSpace()
        }

        /**
         * The default success prefix used in success messages.
         */
        @JvmField
        val SUCCESS_PREFIX: Component = buildText {
            spacer("[")
            success("✔")
            spacer("]")
            appendSpace()
        }

        /**
         * The default warning prefix used in warning messages.
         */
        @JvmField
        val WARNING_PREFIX: Component = buildText {
            spacer("[")
            warning("⚠")
            spacer("]")
            appendSpace()
        }

        /**
         * The default error prefix used in error messages.
         */
        @JvmField
        val ERROR_PREFIX: Component = buildText {
            spacer("[")
            error("✖")
            spacer("]")
            appendSpace()
        }

        /**
         * Represents the color black.
         */
        @JvmField
        val BLACK: NamedTextColor = NamedTextColor.BLACK

        /**
         * Represents the color dark blue.
         */
        @JvmField
        val DARK_BLUE: NamedTextColor = NamedTextColor.DARK_BLUE

        /**
         * Represents the color dark green.
         */
        @JvmField
        val DARK_GREEN: NamedTextColor = NamedTextColor.DARK_GREEN

        /**
         * Represents the color dark aqua.
         */
        @JvmField
        val DARK_AQUA: NamedTextColor = NamedTextColor.DARK_AQUA

        /**
         * Represents the color dark red.
         */
        @JvmField
        val DARK_RED: NamedTextColor = NamedTextColor.DARK_RED

        /**
         * Represents the color dark purple.
         */
        @JvmField
        val DARK_PURPLE: NamedTextColor = NamedTextColor.DARK_PURPLE

        /**
         * Represents the color gold.
         */
        @JvmField
        val GOLD: NamedTextColor = NamedTextColor.GOLD

        /**
         * Represents the color gray.
         */
        @JvmField
        val GRAY: NamedTextColor = NamedTextColor.GRAY

        /**
         * Represents the color dark gray.
         */
        @JvmField
        val DARK_GRAY: NamedTextColor = NamedTextColor.DARK_GRAY

        /**
         * Represents the color blue.
         */
        @JvmField
        val BLUE: NamedTextColor = NamedTextColor.BLUE

        /**
         * Represents the color green.
         */
        @JvmField
        val GREEN: NamedTextColor = NamedTextColor.GREEN

        /**
         * Represents the color aqua.
         */
        @JvmField
        val AQUA: NamedTextColor = NamedTextColor.AQUA

        /**
         * Represents the color red.
         */
        @JvmField
        val RED: NamedTextColor = NamedTextColor.RED

        /**
         * Represents the color light purple.
         */
        @JvmField
        val LIGHT_PURPLE: NamedTextColor = NamedTextColor.LIGHT_PURPLE

        /**
         * Represents the color yellow.
         */
        @JvmField
        val YELLOW: NamedTextColor = NamedTextColor.YELLOW

        /**
         * Represents the color white.
         */
        @JvmField
        val WHITE: NamedTextColor = NamedTextColor.WHITE
    }
}
