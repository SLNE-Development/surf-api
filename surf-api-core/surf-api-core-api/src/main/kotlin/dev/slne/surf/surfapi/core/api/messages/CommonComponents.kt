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
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import kotlin.time.Duration

private fun buildText0(block: TextComponent.Builder.() -> Unit): TextComponent {
    return Component.text().apply(block).build()
}

object CommonComponents {

    @JvmField
    val ELLIPSIS = Component.text("...")

    @JvmField
    val MAP_KEY_VALUE_SEPARATOR = Component.text(" -> ", SPACER)

    @JvmField
    val TIME_SEPARATOR = Component.text(" : ", SPACER)

    @JvmField
    val DISCORD_LINK = buildText0 {
        appendText("discord.gg/castcrafter", PRIMARY) {
            clickOpensUrl("https://discord.gg/castcrafter")
        }
    }

    @JvmField
    val DISCONNECT_HEADER = buildText0 {
        appendText("CASTCRAFTER", PRIMARY)
        appendNewline()
        appendText("COMMUNITY SERVER", PRIMARY)
        appendNewline(2)
    }

    @JvmField
    val DISCONNECT_FOOTER_TRY_AGAIN_LATER = buildText0 {
        appendNewline(2)
        appendText("Bitte versuche es später erneut.", ERROR)
    }

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

    fun renderKickDisconnectMessage(
        messageRenderer: TextComponent.Builder.() -> Unit,
        footerRenderer: TextComponent.Builder.() -> Unit = { },
    ) = renderKickDisconnectMessage(Component.text(), messageRenderer, footerRenderer)

    fun <B : TextComponent.Builder> renderKickDisconnectMessage(
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

    fun renderKickDisconnectMessage(
        messageRenderer: TextComponent.Builder.() -> Unit,
        issue: Boolean,
    ) = renderKickDisconnectMessage(Component.text(), messageRenderer, issue)

    fun <B : TextComponent.Builder> renderKickDisconnectMessage(
        builder: B,
        messageRenderer: B.() -> Unit,
        issue: Boolean,
    ) = renderKickDisconnectMessage(builder, messageRenderer) {
        if (issue) append(DISCONNECT_FOOTER_TRY_AGAIN_LATER_ISSUE)
        else append(DISCONNECT_FOOTER_TRY_AGAIN_LATER)
    }

    fun renderDisconnectMessage(
        disconnectReason: @NoLowercase String,
        suggestHelp: TextComponent.Builder.() -> Unit,
        footerRenderer: TextComponent.Builder.() -> Unit = { },
    ) = renderDisconnectMessage(Component.text(), disconnectReason, suggestHelp, footerRenderer)

    fun <B : TextComponent.Builder> renderDisconnectMessage(
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

    fun renderDisconnectMessage(
        disconnectReason: @NoLowercase String,
        suggestHelp: TextComponent.Builder.() -> Unit,
        issue: Boolean,
    ) = renderDisconnectMessage(Component.text(), disconnectReason, suggestHelp, issue)

    fun <B : TextComponent.Builder> renderDisconnectMessage(
        builder: B,
        disconnectReason: @NoLowercase String,
        suggestHelp: B.() -> Unit,
        issue: Boolean,
    ) = renderDisconnectMessage(builder, disconnectReason, suggestHelp) {
        if (issue) append(DISCONNECT_FOOTER_TRY_AGAIN_LATER_ISSUE)
        else append(DISCONNECT_FOOTER_TRY_AGAIN_LATER)
    }

    fun <E> formatCollection(collection: Iterable<E>, formatter: (E) -> Component): Component {
        val joinConfig = JoinConfiguration.builder()
            .separator(Component.text(", ", SPACER))
            .build()

        return Component.join(joinConfig, collection.mapTo(mutableObjectListOf(), formatter))
    }

    fun <E> formatCollectionNewLine(
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

    fun <K, V> formatMap(
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

fun <E> Iterable<E>.joinToComponent(formatter: (E) -> Component) =
    CommonComponents.formatCollection(this, formatter)

fun <E> Iterable<E>.joinToComponentNewLine(
    linePrefix: Component = PREFIX,
    formatter: (E) -> Component,
) = CommonComponents.formatCollectionNewLine(this, linePrefix, formatter)

fun <K, V> Map<K, V>.joinToComponent(
    keyFormatter: (K) -> Component,
    valueFormatter: (V) -> Component,
    linePrefix: Component = PREFIX,
    keyValueSeparator: Component = MAP_KEY_VALUE_SEPARATOR,
) = CommonComponents.formatMap(this, keyFormatter, valueFormatter, linePrefix, keyValueSeparator)