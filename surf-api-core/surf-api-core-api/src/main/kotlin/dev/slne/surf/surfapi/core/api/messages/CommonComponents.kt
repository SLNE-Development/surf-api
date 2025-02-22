package dev.slne.surf.surfapi.core.api.messages

import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.ERROR
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.PREFIX
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.PRIMARY
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.SPACER
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.VARIABLE_KEY
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.VARIABLE_VALUE
import dev.slne.surf.surfapi.core.api.messages.CommonComponents.MAP_KEY_VALUE_SEPARATOR
import dev.slne.surf.surfapi.core.api.messages.adventure.appendNewline
import dev.slne.surf.surfapi.core.api.messages.adventure.appendText
import dev.slne.surf.surfapi.core.api.messages.adventure.clickOpensUrl
import dev.slne.surf.surfapi.core.api.messages.adventure.text
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import kotlin.time.Duration

/**
 * Builds a [TextComponent] using the provided [block] to configure the component.
 *
 * @param block The configuration block for the text component.
 * @return A built [TextComponent] instance.
 */
@PublishedApi
internal inline fun buildText0(block: TextComponent.Builder.() -> Unit): TextComponent {
    return Component.text().apply(block).build()
}

object CommonComponents {

    /**
     * An ellipsis (`...`).
     */
    @JvmField
    val ELLIPSIS = text("...")

    /**
     * A separator (`->`) used to visually separate key-value pairs in text components.
     */
    @JvmField
    val MAP_KEY_VALUE_SEPARATOR = text(" -> ", SPACER)

    /**
     * A separator (`:`) used to visually format time-related messages.
     */
    @JvmField
    val TIME_SEPARATOR = text(" : ", SPACER)

    /**
     * A clickable Discord link component (`discord.gg/castcrafter`).
     *
     * **Output Example:**
     * ```
     * discord.gg/castcrafter  (clickable)
     * ```
     */
    @JvmField
    val DISCORD_LINK = buildText0 {
        appendText("discord.gg/castcrafter", PRIMARY) {
            clickOpensUrl("https://discord.gg/castcrafter")
        }
    }

    /**
     * The standard header for disconnection messages.
     *
     * **Output Example:**
     * ```
     * CASTCRAFTER
     * COMMUNITY SERVER
     * ```
     */
    @JvmField
    val DISCONNECT_HEADER = buildText0 {
        appendText("CASTCRAFTER", PRIMARY)
        appendNewline()
        appendText("COMMUNITY SERVER", PRIMARY)
        appendNewline(2)
    }

    /**
     * A footer message indicating the user should try again later.
     *
     * **Output Example:**
     * ```
     * \
     * \
     * Bitte versuche es später erneut.
     * ```
     */
    @JvmField
    val DISCONNECT_FOOTER_TRY_AGAIN_LATER = buildText0 {
        appendNewline(2)
        appendText("Bitte versuche es später erneut.", ERROR)
    }

    /**
     * A footer message indicating the user should try again later and seek support if the issue persists.
     *
     * **Output Example:**
     * ```
     * \
     * \
     * Bitte versuche es später erneut.
     * Sollte das Problem weiterhin bestehen,
     * kontaktiere den Support in unserem Discord.
     * \
     * \
     * discord.gg/castcrafter (clickable)
     * ```
     */
    @JvmField
    val DISCONNECT_FOOTER_TRY_AGAIN_LATER_ISSUE = buildText0 {
        append(DISCONNECT_FOOTER_TRY_AGAIN_LATER)
        appendNewline()
        appendText("Sollte das Problem weiterhin bestehen,", ERROR)
        appendNewline()
        appendText("kontaktiere den Support in unserem Discord.", ERROR)
        appendNewline(2)
        append(DISCORD_LINK)
    }

    /**
     * Renders a structured message for when a player is kicked from the server.
     *
     * @param messageRenderer A block to render the main message.
     * @param footerRenderer A block to render the footer message.
     * @return The formatted disconnect message.
     *
     * **Output Example:**
     * ```
     * CASTCRAFTER
     * COMMUNITY SERVER
     *
     * DU WURDEST VOM SERVER GEWORFEN
     * \
     * \
     * \
     * [Custom message]
     * \
     * \
     * \
     * [Footer message]
     * ```
     */
    inline fun renderKickDisconnectMessage(
        messageRenderer: TextComponent.Builder.() -> Unit,
        footerRenderer: TextComponent.Builder.() -> Unit = { },
    ) = renderKickDisconnectMessage(Component.text(), messageRenderer, footerRenderer)

