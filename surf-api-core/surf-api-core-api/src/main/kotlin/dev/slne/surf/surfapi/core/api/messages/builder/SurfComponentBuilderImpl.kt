package dev.slne.surf.surfapi.core.api.messages.builder

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.*
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEventSource
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.util.ARGBLike
import java.util.function.Consumer
import java.util.function.Function

internal class SurfComponentBuilderImpl(private val delegate: TextComponent.Builder) : SurfComponentBuilder {
    private fun withDelegate(block: TextComponent.Builder.() -> Unit): SurfComponentBuilderImpl {
        delegate.block()
        return this
    }

    override fun content(content: String) = withDelegate { content(content) }
    override fun append(builder: ComponentBuilder<*, *>) = withDelegate { append(builder) }
    override fun append(component: Component) = withDelegate { append(component) }
    override fun append(component: ComponentLike) = withDelegate { append(component) }
    override fun append(components: Iterable<ComponentLike?>) = withDelegate { append(components) }
    override fun append(vararg components: Component) = withDelegate { append(*components) }
    override fun append(vararg components: ComponentLike) = withDelegate { append(*components) }
    override fun appendNewline() = withDelegate { appendNewline() }
    override fun appendSpace() = withDelegate { appendSpace() }
    override fun applicableApply(applicable: ComponentBuilderApplicable) =
        withDelegate { applicableApply(applicable) }

    override fun apply(consumer: Consumer<in ComponentBuilder<*, *>>) =
        withDelegate { apply(consumer) }

    override fun applyDeep(action: Consumer<in ComponentBuilder<*, *>>) =
        withDelegate { applyDeep(action) }

    override fun clickEvent(event: ClickEvent?) = withDelegate { clickEvent(event) }
    override fun color(color: TextColor?) = withDelegate { color(color) }
    override fun colorIfAbsent(color: TextColor?) = withDelegate { colorIfAbsent(color) }
    override fun decorate(decoration: TextDecoration) = withDelegate { decorate(decoration) }
    override fun decorate(vararg decorations: TextDecoration) =
        withDelegate { decorate(*decorations) }

    override fun decoration(
        decoration: TextDecoration,
        flag: Boolean,
    ) = withDelegate { decoration(decoration, flag) }

    override fun decoration(
        decoration: TextDecoration,
        state: TextDecoration.State,
    ) = withDelegate { decoration(decoration, state) }

    override fun decorationIfAbsent(
        decoration: TextDecoration,
        state: TextDecoration.State,
    ) = withDelegate { decorationIfAbsent(decoration, state) }

    override fun decorations(decorations: Map<TextDecoration?, TextDecoration.State?>) =
        withDelegate { decorations(decorations) }

    override fun decorations(
        decorations: Set<TextDecoration?>,
        flag: Boolean,
    ) = withDelegate { decorations(decorations, flag) }

    override fun font(font: Key?) = withDelegate { font(font) }
    override fun hoverEvent(source: HoverEventSource<*>?) = withDelegate { hoverEvent(source) }
    override fun insertion(insertion: String?) = withDelegate { insertion(insertion) }
    override fun mapChildren(function: Function<BuildableComponent<*, *>?, out BuildableComponent<*, *>?>) =
        withDelegate { mapChildren(function) }

    override fun mapChildrenDeep(function: Function<BuildableComponent<*, *>?, out BuildableComponent<*, *>?>) =
        withDelegate { mapChildrenDeep(function) }

    override fun mergeStyle(that: Component) = withDelegate { mergeStyle(that) }
    override fun mergeStyle(
        that: Component,
        merges: Set<Style.Merge?>,
    ) = withDelegate { mergeStyle(that, merges) }

    override fun mergeStyle(
        that: Component,
        vararg merges: Style.Merge,
    ) = withDelegate { mergeStyle(that, *merges) }

    override fun resetStyle() = withDelegate { resetStyle() }
    override fun style(consumer: Consumer<Style.Builder?>) = withDelegate { style(consumer) }
    override fun style(style: Style) = withDelegate { style(style) }
    override fun shadowColor(argb: ARGBLike?) = withDelegate { shadowColor(argb) }
    override fun shadowColorIfAbsent(argb: ARGBLike?) = withDelegate { shadowColorIfAbsent(argb) }
    override fun content() = delegate.content()
    override fun children() = delegate.children()
    override fun build() = delegate.build()
}