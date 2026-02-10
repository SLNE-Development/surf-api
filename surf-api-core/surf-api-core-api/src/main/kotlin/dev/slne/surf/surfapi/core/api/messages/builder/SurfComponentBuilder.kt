package dev.slne.surf.surfapi.core.api.messages.builder

import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.NOTE
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.PREFIX
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.SPACER
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.VARIABLE_VALUE
import dev.slne.surf.surfapi.core.api.messages.CommonComponents
import dev.slne.surf.surfapi.core.api.messages.CommonComponents.DISCONNECT_HEADER
import dev.slne.surf.surfapi.core.api.messages.CommonComponents.DISCORD_LINK
import dev.slne.surf.surfapi.core.api.messages.CommonComponents.MAP_SEPERATOR
import dev.slne.surf.surfapi.core.api.messages.CommonComponents.TIME_SEPARATOR
import dev.slne.surf.surfapi.core.api.messages.NoLowercase
import dev.slne.surf.surfapi.core.api.messages.joinToComponent
import dev.slne.surf.surfapi.core.api.messages.joinToComponentNewLine
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.*
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEventSource
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.util.ARGBLike
import org.jetbrains.annotations.ApiStatus
import java.util.function.Consumer
import java.util.function.Function
import kotlin.time.Duration

@ApiStatus.NonExtendable
interface SurfComponentBuilder : TextComponent.Builder, ComponentBuilderColors {
    companion object {
        @JvmStatic
        fun builder(): SurfComponentBuilder = SurfComponentBuilderImpl(Component.text())

        operator fun invoke(): SurfComponentBuilder = builder()
        inline operator fun invoke(block: SurfComponentBuilder.() -> Unit) =
            builder().apply(block).build()
    }

    @Deprecated("Use TYPE specific functions")
    fun appendPrefix() = append(PREFIX)

    @Deprecated("Use TYPE specific functions")
    fun appendNewPrefixedLine() = appendNewline().appendPrefix()

    fun append(block: SurfComponentBuilder.() -> Unit): SurfComponentBuilder =
        append(SurfComponentBuilder(block))

    suspend fun appendAsync(block: suspend SurfComponentBuilder.() -> Unit): SurfComponentBuilder =
        append(SurfComponentBuilder { block() })

    fun appendNewline(block: SurfComponentBuilder.() -> Unit) =
        appendNewline().append(block)

    suspend fun appendNewlineAsync(block: suspend SurfComponentBuilder.() -> Unit) =
        appendNewline().appendAsync(block)

    @Deprecated("Use TYPE specific functions")
    fun appendNewPrefixedLine(block: SurfComponentBuilder.() -> Unit) =
        appendNewPrefixedLine().append(block)

    @Deprecated("Use TYPE specific functions")
    suspend fun appendNewPrefixedLineAsync(block: suspend SurfComponentBuilder.() -> Unit) =
        appendNewPrefixedLine().appendAsync(block)

    fun text(text: String, color: TextColor? = null, vararg decoration: TextDecoration) =
        append(Component.text(text, color, *decoration))

    fun text(boolean: Boolean, color: TextColor? = null, vararg decoration: TextDecoration) =
        append(Component.text(boolean, color, *decoration))

    fun text(char: Char, color: TextColor? = null, vararg decoration: TextDecoration) =
        append(Component.text(char, color, *decoration))

    fun text(number: Number, color: TextColor? = null, vararg decoration: TextDecoration) = append(
        when (number) {
            is Double -> Component.text(number, color, *decoration)
            is Float -> Component.text(number, color, *decoration)
            is Int -> Component.text(number, color, *decoration)
            is Long -> Component.text(number, color, *decoration)
            else -> Component.text(number.toString(), color, *decoration)
        }
    )

    fun note(any: Any, vararg decoration: TextDecoration) = text(any.toString(), NOTE, *decoration)

    fun ellipsis(color: TextColor? = SPACER) = append(CommonComponents.ELLIPSIS.color(color))

    fun appendDiscordLink() = append(DISCORD_LINK)
    fun appendDisconnectHeader() = append(DISCONNECT_HEADER)
    fun appendDisconnectFooterTryAgainLater(issue: Boolean) = append(
        if (issue) CommonComponents.ISSUE_FOOTER
        else CommonComponents.RETRY_LATER_FOOTER
    )

    fun appendKickDisconnectMessage(
        messageRenderer: SurfComponentBuilder.() -> Unit,
        footerRenderer: SurfComponentBuilder.() -> Unit = { },
    ) = append(
        CommonComponents.renderKickDisconnectMessage(
            builder(),
            messageRenderer,
            footerRenderer
        )
    )

