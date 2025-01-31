package dev.slne.surf.surfapi.core.api.messages

import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.BuildableComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentBuilder
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor

inline fun buildText(block: SurfComponentBuilder.() -> Unit): TextComponent {
    val builder = SurfComponentBuilder.builder()
    with(builder) { block() }
    return builder.build()
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