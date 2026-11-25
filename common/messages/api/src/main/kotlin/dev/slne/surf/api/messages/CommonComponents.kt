package dev.slne.surf.api.messages

import dev.slne.surf.api.messages.Colors.Companion.ERROR
import dev.slne.surf.api.messages.Colors.Companion.PREFIX
import dev.slne.surf.api.messages.Colors.Companion.PRIMARY
import dev.slne.surf.api.messages.Colors.Companion.SPACER
import dev.slne.surf.api.messages.Colors.Companion.VARIABLE_KEY
import dev.slne.surf.api.messages.Colors.Companion.VARIABLE_VALUE
import dev.slne.surf.api.messages.CommonComponents.MAP_SEPARATOR
import dev.slne.surf.api.messages.adventure.appendNewline
import dev.slne.surf.api.messages.adventure.appendText
import dev.slne.surf.api.messages.adventure.clickOpensUrl
import dev.slne.surf.api.messages.adventure.text
import dev.slne.surf.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import kotlin.time.Duration

private inline fun buildText0(block: TextComponent.Builder.() -> Unit): TextComponent {
    return Component.text().apply(block).asComponent() as TextComponent
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
     * A separator component (` : `) used to visually format time-related messages.
     */
    @JvmField
    val TIME_SEPARATOR = text(" : ", SPACER)

    /**
     * An em dash component (`—`) used as a visual separator.
     */
    @JvmField
    val EM_DASH = text("—", SPACER)

    /**
     * An en dash component (`–`) used as a visual separator.
     */
    @JvmField
    val EN_DASH = text("–", SPACER)

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
     * A footer message indicating the user should try again later and contact support if the issue
     * persists.
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
     * Renders a kick message from two builder blocks.
     *
     * **Output:**
     * ```
     * CASTCRAFTER
     * COMMUNITY SERVER
     * <empty>
     * <empty>
     * DU WURDEST VOM SERVER GEWORFEN
     * <empty>
     * <empty>
     * <empty>
     * [message]
     * <empty>
     * <empty>
     * <empty>
     * [footer]
     * ```
     *
     * @param messageRenderer builds the message below the kick headline
     * @param footerRenderer builds the footer below the message
     * @return the kick message
     */
    inline fun renderKickDisconnectMessage(
        messageRenderer: SurfComponentBuilder.() -> Unit,
        footerRenderer: SurfComponentBuilder.() -> Unit = { },
    ): TextComponent {
        return renderKickDisconnectMessage(
            message = SurfComponentBuilder(messageRenderer),
            footer = SurfComponentBuilder(footerRenderer),
        )
    }

    /**
     * Renders a kick message from a builder block, followed by [ISSUE_FOOTER] when [issue] is
     * `true` and by [RETRY_LATER_FOOTER] otherwise.
     *
     * @param messageRenderer builds the message below the kick headline
     * @param issue whether to use the issue footer
     * @return the kick message
     */
    inline fun renderKickDisconnectMessage(
        messageRenderer: SurfComponentBuilder.() -> Unit,
        issue: Boolean,
    ): TextComponent {
        return renderKickDisconnectMessage(SurfComponentBuilder(messageRenderer), issue)
    }

    /**
     * Renders a kick message with the layout of [SurfComponentBuilder.appendKickDisconnectMessage].
     *
     * @param message the message below the kick headline
     * @param footer the footer below the message
     * @return the kick message
     */
    fun renderKickDisconnectMessage(
        message: Component,
        footer: Component = Component.empty(),
    ): TextComponent {
        return SurfComponentBuilder.builder()
            .appendKickDisconnectMessage(message, footer)
            .build()
    }

    /**
     * Renders a kick message followed by [ISSUE_FOOTER] when [issue] is `true` and by
     * [RETRY_LATER_FOOTER] otherwise.
     *
     * @param message the message below the kick headline
     * @param issue whether to use the issue footer
     * @return the kick message
     */
    fun renderKickDisconnectMessage(message: Component, issue: Boolean): TextComponent {
        return renderKickDisconnectMessage(message, disconnectFooter(issue))
    }

    /**
     * Renders a disconnect message from builder blocks.
     *
     * **Output:**
     * ```
     * CASTCRAFTER
     * COMMUNITY SERVER
     * <empty>
     * <empty>
     * [DISCONNECT REASON]
     * <empty>
     * <empty>
     * <empty>
     * [help]
     * <empty>
     * <empty>
     * <empty>
     * [footer]
     * ```
     *
     * @param disconnectReason the headline, displayed in uppercase
     * @param suggestHelp builds the help text below the headline
     * @param footerRenderer builds the footer below the help text
     * @return the disconnect message
     */
    inline fun renderDisconnectMessage(
        disconnectReason: @NoLowercase String,
        suggestHelp: SurfComponentBuilder.() -> Unit,
        footerRenderer: SurfComponentBuilder.() -> Unit = { },
    ): TextComponent {
        return renderDisconnectMessage(
            disconnectReason = disconnectReason,
            help = SurfComponentBuilder(suggestHelp),
            footer = SurfComponentBuilder(footerRenderer),
        )
    }

    /**
     * Renders a disconnect message from a builder block, followed by [ISSUE_FOOTER] when [issue]
     * is `true` and by [RETRY_LATER_FOOTER] otherwise.
     *
     * @param disconnectReason the headline, displayed in uppercase
     * @param suggestHelp builds the help text below the headline
     * @param issue whether to use the issue footer
     * @return the disconnect message
     */
    inline fun renderDisconnectMessage(
        disconnectReason: @NoLowercase String,
        suggestHelp: SurfComponentBuilder.() -> Unit,
        issue: Boolean,
    ): TextComponent {
        return renderDisconnectMessage(
            disconnectReason = disconnectReason,
            help = SurfComponentBuilder(suggestHelp),
            issue = issue,
        )
    }

    /**
     * Renders a disconnect message with the layout of
     * [SurfComponentBuilder.appendDisconnectMessage].
     *
     * @param disconnectReason the headline, displayed in uppercase
     * @param help the help text below the headline
     * @param footer the footer below the help text
     * @return the disconnect message
     */
    fun renderDisconnectMessage(
        disconnectReason: @NoLowercase String,
        help: Component,
        footer: Component = Component.empty(),
    ): TextComponent {
        return SurfComponentBuilder.builder()
            .appendDisconnectMessage(disconnectReason, help, footer)
            .build()
    }

    /**
     * Renders a disconnect message followed by [ISSUE_FOOTER] when [issue] is `true` and by
     * [RETRY_LATER_FOOTER] otherwise.
     *
     * @param disconnectReason the headline, displayed in uppercase
     * @param help the help text below the headline
     * @param issue whether to use the issue footer
     * @return the disconnect message
     */
    fun renderDisconnectMessage(
        disconnectReason: @NoLowercase String,
        help: Component,
        issue: Boolean,
    ): TextComponent {
        return renderDisconnectMessage(disconnectReason, help, disconnectFooter(issue))
    }

    private fun disconnectFooter(issue: Boolean): Component {
        return if (issue) ISSUE_FOOTER else RETRY_LATER_FOOTER
    }

    /**
     * Formats [collection] into a comma-separated list.
     *
     * **Output:**
     * ```
     * Apple, Banana, Cherry
     * ```
     *
     * @param collection the elements to format
     * @param formatter converts an element to a component
     * @return the formatted list
     */
    inline fun <E> formatCollection(
        collection: Iterable<E>,
        formatter: (E) -> Component,
    ): Component {
        return formatCollection(collection.map(formatter))
    }

    /**
     * Joins [components] into a comma-separated list.
     *
     * @param components the components to join
     * @return the joined list
     */
    fun formatCollection(components: List<Component>): Component {
        val joinConfig = JoinConfiguration.builder()
            .separator(Component.text(", ", SPACER))
            .build()

        return Component.join(joinConfig, components)
    }

    /**
     * Formats [collection] with each element on a new line behind [linePrefix] and an em dash.
     *
     * The result starts with a newline when [collection] is not empty.
     *
     * **Output:**
     * ```
     * <empty>
     * >> Surf | — Apple
     * >> Surf | — Banana
     * ```
     *
     * @param collection the elements to format
     * @param linePrefix the component starting each line
     * @param formatter converts an element to a component
     * @return the formatted list
     */
    inline fun <E> formatCollectionNewLine(
        collection: Iterable<E>,
        linePrefix: Component = PREFIX,
        formatter: (E) -> Component,
    ): Component {
        return formatCollectionNewLine(collection.map(formatter), linePrefix)
    }

    /**
     * Joins [components] with each one on a new line behind [linePrefix] and an em dash.
     *
     * The result starts with a newline when [components] is not empty.
     *
     * @param components the components to join
     * @param linePrefix the component starting each line
     * @return the joined list
     */
    fun formatCollectionNewLine(
        components: List<Component>,
        linePrefix: Component = PREFIX,
    ): Component {
        if (components.isEmpty()) return Component.empty()

        val separator = lineSeparator(linePrefix)
        val joinConfig = JoinConfiguration.builder()
            .separator(separator)
            .build()

        return separator.append(Component.join(joinConfig, components))
    }

    /**
     * Formats [map] with each entry on a new line behind [linePrefix] and an em dash.
     *
     * The result starts with a newline when [map] is not empty.
     *
     * **Output:**
     * ```
     * <empty>
     * >> Surf | — Name -> Alice
     * >> Surf | — Age -> 25
     * ```
     *
     * @param map the entries to format
     * @param keyFormatter converts a key to a component
     * @param valueFormatter converts a value to a component
     * @param linePrefix the component starting each line
     * @param keyValueSeparator the component between key and value
     * @return the formatted map
     */
    inline fun <K, V> formatMap(
        map: Map<K, V>,
        keyFormatter: (K) -> Component,
        valueFormatter: (V) -> Component,
        linePrefix: Component = PREFIX,
        keyValueSeparator: Component = MAP_SEPARATOR,
    ): Component {
        val entries = map.map { (key, value) -> keyFormatter(key) to valueFormatter(value) }

        return formatMap(entries, linePrefix, keyValueSeparator)
    }

    /**
     * Joins [entries] with each key-value pair on a new line behind [linePrefix] and an em dash.
     *
     * Keys without a color get [VARIABLE_KEY], values without a color [VARIABLE_VALUE]. The result
     * starts with a newline when [entries] is not empty.
     *
     * @param entries the key and value components, in display order
     * @param linePrefix the component starting each line
     * @param keyValueSeparator the component between key and value
     * @return the joined entries
     */
    fun formatMap(
        entries: List<Pair<Component, Component>>,
        linePrefix: Component = PREFIX,
        keyValueSeparator: Component = MAP_SEPARATOR,
    ): Component {
        val lines = entries.map { (key, value) ->
            buildText0 {
                append(key.colorIfAbsent(VARIABLE_KEY))
                append(keyValueSeparator)
                append(value.colorIfAbsent(VARIABLE_VALUE))
            }
        }

        return formatCollectionNewLine(lines, linePrefix)
    }

    private fun lineSeparator(linePrefix: Component): TextComponent {
        return buildText0 {
            appendNewline()
            append(linePrefix)
            appendText("— ", SPACER)
        }
    }

    /**
     * Formats a duration into a human-readable time representation. Supports multiple time units
     * from centuries to seconds.
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
            operator fun invoke(
                time: Long,
                singularForm: String,
                pluralForm: String,
                shortForm: String,
            ): TextComponent {
                return buildText0 {
                    append(Component.text(time, timeColor))
                    appendText(
                        if (shortForms) shortForm
                        else if (time == 1L) " $singularForm"
                        else " $pluralForm",
                        timeColor,
                    )
                }
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

            fun addComponent(
                value: Long,
                singularForm: String,
                pluralForm: String,
                shortForm: String,
            ) {
                if (value > 0) {
                    if (hasAddedComponent) {
                        append(separator)
                    }

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
inline fun <E> Iterable<E>.joinToComponent(formatter: (E) -> Component): Component {
    return CommonComponents.formatCollection(this, formatter)
}

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
): Component = CommonComponents.formatCollectionNewLine(this, linePrefix, formatter)

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
    keyValueSeparator: Component = MAP_SEPARATOR,
): Component {
    return CommonComponents.formatMap(
        this,
        keyFormatter,
        valueFormatter,
        linePrefix,
        keyValueSeparator,
    )
}
