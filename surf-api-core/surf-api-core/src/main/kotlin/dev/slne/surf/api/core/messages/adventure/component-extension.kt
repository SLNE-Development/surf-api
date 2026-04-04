@file:Suppress("DEPRECATION")

package dev.slne.surf.api.core.messages.adventure

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.BuildableComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentBuilder
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

/**
 * Builds a [TextComponent] using the provided [block] to configure the component.
 *
 * @param block The configuration block for the [SurfComponentBuilder].
 * @return A configured [TextComponent] instance.
 */
inline fun buildText(block: SurfComponentBuilder.() -> Unit): TextComponent {
    return SurfComponentBuilder(block)
}

/**
 * Appends a text component with the given [text] and optional [color] to the builder.
 *
 * @param text The text content to append.
 * @param color The optional text color.
 * @return The modified builder instance.
 */
@Suppress("DEPRECATION")
fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.appendText(
    text: String,
    color: TextColor? = null,
) = append(Component.text(text, color))

/**
 * Appends a text component with the given [text] and optional [color] to the builder,
 * allowing further configuration using the provided [block].
 *
 * @param text The text content to append.
 * @param color The optional text color.
 * @param block The configuration block for the text component.
 * @return The modified builder instance.
 */
@Suppress("DEPRECATION")
fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.appendText(
    text: String,
    color: TextColor? = null,
    block: TextComponent.Builder.() -> Unit,
) = append(Component.text().appendText(text, color).apply(block))

/**
 * Sets a click event on the component that opens the given [url] when clicked.
 *
 * @param url The URL to open on click.
 * @return The modified builder instance.
 */
@Suppress("DEPRECATION")
fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.clickOpensUrl(
    url: String,
) = clickEvent(ClickEvent.openUrl(url))

/**
 * Sets a click event on the component that runs the specified [command] when clicked.
 *
 * @param command The command to run on click.
 * @return The modified builder instance.
 */
@Suppress("DEPRECATION")
fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.clickRunsCommand(
    command: String,
) = clickEvent(ClickEvent.runCommand(command))

/**
 * Sets a click event on the component that suggests the specified [command] when clicked.
 *
 * @param command The command to suggest on click.
 * @return The modified builder instance.
 */
@Suppress("DEPRECATION")
fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.clickSuggestsCommand(
    command: String,
) = clickEvent(ClickEvent.suggestCommand(command))

/**
 * Sets a click event on the component that copies the specified [value] to the clipboard when clicked.
 *
 * @param value The value to copy to the clipboard on click.
 * @return The modified builder instance.
 */
@Suppress("DEPRECATION")
fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.clickCopiesToClipboard(
    value: String,
) = clickEvent(ClickEvent.copyToClipboard(value))

/**
 * Appends a newline character to the builder the specified number of times.
 *
 * @param amount The number of newline characters to append.
 * @return The modified builder instance.
 */
@Suppress("DEPRECATION")
fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.appendNewline(
    amount: Int,
) = repeat(amount) { appendNewline() }

/**
 * Creates a [TextComponent] with the given string [content] and optional [color].
 *
 * @param content The text content.
 * @param color The optional text color.
 * @return A [TextComponent] containing the given text.
 */
fun text(content: String, color: TextColor? = null): TextComponent = Component.text(content, color)

/**
 * Creates a [TextComponent] with the given number [value] converted to a string and optional [color].
 *
 * @param number The numeric value to convert to text.
 * @param color The optional text color.
 * @return A [TextComponent] containing the given number as text.
 */
fun text(number: Number, color: TextColor? = null): TextComponent =
    Component.text(number.toString(), color)

/**
 * Creates a [TextComponent] with the given boolean [value] and optional [color].
 *
 * @param boolean The boolean value.
 * @param color The optional text color.
 * @return A [TextComponent] containing the boolean value as text.
 */
fun text(boolean: Boolean, color: TextColor? = null): TextComponent =
    Component.text(boolean, color)

/**
 * Creates a [TextComponent] with the given character [char] and optional [color].
 *
 * @param char The character value.
 * @param color The optional text color.
 * @return A [TextComponent] containing the character.
 */
fun text(char: Char, color: TextColor? = null): TextComponent = Component.text(char, color)

/**
 * Creates a [TextComponent] with the given object's [toString] value and optional [color].
 *
 * @param any The object to convert to text.
 * @param color The optional text color.
 * @return A [TextComponent] containing the object's string representation.
 */
fun text(any: Any, color: TextColor? = null): TextComponent = Component.text(any.toString(), color)

fun Component.plain() = PlainTextComponentSerializer.plainText().serialize(this)