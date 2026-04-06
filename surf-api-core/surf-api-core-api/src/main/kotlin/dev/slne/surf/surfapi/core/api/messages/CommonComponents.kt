package dev.slne.surf.surfapi.core.api.messages

import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.ERROR
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.PREFIX
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.PRIMARY
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.SPACER
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.VARIABLE_KEY
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.VARIABLE_VALUE
import dev.slne.surf.surfapi.core.api.messages.CommonComponents.MAP_SEPARATOR
import dev.slne.surf.surfapi.core.api.messages.CommonComponents.MAP_SEPERATOR
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
 * Builds a [TextComponent] using the provided configuration block.
 *
 * @param block Configuration block for the text component builder.
 * @return A built [TextComponent] instance.
 */
@PublishedApi
internal inline fun buildText0(block: TextComponent.Builder.() -> Unit): TextComponent {
    return Component.text().apply(block).build()
}

/**
 * Provides common text components and formatting utilities for messages.
 */
object CommonComponents {

    /**
     * An ellipsis component (`...`).
     */
    @JvmField
    val ELLIPSIS = text("...")


    /**
     * A separator (`->`) used to visually separate key-value pairs in text components.
     */
    @JvmField
    val MAP_SEPARATOR = text(" -> ", SPACER)

    /**
     * @deprecated Use [MAP_SEPARATOR] instead.
     */
    @Deprecated("Use MAP_SEPARATOR instead", ReplaceWith("MAP_SEPARATOR"))
    @JvmField
    val MAP_SEPERATOR = MAP_SEPARATOR

    /**
     * A separator component (` : `) used to visually format time-related messages.
     */
    @JvmField
    val TIME_SEPARATOR = text(" : ", SPACER)

    /**
     * An em dash component (`—`) used as a visual separator.
     */
    @JvmField
    val EM_DASH = text("—", SPACER)

    @Suppress("FunctionName")
    @Deprecated("Binary compatibility", ReplaceWith("EM_DASH"), DeprecationLevel.HIDDEN)
    fun getEM_DASH() = EM_DASH

    /**
     * A clickable Discord link component (`discord.gg/castcrafter`).
     */
    @JvmField
    val DISCORD_LINK = buildText0 {
        appendText("discord.gg/castcrafter", PRIMARY) {
            clickOpensUrl("https://discord.gg/castcrafter")
        }
    }

    /**
     * The standard header for disconnection messages displaying the server name.
     *
     * **Output:**
     * ```
     * CASTCRAFTER
     * COMMUNITY SERVER
     * <empty>
     * <empty>
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
     * **Output:**
     * ```
     * <empty>
     * <empty>
     * Bitte versuche es später erneut.
     * ```
     */
    @JvmField
    val RETRY_LATER_FOOTER = buildText0 {
        appendNewline(2)
        appendText("Bitte versuche es später erneut.", ERROR)
    }

    /**
     * A footer message indicating the user should try again later and contact support if the issue persists.
     *
     * **Output:**
     * ```
     * <empty>
     * <empty>
     * Bitte versuche es später erneut.
     * Sollte das Problem weiterhin bestehen,
     * kontaktiere den Support in unserem Discord.
     * <empty>
     * <empty>
     * discord.gg/castcrafter (clickable)
     * ```
     */
    @JvmField
    val ISSUE_FOOTER = buildText0 {
        append(RETRY_LATER_FOOTER)
        appendNewline()
        appendText("Sollte das Problem weiterhin bestehen,", ERROR)
        appendNewline()
        appendText("kontaktiere den Support in unserem Discord.", ERROR)
        appendNewline(2)
        append(DISCORD_LINK)
    }

    /**
     * Renders a structured kick message with custom content and footer.
     *
     * @param messageRenderer Block to render the main message content.
     * @param footerRenderer Block to render the footer content.
     * @return The formatted disconnect message component.
     *
     * **Output Example:**
     * ```
     * CASTCRAFTER
     * COMMUNITY SERVER
     * <empty>
     * <empty>
     * DU WURDEST VOM SERVER GEWORFEN
     * <empty>
     * <empty>
     * <empty>
     * [Custom message content]
     * <empty>
     * <empty>
     * <empty>
     * [Footer content]
     * ```
     */
    inline fun renderKickDisconnectMessage(
        messageRenderer: TextComponent.Builder.() -> Unit,
        footerRenderer: TextComponent.Builder.() -> Unit = { },
    ) = renderKickDisconnectMessage(Component.text(), messageRenderer, footerRenderer)

