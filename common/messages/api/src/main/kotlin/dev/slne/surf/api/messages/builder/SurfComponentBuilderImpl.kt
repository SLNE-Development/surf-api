package dev.slne.surf.api.messages.builder

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

internal class SurfComponentBuilderImpl(
    private val delegate: TextComponent.Builder,
) : SurfComponentBuilder() {
    private fun withDelegate(block: TextComponent.Builder.() -> Unit): SurfComponentBuilderImpl {
        delegate.block()

        return this
    }

    override fun content(content: String): SurfComponentBuilderImpl {
        return withDelegate { content(content) }
    }

    override fun content(): String = delegate.content()
    override fun children(): List<Component> = delegate.children()
    override fun build(): TextComponent {
        return delegate.asComponent() as TextComponent
    }

    override fun append(builder: ComponentBuilder<*, *>): SurfComponentBuilderImpl {
        return withDelegate { append(builder) }
    }

    override fun append(component: Component): SurfComponentBuilderImpl {
        return withDelegate { append(component) }
    }

    override fun append(component: ComponentLike): SurfComponentBuilderImpl {
        return withDelegate { append(component) }
    }

    override fun append(components: Iterable<ComponentLike>): SurfComponentBuilderImpl {
        return withDelegate { append(components) }
    }

    override fun append(vararg components: Component): SurfComponentBuilderImpl {
        return withDelegate { append(*components) }
    }

    override fun append(vararg components: ComponentLike): SurfComponentBuilderImpl {
        return withDelegate { append(*components) }
    }

    override fun appendNewline(): SurfComponentBuilderImpl = withDelegate { appendNewline() }
    override fun appendSpace(): SurfComponentBuilderImpl = withDelegate { appendSpace() }
    override fun applicableApply(applicable: ComponentBuilderApplicable): SurfComponentBuilderImpl {
        return withDelegate { applicableApply(applicable) }
    }

    override fun apply(consumer: Consumer<in ComponentBuilder<*, *>>): SurfComponentBuilderImpl {
        return withDelegate { apply(consumer) }
    }

    override fun applyDeep(action: Consumer<in ComponentBuilder<*, *>>): SurfComponentBuilderImpl {
        return withDelegate { applyDeep(action) }
    }

    override fun clickEvent(event: ClickEvent<*>?): SurfComponentBuilderImpl {
        return withDelegate { clickEvent(event) }
    }

    override fun color(color: TextColor?): SurfComponentBuilderImpl = withDelegate { color(color) }
    override fun colorIfAbsent(color: TextColor?): SurfComponentBuilderImpl {
        return withDelegate { colorIfAbsent(color) }
    }

    override fun decorate(decoration: TextDecoration): SurfComponentBuilderImpl {
        return withDelegate { decorate(decoration) }
    }

    override fun decorate(vararg decorations: TextDecoration): SurfComponentBuilderImpl {
        return withDelegate { decorate(*decorations) }
    }

    override fun decoration(
        decoration: TextDecoration,
        flag: Boolean,
    ): SurfComponentBuilderImpl = withDelegate { decoration(decoration, flag) }

    override fun decoration(
        decoration: TextDecoration,
        state: TextDecoration.State,
    ): SurfComponentBuilderImpl = withDelegate { decoration(decoration, state) }

    override fun decorationIfAbsent(
        decoration: TextDecoration,
        state: TextDecoration.State,
    ): SurfComponentBuilderImpl = withDelegate { decorationIfAbsent(decoration, state) }

    override fun decorations(
        decorations: Map<TextDecoration, TextDecoration.State>,
    ): SurfComponentBuilderImpl = withDelegate { decorations(decorations) }

    override fun decorations(
        decorations: Set<TextDecoration>,
        flag: Boolean,
    ): SurfComponentBuilderImpl = withDelegate { decorations(decorations, flag) }

    override fun font(font: Key?): SurfComponentBuilderImpl = withDelegate { font(font) }
    override fun hoverEvent(source: HoverEventSource<*>?): SurfComponentBuilderImpl {
        return withDelegate { hoverEvent(source) }
    }

    override fun insertion(insertion: String?): SurfComponentBuilderImpl {
        return withDelegate { insertion(insertion) }
    }

    override fun mapChildren(
        function: Function<Component, out Component>,
    ): SurfComponentBuilderImpl {
        return withDelegate { mapChildren(function) }
    }

    override fun mapChildrenDeep(
        function: Function<Component, out Component>,
    ): SurfComponentBuilderImpl {
        return withDelegate { mapChildrenDeep(function) }
    }

    override fun mergeStyle(that: Component): SurfComponentBuilderImpl {
        return withDelegate { mergeStyle(that) }
    }

    override fun mergeStyle(
        that: Component,
        merges: Set<Style.Merge>,
    ): SurfComponentBuilderImpl = withDelegate { mergeStyle(that, merges) }

    override fun mergeStyle(
        that: Component,
        vararg merges: Style.Merge,
    ): SurfComponentBuilderImpl = withDelegate { mergeStyle(that, *merges) }

    override fun resetStyle(): SurfComponentBuilderImpl = withDelegate { resetStyle() }
    override fun style(consumer: Consumer<Style.Builder>): SurfComponentBuilderImpl {
        return withDelegate { style(consumer) }
    }

    override fun style(style: Style): SurfComponentBuilderImpl = withDelegate { style(style) }
    override fun shadowColor(argb: ARGBLike?): SurfComponentBuilderImpl {
        return withDelegate { shadowColor(argb) }
    }

    override fun shadowColorIfAbsent(argb: ARGBLike?): SurfComponentBuilderImpl {
        return withDelegate { shadowColorIfAbsent(argb) }
    }

    override fun asComponent(): Component {
        return build()
    }
}