    /**
     * Renders a structured message for when a player is kicked from the server.
     *
     * @param builder The builder to use for the message.
     * @param messageRenderer A block to render the main message.
     * @param footerRenderer A block to render the footer message.
     * @return The formatted disconnect message.
     *
     * **Output Example:**
     * ```
     * CASTCRAFTER
     * COMMUNITY SERVER
     *
     * DU WURDEST VOM SERVER GEWORFEN
     * \
     * \
     * \
     * [Custom message]
     * \
     * \
     * \
     * [Footer message]
     * ```
     */
    inline fun <B : TextComponent.Builder> renderKickDisconnectMessage(
        builder: B,
        messageRenderer: B.() -> Unit,
        footerRenderer: B.() -> Unit = { },
    ): TextComponent {
        with(builder) {
            append(DISCONNECT_HEADER)
            appendText("DU WURDEST VOM SERVER GEWORFEN", ERROR)
            appendNewline(3)
            messageRenderer()
            appendNewline(3)
            footerRenderer()
        }

        return builder.build()
    }

    /**
     * Renders a structured message for when a player is kicked from the server.
     *
     * @param messageRenderer A block to render the main message.
     * @param issue Whether the message should include an issue-related footer.
     * @return The formatted disconnect message.
     */
    inline fun renderKickDisconnectMessage(
        messageRenderer: TextComponent.Builder.() -> Unit,
        issue: Boolean,
    ) = renderKickDisconnectMessage(Component.text(), messageRenderer, issue)

    /**
     * Renders a structured message for when a player is kicked from the server.
     *
     * @param builder The builder to use for the message.
     * @param messageRenderer A block to render the main message.
     * @param issue Whether the message should include an issue-related footer.
     * @return The formatted disconnect message.
     */
    inline fun <B : TextComponent.Builder> renderKickDisconnectMessage(
        builder: B,
        messageRenderer: B.() -> Unit,
        issue: Boolean,
    ) = renderKickDisconnectMessage(builder, messageRenderer) {
        if (issue) append(DISCONNECT_FOOTER_TRY_AGAIN_LATER_ISSUE)
        else append(DISCONNECT_FOOTER_TRY_AGAIN_LATER)
    }

    /**
     * Renders a structured message for when a player is disconnected from the server.
     *
     * @param disconnectReason The reason for the disconnection.
     * @param suggestHelp A block to render the help message.
     * @param footerRenderer A block to render the footer message.
     * @return The formatted disconnect message.
     *
     * **Output Example:**
     * ```
     * CASTCRAFTER
     * COMMUNITY SERVER
     *
     * DU WURDEST VOM SERVER GEWORFEN
     * \
     * \
     * \
     * [Help message]
     * \
     * \
     * \
     * [Footer message]
     * ```
     */
    inline fun renderDisconnectMessage(
        disconnectReason: @NoLowercase String,
        suggestHelp: TextComponent.Builder.() -> Unit,
        footerRenderer: TextComponent.Builder.() -> Unit = { },
    ) = renderDisconnectMessage(Component.text(), disconnectReason, suggestHelp, footerRenderer)

    /**
     * Renders a structured message for when a player is disconnected from the server.
     *
     * @param builder The builder to use for the message.
     * @param disconnectReason The reason for the disconnection.
     * @param suggestHelp A block to render the help message.
     * @param footerRenderer A block to render the footer message.
     * @return The formatted disconnect message.
     *
     * **Output Example:**
     * ```
     * CASTCRAFTER
     * COMMUNITY SERVER
     *
     * DU WURDEST VOM SERVER GEWORFEN
     * \
     * \
     * \
     * [Help message]
     * \
     * \
     * \
     * [Footer message]
     * ```
     */
    inline fun <B : TextComponent.Builder> renderDisconnectMessage(
        builder: B,
        disconnectReason: @NoLowercase String,
        suggestHelp: B.() -> Unit,
        footerRenderer: B.() -> Unit = { },
    ): TextComponent {
        with(builder) {
            append(DISCONNECT_HEADER)
            appendText(disconnectReason.uppercase(), VARIABLE_VALUE)
            appendNewline(3)
            suggestHelp()
            appendNewline(3)
            footerRenderer()
        }

        return builder.build()
    }

