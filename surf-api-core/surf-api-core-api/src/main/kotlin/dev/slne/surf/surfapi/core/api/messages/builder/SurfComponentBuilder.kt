package dev.slne.surf.surfapi.core.api.messages.builder

import dev.slne.surf.surfapi.core.api.messages.*
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.DARK_SPACER
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.ERROR
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.INFO
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.PREFIX
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.PRIMARY
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.SECONDARY
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.SPACER
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.SUCCESS
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.VARIABLE_KEY
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.VARIABLE_VALUE
import dev.slne.surf.surfapi.core.api.messages.Colors.Companion.WARNING
import dev.slne.surf.surfapi.core.api.messages.CommonComponents.DISCONNECT_HEADER
import dev.slne.surf.surfapi.core.api.messages.CommonComponents.DISCORD_LINK
import dev.slne.surf.surfapi.core.api.messages.CommonComponents.MAP_SEPERATOR
import dev.slne.surf.surfapi.core.api.messages.CommonComponents.TIME_SEPARATOR
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
interface SurfComponentBuilder : TextComponent.Builder {
    companion object {
        @JvmStatic
        fun builder(): SurfComponentBuilder = SurfComponentBuilderImpl(Component.text())

        operator fun invoke(): SurfComponentBuilder = builder()
        inline operator fun invoke(block: SurfComponentBuilder.() -> Unit) =
            builder().apply(block).build()
    }

    fun appendPrefix() = append(PREFIX)
    fun appendSurfPrefix() = append(Colors.SURF_PREFIX)
    fun appendErrorPrefix() = append(Colors.ERROR_PREFIX)
    fun appendWarningPrefix() = append(Colors.WARNING_PREFIX)
    fun appendNewPrefixedLine() = appendNewline().appendPrefix()

    fun append(block: SurfComponentBuilder.() -> Unit): SurfComponentBuilder =
        append(SurfComponentBuilder(block))

    suspend fun appendAsync(block: suspend SurfComponentBuilder.() -> Unit): SurfComponentBuilder =
        append(SurfComponentBuilder { block() })

    fun appendNewline(block: SurfComponentBuilder.() -> Unit) =
        appendNewline().append(block)

    suspend fun appendNewlineAsync(block: suspend SurfComponentBuilder.() -> Unit) =
        appendNewline().appendAsync(block)

    fun appendNewPrefixedLine(block: SurfComponentBuilder.() -> Unit) =
        appendNewPrefixedLine().append(block)

    suspend fun appendNewPrefixedLineAsync(block: suspend SurfComponentBuilder.() -> Unit) =
        appendNewPrefixedLine().appendAsync(block)

    fun text(text: String, color: TextColor? = null, vararg decoration: TextDecoration) =
        append(Component.text(text, color, *decoration))

    fun text(boolean: Boolean, color: TextColor? = null, vararg decoration: TextDecoration) =
        append(Component.text(boolean, color, *decoration))

    fun text(char: Char, color: TextColor? = null, vararg decoration: TextDecoration) =
        append(Component.text(char, color, *decoration))

    fun text(double: Double, color: TextColor? = null, vararg decoration: TextDecoration) =
        append(Component.text(double, color, *decoration))

    fun text(float: Float, color: TextColor? = null, vararg decoration: TextDecoration) =
        append(Component.text(float, color, *decoration))

    fun text(int: Int, color: TextColor? = null, vararg decoration: TextDecoration) =
        append(Component.text(int, color, *decoration))

    fun text(long: Long, color: TextColor? = null, vararg decoration: TextDecoration) =
        append(Component.text(long, color, *decoration))

    fun primary(text: String, vararg decoration: TextDecoration) = text(text, PRIMARY, *decoration)
    fun primary(boolean: Boolean, vararg decoration: TextDecoration) =
        text(boolean, PRIMARY, *decoration)