    /**
     * Renders a structured kick message with custom content and footer using a provided builder.
     *
     * @param B The type of builder.
     * @param builder The builder to use for constructing the message.
     * @param messageRenderer Block to render the main message content.
     * @param footerRenderer Block to render the footer content.
     * @return The formatted disconnect message component.
     *
     * **Output Example:**
     * ```
     * CASTCRAFTER
     * COMMUNITY SERVER
     * <empty>
     * <empty>
     * DU WURDEST VOM SERVER GEWORFEN
     * <empty>
     * <empty>
     * <empty>
     * [Custom message content]
     * <empty>
     * <empty>
     * <empty>
     * [Footer content]
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
     * Renders a structured kick message with an automatic issue or retry footer.
     *
     * @param messageRenderer Block to render the main message content.
     * @param issue Whether to include an issue-related footer instead of a simple retry footer.
     * @return The formatted disconnect message component.
     *
     * **Output Example (issue = true):**
     * ```
     * CASTCRAFTER
     * COMMUNITY SERVER
     * <empty>
     * <empty>
     * DU WURDEST VOM SERVER GEWORFEN
     * <empty>
     * <empty>
     * <empty>
     * [Custom message content]
     * <empty>
     * <empty>
     * <empty>
     * <empty>
     * <empty>
     * Bitte versuche es später erneut.
     * Sollte das Problem weiterhin bestehen,
     * kontaktiere den Support in unserem Discord.
     * <empty>
     * <empty>
     * discord.gg/castcrafter (clickable)
     * ```
     *
     * **Output Example (issue = false):**
     * ```
     * CASTCRAFTER
     * COMMUNITY SERVER
     * <empty>
     * <empty>
     * DU WURDEST VOM SERVER GEWORFEN
     * <empty>
     * <empty>
     * <empty>
     * [Custom message content]
     * <empty>
     * <empty>
     * <empty>
     * <empty>
     * <empty>
     * Bitte versuche es später erneut.
     * ```
     */
    inline fun renderKickDisconnectMessage(
        messageRenderer: TextComponent.Builder.() -> Unit,
        issue: Boolean,
    ) = renderKickDisconnectMessage(Component.text(), messageRenderer, issue)

    /**
     * Renders a structured kick message with an automatic issue or retry footer using a provided builder.
     *
     * @param B The type of builder.
     * @param builder The builder to use for constructing the message.
     * @param messageRenderer Block to render the main message content.
     * @param issue Whether to include an issue-related footer instead of a simple retry footer.
     * @return The formatted disconnect message component.
     *
     * **Output Example (issue = true):**
     * ```
     * CASTCRAFTER
     * COMMUNITY SERVER
     * <empty>
     * <empty>
     * DU WURDEST VOM SERVER GEWORFEN
     * <empty>
     * <empty>
     * <empty>
     * [Custom message content]
     * <empty>
     * <empty>
     * <empty>
     * <empty>
     * <empty>
     * Bitte versuche es später erneut.
     * Sollte das Problem weiterhin bestehen,
     * kontaktiere den Support in unserem Discord.
     * <empty>
     * <empty>
     * discord.gg/castcrafter (clickable)
     * ```
     *
     * **Output Example (issue = false):**
     * ```
     * CASTCRAFTER
     * COMMUNITY SERVER
     * <empty>
     * <empty>
     * DU WURDEST VOM SERVER GEWORFEN
     * <empty>
     * <empty>
     * <empty>
     * [Custom message content]
     * <empty>
     * <empty>
     * <empty>
     * <empty>
     * <empty>
     * Bitte versuche es später erneut.
     * ```
     */
    inline fun <B : TextComponent.Builder> renderKickDisconnectMessage(
        builder: B,
        messageRenderer: B.() -> Unit,
        issue: Boolean,
    ) = renderKickDisconnectMessage(builder, messageRenderer) {
        if (issue) append(ISSUE_FOOTER)
        else append(RETRY_LATER_FOOTER)
    }

