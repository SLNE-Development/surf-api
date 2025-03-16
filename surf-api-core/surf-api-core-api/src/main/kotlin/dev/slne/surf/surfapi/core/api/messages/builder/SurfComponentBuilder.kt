package dev.slne.surf.surfapi.core.api.messages.builder

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
interface SurfComponentBuilder : TextComponent.Builder {
    companion object {
        @JvmStatic
        fun builder(): SurfComponentBuilder = SurfComponentBuilderImpl(Component.text())

        operator fun invoke(): SurfComponentBuilder = builder()
        inline operator fun invoke(block: SurfComponentBuilder.() -> Unit) =
            builder().apply(block).build()
    }

    fun appendPrefix() = append(PREFIX)
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

    fun text(text: String, color: TextColor? = null) = append(Component.text(text, color))
    fun text(boolean: Boolean, color: TextColor? = null) = append(Component.text(boolean, color))
    fun text(char: Char, color: TextColor? = null) = append(Component.text(char, color))
    fun text(double: Double, color: TextColor? = null) = append(Component.text(double, color))
    fun text(float: Float, color: TextColor? = null) = append(Component.text(float, color))
    fun text(int: Int, color: TextColor? = null) = append(Component.text(int, color))
    fun text(long: Long, color: TextColor? = null) = append(Component.text(long, color))

    fun primary(text: String) = text(text, PRIMARY)
    fun primary(boolean: Boolean) = text(boolean, PRIMARY)
    fun primary(char: Char) = text(char, PRIMARY)
    fun primary(double: Double) = text(double, PRIMARY)
    fun primary(float: Float) = text(float, PRIMARY)
    fun primary(int: Int) = text(int, PRIMARY)
    fun primary(long: Long) = text(long, PRIMARY)

    fun secondary(text: String) = text(text, SECONDARY)
    fun secondary(boolean: Boolean) = text(boolean, SECONDARY)
    fun secondary(char: Char) = text(char, SECONDARY)
    fun secondary(double: Double) = text(double, SECONDARY)
    fun secondary(float: Float) = text(float, SECONDARY)
    fun secondary(int: Int) = text(int, SECONDARY)
    fun secondary(long: Long) = text(long, SECONDARY)

    fun info(text: String) = text(text, INFO)
    fun info(boolean: Boolean) = text(boolean, INFO)
    fun info(char: Char) = text(char, INFO)
    fun info(double: Double) = text(double, INFO)
    fun info(float: Float) = text(float, INFO)
    fun info(int: Int) = text(int, INFO)
    fun info(long: Long) = text(long, INFO)

    fun success(text: String) = text(text, SUCCESS)
    fun success(boolean: Boolean) = text(boolean, SUCCESS)
    fun success(char: Char) = text(char, SUCCESS)
    fun success(double: Double) = text(double, SUCCESS)
    fun success(float: Float) = text(float, SUCCESS)
    fun success(int: Int) = text(int, SUCCESS)
    fun success(long: Long) = text(long, SUCCESS)

    fun warning(text: String) = text(text, WARNING)
    fun warning(boolean: Boolean) = text(boolean, WARNING)
    fun warning(char: Char) = text(char, WARNING)
    fun warning(double: Double) = text(double, WARNING)
    fun warning(float: Float) = text(float, WARNING)
    fun warning(int: Int) = text(int, WARNING)
    fun warning(long: Long) = text(long, WARNING)


    fun error(text: String) = text(text, ERROR)
    fun error(boolean: Boolean) = text(boolean, ERROR)
    fun error(char: Char) = text(char, ERROR)
    fun error(double: Double) = text(double, ERROR)
    fun error(float: Float) = text(float, ERROR)
    fun error(int: Int) = text(int, ERROR)
    fun error(long: Long) = text(long, ERROR)

    fun variableKey(text: String) = text(text, VARIABLE_KEY)
    fun variableKey(boolean: Boolean) = text(boolean, VARIABLE_KEY)
    fun variableKey(char: Char) = text(char, VARIABLE_KEY)
    fun variableKey(double: Double) = text(double, VARIABLE_KEY)
    fun variableKey(float: Float) = text(float, VARIABLE_KEY)
    fun variableKey(int: Int) = text(int, VARIABLE_KEY)
    fun variableKey(long: Long) = text(long, VARIABLE_KEY)

    fun variableValue(text: String) = text(text, VARIABLE_VALUE)
    fun variableValue(boolean: Boolean) = text(boolean, VARIABLE_VALUE)
    fun variableValue(char: Char) = text(char, VARIABLE_VALUE)
    fun variableValue(double: Double) = text(double, VARIABLE_VALUE)
    fun variableValue(float: Float) = text(float, VARIABLE_VALUE)
    fun variableValue(int: Int) = text(int, VARIABLE_VALUE)
    fun variableValue(long: Long) = text(long, VARIABLE_VALUE)

    fun spacer(text: String) = text(text, SPACER)
    fun spacer(boolean: Boolean) = text(boolean, SPACER)
    fun spacer(char: Char) = text(char, SPACER)
    fun spacer(double: Double) = text(double, SPACER)
    fun spacer(float: Float) = text(float, SPACER)
    fun spacer(int: Int) = text(int, SPACER)
    fun spacer(long: Long) = text(long, SPACER)

    fun darkSpacer(text: String) = text(text, DARK_SPACER)
    fun darkSpacer(boolean: Boolean) = text(boolean, DARK_SPACER)
    fun darkSpacer(char: Char) = text(char, DARK_SPACER)
    fun darkSpacer(double: Double) = text(double, DARK_SPACER)
    fun darkSpacer(float: Float) = text(float, DARK_SPACER)
    fun darkSpacer(int: Int) = text(int, DARK_SPACER)
    fun darkSpacer(long: Long) = text(long, DARK_SPACER)

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