    /**
     * Renders a structured message for when a player is disconnected from the server.
     *
     * @param disconnectReason The reason for the disconnection.
     * @param suggestHelp A block to render the help message.
     * @param issue Whether the message should include an issue-related footer.
     * @return The formatted disconnect message.
     */
    inline fun renderDisconnectMessage(
        disconnectReason: @NoLowercase String,
        suggestHelp: TextComponent.Builder.() -> Unit,
        issue: Boolean,
    ) = renderDisconnectMessage(Component.text(), disconnectReason, suggestHelp, issue)

    /**
     * Renders a structured message for when a player is disconnected from the server.
     *
     * @param builder The builder to use for the message.
     * @param disconnectReason The reason for the disconnection.
     * @param suggestHelp A block to render the help message.
     * @param issue Whether the message should include an issue-related footer.
     * @return The formatted disconnect message.
     */
    inline fun <B : TextComponent.Builder> renderDisconnectMessage(
        builder: B,
        disconnectReason: @NoLowercase String,
        suggestHelp: B.() -> Unit,
        issue: Boolean,
    ) = renderDisconnectMessage(builder, disconnectReason, suggestHelp) {
        if (issue) append(DISCONNECT_FOOTER_TRY_AGAIN_LATER_ISSUE)
        else append(DISCONNECT_FOOTER_TRY_AGAIN_LATER)
    }

    /**
     * Formats a collection into a comma-separated list.
     *
     * @param collection The collection to format.
     * @param formatter A function to format each element.
     * @return A [Component] representing the formatted collection.
     *
     * **Example Usage:**
     * ```kotlin
     * val list = listOf("Apple", "Banana", "Cherry")
     * val formatted = formatCollection(list) { text(it) }
     * ```
     *
     * **Output Example:**
     * ```
     * Apple, Banana, Cherry
     * ```
     */
    inline fun <E> formatCollection(
        collection: Iterable<E>,
        formatter: (E) -> Component,
    ): Component {
        val joinConfig = JoinConfiguration.builder()
            .separator(Component.text(", ", SPACER))
            .build()

        return Component.join(joinConfig, collection.mapTo(mutableObjectListOf(), formatter))
    }

    /**
     * Formats a collection into a structured text representation with each element on a new line.
     *
     * @param collection The collection to format.
     * @param linePrefix The prefix for each line.
     * @param formatter A function to format each element.
     * @return A [Component] representing the formatted collection.
     *
     * **Example Usage:**
     * ```kotlin
     * val list = listOf("Apple", "Banana", "Cherry")
     * val formatted = formatCollectionNewLine(list, PREFIX) { text(it) }
     * ```
     *
     * **Output Example:**
     * ```
     * >> Surf | - Apple
     * >> Surf | - Banana
     * >> Surf | - Cherry
     * ```
     */
    inline fun <E> formatCollectionNewLine(
        collection: Iterable<E>,
        linePrefix: Component = PREFIX,
        formatter: (E) -> Component,
    ): Component {
        val joinConfig = JoinConfiguration.builder()
            .separator(buildText0 {
                appendNewline()
                append(linePrefix)
                appendText("-  ", SPACER)
            })
            .build()

        return Component.join(joinConfig, collection.mapTo(mutableObjectListOf(), formatter))
    }

