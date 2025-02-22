package dev.slne.surf.surfapi.core.api.messages.adventure

import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.BuildableComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentBuilder
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor

inline fun buildText(block: SurfComponentBuilder.() -> Unit): TextComponent {
    return SurfComponentBuilder(block)
}

fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.appendText(
    text: String,
    color: TextColor? = null,
) = append(Component.text(text, color))

fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.appendText(
    text: String,
    color: TextColor? = null,
    block: TextComponent.Builder.() -> Unit,
) = append(Component.text().appendText(text, color).apply(block))


fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.clickOpensUrl(
    url: String,
) = clickEvent(ClickEvent.openUrl(url))

fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.appendNewline(
    amount: Int,
) = repeat(amount) { appendNewline() }

fun text(content: String, color: TextColor? = null): TextComponent = Component.text(content, color)
fun text(number: Number, color: TextColor? = null): TextComponent =
    Component.text(number.toString(), color)
fun text(boolean: Boolean, color: TextColor? = null): TextComponent =
    Component.text(boolean, color)
fun text(char: Char, color: TextColor? = null): TextComponent = Component.text(char, color)
fun text(any: Any, color: TextColor? = null): TextComponent = Component.text(any.toString(), color)