    /**
     * Renders a structured disconnection message with a specific reason and help suggestion.
     *
     * @param disconnectReason The reason for the disconnection (will be displayed in uppercase).
     * @param suggestHelp Block to render the help message content.
     * @param footerRenderer Block to render the footer content.
     * @return The formatted disconnect message component.
     *
     * **Output Example:**
     * ```
     * CASTCRAFTER
     * COMMUNITY SERVER
     * <empty>
     * <empty>
     * VERBINDUNG VERLOREN
     * <empty>
     * <empty>
     * <empty>
     * [Help message content]
     * <empty>
     * <empty>
     * <empty>
     * [Footer content]
     * ```
     */
    inline fun renderDisconnectMessage(
        disconnectReason: @NoLowercase String,
        suggestHelp: TextComponent.Builder.() -> Unit,
        footerRenderer: TextComponent.Builder.() -> Unit = { },
    ) = renderDisconnectMessage(Component.text(), disconnectReason, suggestHelp, footerRenderer)

    /**
     * Renders a structured disconnection message with a specific reason and help suggestion using a provided builder.
     *
     * @param B The type of builder.
     * @param builder The builder to use for constructing the message.
     * @param disconnectReason The reason for the disconnection (will be displayed in uppercase).
     * @param suggestHelp Block to render the help message content.
     * @param footerRenderer Block to render the footer content.
     * @return The formatted disconnect message component.
     *
     * **Output Example:**
     * ```
     * CASTCRAFTER
     * COMMUNITY SERVER
     * <empty>
     * <empty>
     * VERBINDUNG VERLOREN
     * <empty>
     * <empty>
     * <empty>
     * [Help message content]
     * <empty>
     * <empty>
     * <empty>
     * [Footer content]
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
            appendText(disconnectReason.uppercase(), ERROR)
            appendNewline(3)
            suggestHelp()
            appendNewline(3)
            footerRenderer()
        }

        return builder.build()
    }

    /**
     * Renders a structured disconnection message with an automatic issue or retry footer.
     *
     * @param disconnectReason The reason for the disconnection (will be displayed in uppercase).
     * @param suggestHelp Block to render the help message content.
     * @param issue Whether to include an issue-related footer instead of a simple retry footer.
     * @return The formatted disconnect message component.
     *
     * **Output Example (issue = false):**
     * ```
     * CASTCRAFTER
     * COMMUNITY SERVER
     * <empty>
     * <empty>
     * VERBINDUNG VERLOREN
     * <empty>
     * <empty>
     * <empty>
     * [Help message content]
     * <empty>
     * <empty>
     * <empty>
     * <empty>
     * <empty>
     * Bitte versuche es später erneut.
     * ```
     */
    inline fun renderDisconnectMessage(
        disconnectReason: @NoLowercase String,
        suggestHelp: TextComponent.Builder.() -> Unit,
        issue: Boolean,
    ) = renderDisconnectMessage(Component.text(), disconnectReason, suggestHelp, issue)

    /**
     * Renders a structured disconnection message with an automatic issue or retry footer using a provided builder.
     *
     * @param B The type of builder.
     * @param builder The builder to use for constructing the message.
     * @param disconnectReason The reason for the disconnection (will be displayed in uppercase).
     * @param suggestHelp Block to render the help message content.
     * @param issue Whether to include an issue-related footer instead of a simple retry footer.
     * @return The formatted disconnect message component.
     *
     * **Output Example (issue = false):**
     * ```
     * CASTCRAFTER
     * COMMUNITY SERVER
     * <empty>
     * <empty>
     * VERBINDUNG VERLOREN
     * <empty>
     * <empty>
     * <empty>
     * [Help message content]
     * <empty>
     * <empty>
     * <empty>
     * <empty>
     * <empty>
     * Bitte versuche es später erneut.
     * ```
     */
    inline fun <B : TextComponent.Builder> renderDisconnectMessage(
        builder: B,
        disconnectReason: @NoLowercase String,
        suggestHelp: B.() -> Unit,
        issue: Boolean,
    ) = renderDisconnectMessage(builder, disconnectReason, suggestHelp) {
        if (issue) append(ISSUE_FOOTER)
        else append(RETRY_LATER_FOOTER)
    }

    /**
     * Formats a collection into a comma-separated list component.
     *
     * @param E The element type.
     * @param collection The collection to format.
     * @param formatter Function to convert each element to a component.
     * @return A component representing the formatted collection.
     *
     * **Example:**
     * ```kotlin
     * val fruits = listOf("Apple", "Banana", "Cherry")
     * val formatted = formatCollection(fruits) { text(it) }
     * ```
     *
     * **Output:**
     * ```
     * Apple, Banana, Cherry
     * ```
     */
    inline fun <E> formatCollection(
        collection: Iterable<E>,
        formatter: (E) -> Component,
    ): Component {
        val joinConfig = JoinConfiguration.builder().separator(Component.text(", ", SPACER)).build()

        return Component.join(joinConfig, collection.mapTo(mutableObjectListOf(), formatter))
    }