    /**
     * Formats a map into a structured text representation.
     *
     * @param map The map to format.
     * @param keyFormatter A function to format the keys.
     * @param valueFormatter A function to format the values.
     * @param linePrefix The prefix for each line.
     * @param keyValueSeparator The separator between keys and values.
     * @return A formatted [Component].
     *
     * **Example Usage:**
     * ```kotlin
     * val data = mapOf("Name" to "Alice", "Age" to "25")
     * val formatted = formatMap(data, { text(it) }, { text(it) }, PREFIX, MAP_KEY_VALUE_SEPARATOR)
     * ```
     *
     * **Output Example:**
     * ```
     * >> Surf | - Name -> Alice
     * >> Surf | - Age -> 25
     * ```
     */
    inline fun <K, V> formatMap(
        map: Map<K, V>,
        keyFormatter: (K) -> Component,
        valueFormatter: (V) -> Component,
        linePrefix: Component = PREFIX,
        keyValueSeparator: Component = MAP_KEY_VALUE_SEPARATOR,
    ): Component {
        val joinConfig = JoinConfiguration.builder()
            .separator(buildText0 {
                appendNewline()
                append(linePrefix)
                appendText("-  ", SPACER)
            })

        return Component.join(joinConfig, map.mapTo(mutableObjectListOf()) { (key, value) ->
            buildText0 {
                append(keyFormatter(key).colorIfAbsent(VARIABLE_KEY))
                append(keyValueSeparator)
                append(valueFormatter(value).colorIfAbsent(VARIABLE_VALUE))
            }
        })
    }

    /**
     * Formats a duration into a human-readable time representation.
     *
     * @param time The duration to format.
     * @param showSeconds Whether to include seconds in the output.
     * @param shortForms Whether to use short forms for time units.
     * @param separator The separator between time units.
     * @param timeColor The color for the time values.
     * @return A formatted [Component] representing the time.
     *
     * **Example Usage:**
     * ```kotlin
     * val time = Duration.ofSeconds(123456)
     * val formatted = formatTime(time, showSeconds = true, shortForms = true)
     * ```
     * **Output Example:**
     * ```
     * 1d:10h:17m:36s
     * ```
     */
    fun formatTime(
        time: Duration,
        showSeconds: Boolean,
        shortForms: Boolean,
        separator: Component = TIME_SEPARATOR,
        timeColor: TextColor = VARIABLE_VALUE,
    ): Component {
        data class Formatter(val shortForms: Boolean, val timeColor: TextColor) {
            operator fun invoke(time: Long, longForm: String, shortForm: String) = buildText0 {
                append(Component.text(time, timeColor))
                appendText(if (shortForms) shortForm else " $longForm", timeColor)
            }
        }

        val formatter = Formatter(shortForms, timeColor)

        val centuries = time.inWholeDays / 365 / 100
        val decades = time.inWholeDays / 365 % 100
        val years = time.inWholeDays / 365 % 10
        val days = time.inWholeDays % 365
        val hours = time.inWholeHours % 24
        val minutes = time.inWholeMinutes % 60
        val seconds = time.inWholeSeconds % 60

        return buildText0 {
            if (centuries > 0) append(formatter(centuries, "Jahrhundert", "Jh"))
            if (decades > 0) {
                append(separator)
                append(formatter(decades, "Jahrzehnt", "Jz"))
            }
            if (years > 0) {
                append(separator)
                append(formatter(years, "Jahr", "J"))
            }
            if (days > 0) {
                append(separator)
                append(formatter(days, "Tag", "d"))
            }
            if (hours > 0) {
                append(separator)
                append(formatter(hours, "Stunde", "h"))
            }
            if (minutes > 0) {
                append(separator)
                append(formatter(minutes, "Minute", "m"))
            }
            if (showSeconds) {
                append(separator)
                append(formatter(seconds, "Sekunde", "s"))
            }
        }
    }
}

/**
 * @see CommonComponents.formatCollection
 */
inline fun <E> Iterable<E>.joinToComponent(formatter: (E) -> Component) =
    CommonComponents.formatCollection(this, formatter)

/**
 * @see CommonComponents.formatCollectionNewLine
 */
inline fun <E> Iterable<E>.joinToComponentNewLine(
    linePrefix: Component = PREFIX,
    formatter: (E) -> Component,
) = CommonComponents.formatCollectionNewLine(this, linePrefix, formatter)

/**
 * @see CommonComponents.formatMap
 */
inline fun <K, V> Map<K, V>.joinToComponent(
    keyFormatter: (K) -> Component,
    valueFormatter: (V) -> Component,
    linePrefix: Component = PREFIX,
    keyValueSeparator: Component = MAP_KEY_VALUE_SEPARATOR,
) = CommonComponents.formatMap(this, keyFormatter, valueFormatter, linePrefix, keyValueSeparator)