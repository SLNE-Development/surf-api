package dev.slne.surf.surfapi.core.api.messages

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextColor.color

/**
 * A class that contains all the colors used in the Surf system. This class is used to provide a
 * uniform appearance across all Surf plugins.
 * [Simons dc post](https://discord.com/channels/1094422317783851108/1096084922499862658)
 *
 * @see [Simons dc post](https://discord.com/channels/1094422317783851108/1096084922499862658)
 */
@Suppress("unused")
interface Colors {
    companion object {
        // -------------------- Surf Colors -------------------- //
        /**
         * PRIMARY color (#3b92d1). Generally not used in our system. However, an example of its use could
         * be for titles, subtitles, etc.
         */
        @JvmField
        val PRIMARY: TextColor = color(0x3b92d1)

        /**
         * SECONDARY color (#5b5b5b). Also seldom used in our system. Could be used for elements like
         * subtitles.
         */
        @JvmField
        val SECONDARY: TextColor = color(0x5b5b5b)

        /**
         * INFO color (#40d1db). Used to inform the user about a specific situation. Typically, it's not a
         * follow-up to a user action. Exceptions include queued user actions that update after a delay
         * (e.g., status changes not related to Success or Danger), and for toggle messages (e.g., "You
         * have [deactivated/activated] the chat").
         */
        @JvmField
        val INFO: TextColor = color(0x40d1db)

        /**
         * SUCCESS color (#65ff64). Indicates a positive outcome of an action performed by the user. Used
         * only in direct response to the user.
         */
        @JvmField
        val SUCCESS: TextColor = color(0x65ff64)

        /**
         * WARNING color (#f9c353). Used as a direct warning to the user. This could be a response to a
         * user action or a system notification, and serves as a precursor to Danger.
         */
        @JvmField
        val WARNING: TextColor = color(0xf9c353)

        /**
         * ERROR (or DANGER) color (#ee3d51). Represents error messages directed at the user. These can
         * follow a direct action by the user, or serve as a warning about potential issues.
         */
        @JvmField
        val ERROR: TextColor = color(0xee3d51)

        /**
         * VARIABLE_KEY color (#3b92d1). Mainly used as a key in listings (e.g., "Key 1: Value", "Key 2:
         * Value", etc.).
         */
        @JvmField
        val VARIABLE_KEY: TextColor = INFO

        /**
         * VARIABLE_VALUE color (#f9c353). Primarily used in listings and chat messages as a variable
         * (e.g., "Your property [PROPERTY] has been sold.").
         */
        @JvmField
        val VARIABLE_VALUE: TextColor = WARNING

        /**
         * SPACER color (GRAY). Used for various forms of spacers, such as "-, ..., /, etc."
         */
        @JvmField
        val SPACER: NamedTextColor = NamedTextColor.GRAY

        /**
         * DARK_SPACER color (DARK_GRAY). Used for dark spacers, such as those needed in prefixes like
         * ">>", "|", etc.
         */
        @JvmField
        val DARK_SPACER: NamedTextColor = NamedTextColor.DARK_GRAY

        /**
         * PREFIX color (#3b92d1). Used for the color of every prefix to provide a uniform appearance.
         */
        @JvmField
        val PREFIX_COLOR: TextColor = PRIMARY

        // ----------------------------------------------------- //
        // -------------------- Default Colors -------------------- //
        /**
         * The prefix for all Surf plugins
         */
        @JvmField
        val PREFIX: Component = Component.text(">> ", DARK_SPACER)
            .append(Component.text("Surf", PREFIX_COLOR))
            .append(Component.text(" | ", DARK_SPACER))

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
         * Represents the named text color DARK_AQUA.
         */
        @JvmField
        val DARK_AQUA: NamedTextColor = NamedTextColor.DARK_AQUA

        /**
         * Represents the dark red color.
         */
        @JvmField
        val DARK_RED: NamedTextColor = NamedTextColor.DARK_RED

        /**
         * Represents the dark purple named text color.
         */
        @JvmField
        val DARK_PURPLE: NamedTextColor = NamedTextColor.DARK_PURPLE

        /**
         * The GOLD color for naming text.
         */
        @JvmField
        val GOLD: NamedTextColor = NamedTextColor.GOLD

        /**
         * Represents the named text color "GRAY".
         */
        @JvmField
        val GRAY: NamedTextColor = NamedTextColor.GRAY

        /**
         * Represents the named text color "DARK_GRAY".
         */
        @JvmField
        val DARK_GRAY: NamedTextColor = NamedTextColor.DARK_GRAY

        /**
         * Represents the named text color "BLUE".
         */
        @JvmField
        val BLUE: NamedTextColor = NamedTextColor.BLUE

        /**
         * Represents the named text color "GREEN".
         */
        @JvmField
        val GREEN: NamedTextColor = NamedTextColor.GREEN

        /**
         * Represents the named text color "AQUA".
         */
        @JvmField
        val AQUA: NamedTextColor = NamedTextColor.AQUA

        /**
         * Represents the named text color "RED".
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

        // --------------------------------------------------------- //
        // -------------------- Prefix -------------------- //
        /**
         * Represents the color white.
         */
        @JvmField
        val WHITE: NamedTextColor = NamedTextColor.WHITE
        // ------------------------------------------------ //
    }
}