    /**
     * Formats a collection into a structured list with each element on a new line, prefixed with an em dash.
     *
     * @param E The element type.
     * @param collection The collection to format.
     * @param linePrefix The prefix component for each line.
     * @param formatter Function to convert each element to a component.
     * @return A component representing the formatted collection.
     *
     * **Example:**
     * ```kotlin
     * val fruits = listOf("Apple", "Banana", "Cherry")
     * val formatted = formatCollectionNewLine(fruits, PREFIX) { text(it) }
     * ```
     *
     * **Output:**
     * ```
     * <empty>
     * >> Surf | — Apple
     * >> Surf | — Banana
     * >> Surf | — Cherry
     * ```
     */
    inline fun <E> formatCollectionNewLine(
        collection: Iterable<E>,
        linePrefix: Component = PREFIX,
        formatter: (E) -> Component,
    ): Component {
        val separator = buildText0 {
            appendNewline()
            append(linePrefix)
            appendText("— ", SPACER)
        }
        val joinConfig = JoinConfiguration.builder().separator(separator).build()

        val firstPrefix = if (collection.iterator().hasNext()) separator else Component.empty()

        return firstPrefix.append(
            Component.join(
                joinConfig, collection.mapTo(mutableObjectListOf(), formatter)
            )
        )
    }

    /**
     * Formats a map into a structured list with each entry on a new line showing key-value pairs.
     *
     * @param K The key type.
     * @param V The value type.
     * @param map The map to format.
     * @param keyFormatter Function to convert each key to a component.
     * @param valueFormatter Function to convert each value to a component.
     * @param linePrefix The prefix component for each line.
     * @param keyValueSeparator The separator component between keys and values.
     * @return A component representing the formatted map.
     *
     * **Example:**
     * ```kotlin
     * val userData = mapOf("Name" to "Alice", "Age" to "25")
     * val formatted = formatMap(userData, { text(it) }, { text(it) }, PREFIX, MAP_SEPERATOR)
     * ```
     *
     * **Output:**
     * ```
     * <empty>
     * >> Surf | — Name -> Alice
     * >> Surf | — Age -> 25
     * ```
     */
    inline fun <K, V> formatMap(
        map: Map<K, V>,
        keyFormatter: (K) -> Component,
        valueFormatter: (V) -> Component,
        linePrefix: Component = PREFIX,
        keyValueSeparator: Component = MAP_SEPERATOR,
    ): Component {
        val separator = buildText0 {
            appendNewline()
            append(linePrefix)
            appendText("— ", SPACER)
        }
        val joinConfig = JoinConfiguration.builder().separator(separator).build()
        val firstPrefix = if (map.isNotEmpty()) separator else Component.empty()

        return firstPrefix.append(
            Component.join(
                joinConfig,
                map.mapTo(mutableObjectListOf()) { (key, value) ->
                    buildText0 {
                        append(keyFormatter(key).colorIfAbsent(VARIABLE_KEY))
                        append(keyValueSeparator)
                        append(valueFormatter(value).colorIfAbsent(VARIABLE_VALUE))
                    }
                }
            )
        )
    }