    fun appendDisconnectMessage(
        disconnectReason: @NoLowercase String,
        suggestHelp: SurfComponentBuilder.() -> Unit,
        footerRenderer: SurfComponentBuilder.() -> Unit = { },
    ) = append(
        CommonComponents.renderDisconnectMessage(
            builder(),
            disconnectReason,
            suggestHelp,
            footerRenderer
        )
    )

    fun <E> appendCollection(collection: Iterable<E>, formatter: (E) -> Component) =
        append(collection.joinToComponent(formatter))

    fun <E> appendCollectionNewLine(
        collection: Iterable<E>,
        linePrefix: Component = PREFIX,
        formatter: (E) -> Component,
    ) = append(collection.joinToComponentNewLine(linePrefix, formatter))

    fun <K, V> appendMap(
        map: Map<K, V>,
        keyFormatter: (K) -> Component,
        valueFormatter: (V) -> Component,
        linePrefix: Component = PREFIX,
        keyValueSeparator: Component = MAP_SEPERATOR,
    ) = append(map.joinToComponent(keyFormatter, valueFormatter, linePrefix, keyValueSeparator))

    fun appendTime(
        time: Duration,
        showSeconds: Boolean = true,
        shortForms: Boolean = false,
        separator: Component = TIME_SEPARATOR,
        timeColor: TextColor = VARIABLE_VALUE,
    ) = append(CommonComponents.formatTime(time, showSeconds, shortForms, separator, timeColor))

    override fun content(content: String): SurfComponentBuilder
    override fun append(builder: ComponentBuilder<*, *>): SurfComponentBuilder
    override fun append(component: Component): SurfComponentBuilder
    override fun append(component: ComponentLike): SurfComponentBuilder
    override fun append(components: Iterable<ComponentLike?>): SurfComponentBuilder
    override fun append(vararg components: Component): SurfComponentBuilder
    override fun append(vararg components: ComponentLike): SurfComponentBuilder
    override fun appendNewline(): SurfComponentBuilder
    override fun appendSpace(): SurfComponentBuilder
    override fun applicableApply(applicable: ComponentBuilderApplicable): SurfComponentBuilder
    override fun apply(consumer: Consumer<in ComponentBuilder<*, *>>): SurfComponentBuilder
    override fun applyDeep(action: Consumer<in ComponentBuilder<*, *>>): SurfComponentBuilder
    override fun clickEvent(event: ClickEvent?): SurfComponentBuilder
    override fun color(color: TextColor?): SurfComponentBuilder
    override fun colorIfAbsent(color: TextColor?): SurfComponentBuilder
    override fun decorate(decoration: TextDecoration): SurfComponentBuilder
    override fun decorate(vararg decorations: TextDecoration): SurfComponentBuilder
    override fun decoration(decoration: TextDecoration, flag: Boolean): SurfComponentBuilder
    override fun decoration(
        decoration: TextDecoration,
        state: TextDecoration.State,
    ): SurfComponentBuilder

    override fun decorationIfAbsent(
        decoration: TextDecoration,
        state: TextDecoration.State,
    ): SurfComponentBuilder

    override fun decorations(decorations: Map<TextDecoration?, TextDecoration.State?>): SurfComponentBuilder
    override fun decorations(
        decorations: Set<TextDecoration?>,
        flag: Boolean,
    ): SurfComponentBuilder

    override fun font(font: Key?): SurfComponentBuilder
    override fun hoverEvent(source: HoverEventSource<*>?): SurfComponentBuilder
    override fun insertion(insertion: String?): SurfComponentBuilder
    override fun mapChildren(function: Function<BuildableComponent<*, *>?, out BuildableComponent<*, *>?>): SurfComponentBuilder
    override fun mapChildrenDeep(function: Function<BuildableComponent<*, *>?, out BuildableComponent<*, *>?>): SurfComponentBuilder
    override fun mergeStyle(that: Component): SurfComponentBuilder
    override fun mergeStyle(that: Component, merges: Set<Style.Merge?>): SurfComponentBuilder
    override fun mergeStyle(that: Component, vararg merges: Style.Merge): SurfComponentBuilder
    override fun resetStyle(): SurfComponentBuilder
    override fun style(consumer: Consumer<Style.Builder?>): SurfComponentBuilder
    override fun style(style: Style): SurfComponentBuilder
    override fun shadowColor(argb: ARGBLike?): SurfComponentBuilder
    override fun shadowColorIfAbsent(argb: ARGBLike?): SurfComponentBuilder
}