    fun primary(char: Char, vararg decoration: TextDecoration) = text(char, PRIMARY, *decoration)
    fun primary(double: Double, vararg decoration: TextDecoration) =
        text(double, PRIMARY, *decoration)

    fun primary(float: Float, vararg decoration: TextDecoration) = text(float, PRIMARY, *decoration)
    fun primary(int: Int, vararg decoration: TextDecoration) = text(int, PRIMARY, *decoration)
    fun primary(long: Long, vararg decoration: TextDecoration) = text(long, PRIMARY, *decoration)

    fun secondary(text: String, vararg decoration: TextDecoration) =
        text(text, SECONDARY, *decoration)

    fun secondary(boolean: Boolean, vararg decoration: TextDecoration) =
        text(boolean, SECONDARY, *decoration)

    fun secondary(char: Char, vararg decoration: TextDecoration) =
        text(char, SECONDARY, *decoration)

    fun secondary(double: Double, vararg decoration: TextDecoration) =
        text(double, SECONDARY, *decoration)

    fun secondary(float: Float, vararg decoration: TextDecoration) =
        text(float, SECONDARY, *decoration)

    fun secondary(int: Int, vararg decoration: TextDecoration) = text(int, SECONDARY, *decoration)
    fun secondary(long: Long, vararg decoration: TextDecoration) =
        text(long, SECONDARY, *decoration)

    fun info(text: String, vararg decoration: TextDecoration) = text(text, INFO, *decoration)
    fun info(boolean: Boolean, vararg decoration: TextDecoration) = text(boolean, INFO, *decoration)
    fun info(char: Char, vararg decoration: TextDecoration) = text(char, INFO, *decoration)
    fun info(double: Double, vararg decoration: TextDecoration) = text(double, INFO, *decoration)
    fun info(float: Float, vararg decoration: TextDecoration) = text(float, INFO, *decoration)
    fun info(int: Int, vararg decoration: TextDecoration) = text(int, INFO, *decoration)
    fun info(long: Long, vararg decoration: TextDecoration) = text(long, INFO, *decoration)

    fun success(text: String, vararg decoration: TextDecoration) = text(text, SUCCESS, *decoration)
    fun success(boolean: Boolean, vararg decoration: TextDecoration) =
        text(boolean, SUCCESS, *decoration)

    fun success(char: Char, vararg decoration: TextDecoration) = text(char, SUCCESS, *decoration)
    fun success(double: Double, vararg decoration: TextDecoration) =
        text(double, SUCCESS, *decoration)

    fun success(float: Float, vararg decoration: TextDecoration) = text(float, SUCCESS, *decoration)
    fun success(int: Int, vararg decoration: TextDecoration) = text(int, SUCCESS, *decoration)
    fun success(long: Long, vararg decoration: TextDecoration) = text(long, SUCCESS, *decoration)

    fun warning(text: String, vararg decoration: TextDecoration) = text(text, WARNING, *decoration)
    fun warning(boolean: Boolean, vararg decoration: TextDecoration) =
        text(boolean, WARNING, *decoration)

    fun warning(char: Char, vararg decoration: TextDecoration) = text(char, WARNING, *decoration)
    fun warning(double: Double, vararg decoration: TextDecoration) =
        text(double, WARNING, *decoration)

    fun warning(float: Float, vararg decoration: TextDecoration) = text(float, WARNING, *decoration)
    fun warning(int: Int, vararg decoration: TextDecoration) = text(int, WARNING, *decoration)
    fun warning(long: Long, vararg decoration: TextDecoration) = text(long, WARNING, *decoration)


    fun error(text: String, vararg decoration: TextDecoration) = text(text, ERROR, *decoration)
    fun error(boolean: Boolean, vararg decoration: TextDecoration) =
        text(boolean, ERROR, *decoration)