    /**
     * Formats a duration into a human-readable time representation. Supports multiple time units from centuries to seconds.
     *
     * @param time The duration to format.
     * @param showSeconds Whether to include seconds in the output.
     * @param shortForms Whether to use short forms for time units (e.g., "d" instead of "Tage").
     * @param separator The separator component between time units.
     * @param timeColor The color for the time values.
     * @return A component representing the formatted time.
     *
     * **Example (short forms):**
     * ```kotlin
     * val duration = Duration.parse("P123DT6H7M8S") // 123 days, 6 hours, 7 minutes, 8 seconds
     * val formatted = formatTime(duration, showSeconds = true, shortForms = true)
     * ```
     *
     * **Output:**
     * ```
     * 123d : 6h : 7m : 8s
     * ```
     *
     * **Example (long forms):**
     * ```kotlin
     * val duration = Duration.parse("P45DT6H7M")
     * val formatted = formatTime(duration, showSeconds = false, shortForms = false)
     * ```
     *
     * **Output:**
     * ```
     * 45 Tage : 6 Stunden : 7 Minuten
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
            operator fun invoke(time: Long, singularForm: String, pluralForm: String, shortForm: String) = buildText0 {
                append(Component.text(time, timeColor))
                appendText(
                    if (shortForms) shortForm
                    else if (time == 1L) " $singularForm"
                    else " $pluralForm",
                    timeColor
                )
            }
        }

        val formatter = Formatter(shortForms, timeColor)

        val centuries = time.inWholeDays / 365 / 100
        val decades = time.inWholeDays / 365 % 100 / 10
        val years = time.inWholeDays / 365 % 10
        val days = time.inWholeDays % 365
        val hours = time.inWholeHours % 24
        val minutes = time.inWholeMinutes % 60
        val seconds = time.inWholeSeconds % 60

        return buildText0 {
            var hasAddedComponent = false

            fun addComponent(value: Long, singularForm: String, pluralForm: String, shortForm: String) {
                if (value > 0) {
                    if (hasAddedComponent) append(separator)
                    append(formatter(value, singularForm, pluralForm, shortForm))
                    hasAddedComponent = true
                }
            }

            addComponent(centuries, "Jahrhundert", "Jahrhunderte", "Jh")
            addComponent(decades, "Jahrzehnt", "Jahrzehnte", "Jz")
            addComponent(years, "Jahr", "Jahre", "J")
            addComponent(days, "Tag", "Tage", "d")
            addComponent(hours, "Stunde", "Stunden", "h")
            addComponent(minutes, "Minute", "Minuten", "m")
            if (showSeconds) {
                addComponent(seconds, "Sekunde", "Sekunden", "s")
            }
        }
    }
}

/**
 * Formats this collection into a comma-separated list component.
 *
 * @param E The element type.
 * @param formatter Function to convert each element to a component.
 * @return A component representing the formatted collection.
 * @see CommonComponents.formatCollection
 *
 * **Example:**
 * ```kotlin
 * val fruits = listOf("Apple", "Banana", "Cherry")
 * val formatted = fruits.joinToComponent { text(it) }
 * ```
 *
 * **Output:**
 * ```
 * Apple, Banana, Cherry
 * ```
 */
inline fun <E> Iterable<E>.joinToComponent(formatter: (E) -> Component) =
    CommonComponents.formatCollection(this, formatter)

/**
 * Formats this collection into a structured list with each element on a new line.
 *
 * @param E The element type.
 * @param linePrefix The prefix component for each line.
 * @param formatter Function to convert each element to a component.
 * @return A component representing the formatted collection.
 * @see CommonComponents.formatCollectionNewLine
 *
 * **Example:**
 * ```kotlin
 * val tasks = listOf("Buy groceries", "Clean room", "Study")
 * val formatted = tasks.joinToComponentNewLine(PREFIX) { text(it) }
 * ```
 *
 * **Output:**
 * ```
 * <empty>
 * >> Surf | — Buy groceries
 * >> Surf | — Clean room
 * >> Surf | — Study
 * ```
 */
inline fun <E> Iterable<E>.joinToComponentNewLine(
    linePrefix: Component = PREFIX,
    formatter: (E) -> Component,
) = CommonComponents.formatCollectionNewLine(this, linePrefix, formatter)

/**
 * Formats this map into a structured list with each entry on a new line showing key-value pairs.
 *
 * @param K The key type.
 * @param V The value type.
 * @param keyFormatter Function to convert each key to a component.
 * @param valueFormatter Function to convert each value to a component.
 * @param linePrefix The prefix component for each line.
 * @param keyValueSeparator The separator component between keys and values.
 * @return A component representing the formatted map.
 * @see CommonComponents.formatMap
 *
 * **Example:**
 * ```kotlin
 * val settings = mapOf("Volume" to "80%", "Quality" to "High")
 * val formatted = settings.joinToComponent(
 *     keyFormatter = { text(it) },
 *     valueFormatter = { text(it) }
 * )
 * ```
 *
 * **Output:**
 * ```
 * <empty>
 * >> Surf | — Volume -> 80%
 * >> Surf | — Quality -> High
 * ```
 */
inline fun <K, V> Map<K, V>.joinToComponent(
    keyFormatter: (K) -> Component,
    valueFormatter: (V) -> Component,
    linePrefix: Component = PREFIX,
    keyValueSeparator: Component = MAP_SEPERATOR,
) = CommonComponents.formatMap(this, keyFormatter, valueFormatter, linePrefix, keyValueSeparator)
