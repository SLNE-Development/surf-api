package dev.slne.surf.api.core.messages.builder

import dev.slne.surf.api.core.messages.*
import dev.slne.surf.api.core.messages.Colors.Companion.NOTE
import dev.slne.surf.api.core.messages.Colors.Companion.PREFIX
import dev.slne.surf.api.core.messages.Colors.Companion.SPACER
import dev.slne.surf.api.core.messages.Colors.Companion.VARIABLE_VALUE
import dev.slne.surf.api.core.messages.CommonComponents.DISCONNECT_HEADER
import dev.slne.surf.api.core.messages.CommonComponents.DISCORD_LINK
import dev.slne.surf.api.core.messages.CommonComponents.TIME_SEPARATOR
import dev.slne.surf.api.core.messages.adventure.ClickCallbackWithOptionsBuilder
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.*
import net.kyori.adventure.text.event.ClickCallback
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
interface SurfComponentBuilder : ComponentBuilderColors, ComponentLike {
    companion object {
        @JvmStatic
        fun builder(): SurfComponentBuilder = SurfComponentBuilderImpl(Component.text())

        operator fun invoke(): SurfComponentBuilder = builder()
        inline operator fun invoke(block: SurfComponentBuilder.() -> Unit) =
            builder().apply(block).build()
    }

    fun append(block: SurfComponentBuilder.() -> Unit): SurfComponentBuilder =
        append(SurfComponentBuilder(block))

    suspend fun appendAsync(block: suspend SurfComponentBuilder.() -> Unit): SurfComponentBuilder =
        append(SurfComponentBuilder { block() })

    fun appendNewline(block: SurfComponentBuilder.() -> Unit) =
        appendNewline().append(block)

    suspend fun appendNewlineAsync(block: suspend SurfComponentBuilder.() -> Unit) =
        appendNewline().appendAsync(block)

    fun appendNewline(amount: Int): SurfComponentBuilder = apply { repeat(amount) { appendNewline() } }

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

    fun appendText(text: String, color: TextColor? = null) = this.text(text, color)

    fun appendText(
        text: String,
        color: TextColor? = null,
        block: TextComponent.Builder.() -> Unit
    ): SurfComponentBuilder {
        val builder = Component.text().content(text)
        if (color != null) builder.color(color)
        builder.apply(block)
        return append(builder.build())
    }

    fun clickOpensUrl(url: String) = clickEvent(ClickEvent.openUrl(url))
    fun clickRunsCommand(command: String) = clickEvent(ClickEvent.runCommand(command))
    fun clickSuggestsCommand(command: String) = clickEvent(ClickEvent.suggestCommand(command))
    fun clickCopiesToClipboard(value: String) = clickEvent(ClickEvent.copyToClipboard(value))

    fun note(any: Any, vararg decoration: TextDecoration) = text(any.toString(), NOTE, *decoration)

    fun ellipsis(color: TextColor? = SPACER) = append(CommonComponents.ELLIPSIS.color(color))
    fun translatable(
        key: String,
        color: TextColor? = Colors.WHITE,
        vararg decoration: TextDecoration
    ) = append(Component.translatable(key, color, *decoration))

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
        keyValueSeparator: Component = CommonComponents.MAP_SEPARATOR,
    ) = append(map.joinToComponent(keyFormatter, valueFormatter, linePrefix, keyValueSeparator))

    fun appendTime(
        time: Duration,
        showSeconds: Boolean = true,
        shortForms: Boolean = false,
        separator: Component = TIME_SEPARATOR,
        timeColor: TextColor = VARIABLE_VALUE,
    ) = append(CommonComponents.formatTime(time, showSeconds, shortForms, separator, timeColor))

    fun content(): String
    fun content(content: String): SurfComponentBuilder
    fun children(): List<Component>

    fun build(): TextComponent

    fun append(builder: ComponentBuilder<*, *>): SurfComponentBuilder
    fun append(component: Component): SurfComponentBuilder
    fun append(component: ComponentLike): SurfComponentBuilder
    fun append(components: Iterable<ComponentLike>): SurfComponentBuilder
    fun append(vararg components: Component): SurfComponentBuilder
    fun append(vararg components: ComponentLike): SurfComponentBuilder
    fun appendNewline(): SurfComponentBuilder
    fun appendSpace(): SurfComponentBuilder
    fun applicableApply(applicable: ComponentBuilderApplicable): SurfComponentBuilder
    fun apply(consumer: Consumer<in ComponentBuilder<*, *>>): SurfComponentBuilder
    fun applyDeep(action: Consumer<in ComponentBuilder<*, *>>): SurfComponentBuilder

    fun clickEvent(event: ClickEvent<*>?): SurfComponentBuilder

    fun clickCallback(callback: ClickCallback<Audience>) = clickEvent(ClickEvent.callback(callback))
    fun clickCallbackWithOptions(
        builder: ClickCallbackWithOptionsBuilder<Audience>.() -> Unit,
    ) = clickEvent(ClickCallbackWithOptionsBuilder(Audience::class.java).apply(builder).build())

    fun color(color: TextColor?): SurfComponentBuilder
    fun colorIfAbsent(color: TextColor?): SurfComponentBuilder
    fun decorate(decoration: TextDecoration): SurfComponentBuilder
    fun decorate(vararg decorations: TextDecoration): SurfComponentBuilder
    fun decoration(decoration: TextDecoration, flag: Boolean): SurfComponentBuilder
    fun decoration(
        decoration: TextDecoration,
        state: TextDecoration.State,
    ): SurfComponentBuilder

    fun decorationIfAbsent(
        decoration: TextDecoration,
        state: TextDecoration.State,
    ): SurfComponentBuilder

    fun decorations(decorations: Map<TextDecoration, TextDecoration.State>): SurfComponentBuilder
    fun decorations(
        decorations: Set<TextDecoration>,
        flag: Boolean,
    ): SurfComponentBuilder

    fun font(font: Key?): SurfComponentBuilder
    fun hoverEvent(source: HoverEventSource<*>?): SurfComponentBuilder
    fun insertion(insertion: String?): SurfComponentBuilder

    fun mapChildren(function: Function<Component, out Component>): SurfComponentBuilder

    fun mapChildrenDeep(function: Function<Component, out Component>): SurfComponentBuilder
    fun mergeStyle(that: Component): SurfComponentBuilder
    fun mergeStyle(that: Component, merges: Set<Style.Merge>): SurfComponentBuilder
    fun mergeStyle(that: Component, vararg merges: Style.Merge): SurfComponentBuilder
    fun resetStyle(): SurfComponentBuilder
    fun style(consumer: Consumer<Style.Builder>): SurfComponentBuilder
    fun style(style: Style): SurfComponentBuilder
    fun shadowColor(argb: ARGBLike?): SurfComponentBuilder
    fun shadowColorIfAbsent(argb: ARGBLike?): SurfComponentBuilder
}

inline fun <reified T : Audience> SurfComponentBuilder.clickCallbackTypedWithOptions(
    builder: ClickCallbackWithOptionsBuilder<T>.() -> Unit
) = clickEvent(ClickCallbackWithOptionsBuilder(T::class.java).apply(builder).build())

inline fun <reified T : Audience> SurfComponentBuilder.clickCallbackTyped(
    callback: ClickCallback<T>,
) = clickEvent(ClickEvent.callback(ClickCallback.widen(callback, T::class.java)))