    fun error(char: Char, vararg decoration: TextDecoration) = text(char, ERROR, *decoration)
    fun error(double: Double, vararg decoration: TextDecoration) = text(double, ERROR, *decoration)
    fun error(float: Float, vararg decoration: TextDecoration) = text(float, ERROR, *decoration)
    fun error(int: Int, vararg decoration: TextDecoration) = text(int, ERROR, *decoration)
    fun error(long: Long, vararg decoration: TextDecoration) = text(long, ERROR, *decoration)

    fun variableKey(text: String, vararg decoration: TextDecoration) =
        text(text, VARIABLE_KEY, *decoration)

    fun variableKey(boolean: Boolean, vararg decoration: TextDecoration) =
        text(boolean, VARIABLE_KEY, *decoration)

    fun variableKey(char: Char, vararg decoration: TextDecoration) =
        text(char, VARIABLE_KEY, *decoration)

    fun variableKey(double: Double, vararg decoration: TextDecoration) =
        text(double, VARIABLE_KEY, *decoration)

    fun variableKey(float: Float, vararg decoration: TextDecoration) =
        text(float, VARIABLE_KEY, *decoration)

    fun variableKey(int: Int, vararg decoration: TextDecoration) =
        text(int, VARIABLE_KEY, *decoration)

    fun variableKey(long: Long, vararg decoration: TextDecoration) =
        text(long, VARIABLE_KEY, *decoration)

    fun variableValue(text: String, vararg decoration: TextDecoration) =
        text(text, VARIABLE_VALUE, *decoration)

    fun variableValue(boolean: Boolean, vararg decoration: TextDecoration) =
        text(boolean, VARIABLE_VALUE, *decoration)

    fun variableValue(char: Char, vararg decoration: TextDecoration) =
        text(char, VARIABLE_VALUE, *decoration)

    fun variableValue(double: Double, vararg decoration: TextDecoration) =
        text(double, VARIABLE_VALUE, *decoration)

    fun variableValue(float: Float, vararg decoration: TextDecoration) =
        text(float, VARIABLE_VALUE, *decoration)

    fun variableValue(int: Int, vararg decoration: TextDecoration) =
        text(int, VARIABLE_VALUE, *decoration)

    fun variableValue(long: Long, vararg decoration: TextDecoration) =
        text(long, VARIABLE_VALUE, *decoration)

    fun spacer(text: String, vararg decoration: TextDecoration) = text(text, SPACER, *decoration)
    fun spacer(boolean: Boolean, vararg decoration: TextDecoration) =
        text(boolean, SPACER, *decoration)

    fun spacer(char: Char, vararg decoration: TextDecoration) = text(char, SPACER, *decoration)
    fun spacer(double: Double, vararg decoration: TextDecoration) =
        text(double, SPACER, *decoration)

    fun spacer(float: Float, vararg decoration: TextDecoration) = text(float, SPACER, *decoration)
    fun spacer(int: Int, vararg decoration: TextDecoration) = text(int, SPACER, *decoration)
    fun spacer(long: Long, vararg decoration: TextDecoration) = text(long, SPACER, *decoration)

    fun darkSpacer(text: String, vararg decoration: TextDecoration) =
        text(text, DARK_SPACER, *decoration)

    fun darkSpacer(boolean: Boolean, vararg decoration: TextDecoration) =
        text(boolean, DARK_SPACER, *decoration)

    fun darkSpacer(char: Char, vararg decoration: TextDecoration) =
        text(char, DARK_SPACER, *decoration)

    fun darkSpacer(double: Double, vararg decoration: TextDecoration) =
        text(double, DARK_SPACER, *decoration)

    fun darkSpacer(float: Float, vararg decoration: TextDecoration) =
        text(float, DARK_SPACER, *decoration)

    fun darkSpacer(int: Int, vararg decoration: TextDecoration) =
        text(int, DARK_SPACER, *decoration)

    fun darkSpacer(long: Long, vararg decoration: TextDecoration) =
        text(long, DARK_SPACER, *decoration)

    fun white(any: Any, vararg decoration: TextDecoration) =
        text(any.toString(), Colors.WHITE, *decoration)

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