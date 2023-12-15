package dev.slne.surf.surfapi.core.api.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

@SuppressWarnings("unused")
public interface Colors {
    // -------------------- Surf Colors -------------------- //
    /**
     * PRIMARY color (#3b92d1).
     * Generally not used in our system. However, an example of its use could be for titles, subtitles, etc.
     */
    TextColor PRIMARY = TextColor.color(0x3b92d1);
    /**
     * SECONDARY color (#5b5b5b).
     * Also seldom used in our system. Could be used for elements like subtitles.
     */
    TextColor SECONDARY = TextColor.color(0x5b5b5b);

    /**
     * INFO color (#40d1db).
     * Used to inform the user about a specific situation. Typically, it's not a follow-up to a user action.
     * Exceptions include queued user actions that update after a delay (e.g., status changes not related to Success or Danger),
     * and for toggle messages (e.g., "You have [deactivated/activated] the chat").
     */
    TextColor INFO = TextColor.color(0x40d1db);
    /**
     * SUCCESS color (#65ff64).
     * Indicates a positive outcome of an action performed by the user. Used only in direct response to the user.
     */
    TextColor SUCCESS = TextColor.color(0x65ff64);
    /**
     * WARNING color (#f9c353).
     * Used as a direct warning to the user. This could be a response to a user action or a system notification,
     * and serves as a precursor to Danger.
     */
    TextColor WARNING = TextColor.color(0xf9c353);
    /**
     * ERROR (or DANGER) color (#ee3d51).
     * Represents error messages directed at the user. These can follow a direct action by the user,
     * or serve as a warning about potential issues.
     */
    TextColor ERROR = TextColor.color(0xee3d51);

    /**
     * VARIABLE_KEY color (#3b92d1).
     * Mainly used as a key in listings (e.g., "Key 1: Value", "Key 2: Value", etc.).
     */
    TextColor VARIABLE_KEY = TextColor.color(0x3b92d1);
    /**
     * VARIABLE_VALUE color (#f9c353).
     * Primarily used in listings and chat messages as a variable (e.g., "Your property [PROPERTY] has been sold.").
     */
    TextColor VARIABLE_VALUE = TextColor.color(0xf9c353);

    /**
     * SPACER color (GRAY).
     * Used for various forms of spacers, such as "-, ..., /, etc."
     */
    NamedTextColor SPACER = NamedTextColor.GRAY;
    /**
     * DARK_SPACER color (DARK_GRAY).
     * Used for dark spacers, such as those needed in prefixes like ">>", "|", etc.
     */
    NamedTextColor DARK_SPACER = NamedTextColor.DARK_GRAY;
    /**
     * PREFIX color (#3b92d1).
     * Used for the color of every prefix to provide a uniform appearance.
     */
    TextColor PREFIX_COLOR = TextColor.color(0x3b92d1);
    // ----------------------------------------------------- //

    // -------------------- Default Colors -------------------- //
    /**
     * Represents the color black.
     */
    NamedTextColor BLACK = NamedTextColor.BLACK;
    /**
     * Represents the color dark blue.
     */
    NamedTextColor DARK_BLUE = NamedTextColor.DARK_BLUE;
    /**
     * Represents the color dark green.
     */
    NamedTextColor DARK_GREEN = NamedTextColor.DARK_GREEN;
    /**
     * Represents the named text color DARK_AQUA.
     */
    NamedTextColor DARK_AQUA = NamedTextColor.DARK_AQUA;
    /**
     * Represents the dark red color.
     */
    NamedTextColor DARK_RED = NamedTextColor.DARK_RED;
    /**
     * Represents the dark purple named text color.
     */
    NamedTextColor DARK_PURPLE = NamedTextColor.DARK_PURPLE;
    /**
     * The GOLD color for naming text.
     */
    NamedTextColor GOLD = NamedTextColor.GOLD;
    /**
     * Represents the named text color "GRAY".
     */
    NamedTextColor GRAY = NamedTextColor.GRAY;
    /**
     * Represents the named text color "DARK_GRAY".
     */
    NamedTextColor DARK_GRAY = NamedTextColor.DARK_GRAY;
    /**
     * Represents the named text color "BLUE".
     */
    NamedTextColor BLUE = NamedTextColor.BLUE;
    /**
     * Represents the named text color "GREEN".
     */
    NamedTextColor GREEN = NamedTextColor.GREEN;
    /**
     * Represents the named text color "AQUA".
     */
    NamedTextColor AQUA = NamedTextColor.AQUA;
    /**
     * Represents the named text color "RED".
     */
    NamedTextColor RED = NamedTextColor.RED;
    /**
     * Represents the color light purple.
     */
    NamedTextColor LIGHT_PURPLE = NamedTextColor.LIGHT_PURPLE;
    /**
     * Represents the color yellow.
     */
    NamedTextColor YELLOW = NamedTextColor.YELLOW;
    /**
     * Represents the color white.
     */
    NamedTextColor WHITE = NamedTextColor.WHITE;
    // --------------------------------------------------------- //

    // -------------------- Prefix -------------------- //
    /**
     * The prefix for all Surf plugins
     */
    Component PREFIX = Component.text(">> ", DARK_SPACER)
            .append(Component.text("Surf", PREFIX_COLOR))
            .append(Component.text(" | ", DARK_SPACER));
    // ------------------------------------------------ //
}
