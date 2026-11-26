package dev.slne.surf.api.messages.builder

import dev.slne.surf.api.messages.Colors
import dev.slne.surf.api.messages.CommonComponents
import dev.slne.surf.api.messages.NoLowercase
import dev.slne.surf.api.messages.adventure.ClickCallbackWithOptionsBuilder
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

/**
 * Mutable builder for a [TextComponent] with the surf palette, prefixes and formatting helpers.
 *
 * Every call appends to or styles the component being built and returns this builder. The builder
 * stays usable after [build], so it can be built more than once.
 *
 * Members that take a block are `inline`: the block runs in the caller's frame and may suspend when
 * the caller does. They evaluate their blocks into components and pass those to a non-inline
 * overload.
 */
@ApiStatus.NonExtendable
abstract class SurfComponentBuilder internal constructor() : ComponentLike {
    companion object {
        /**
         * Creates an empty builder.
         *
         * @return a new builder
         */
        fun builder(): SurfComponentBuilder = SurfComponentBuilderImpl(Component.text())

        /**
         * Creates an empty builder.
         *
         * @return a new builder
         */
        operator fun invoke(): SurfComponentBuilder = builder()

        /**
         * Builds a [TextComponent] from [block].
         *
         * @param block configures the builder
         * @return the built component
         */
        inline operator fun invoke(block: SurfComponentBuilder.() -> Unit): TextComponent {
            val builder = builder()
            builder.block()

            return builder.build()
        }
    }

    /**
     * Appends the component built by [block] as a child.
     *
     * @param block configures the child builder
     * @return this builder
     */
    inline fun append(block: SurfComponentBuilder.() -> Unit): SurfComponentBuilder {
        return append(SurfComponentBuilder(block))
    }

    /**
     * Appends a newline, then the component built by [block].
     *
     * @param block configures the child builder
     * @return this builder
     */
    inline fun appendNewline(block: SurfComponentBuilder.() -> Unit): SurfComponentBuilder {
        return appendNewline().append(block)
    }

    /**
     * Appends [amount] newlines.
     *
     * @param amount the number of newlines
     * @return this builder
     */
    fun appendNewline(amount: Int): SurfComponentBuilder {
        repeat(amount) { appendNewline() }

        return this
    }

    fun text(
        text: String,
        color: TextColor? = null,
        vararg decoration: TextDecoration,
    ): SurfComponentBuilder {
        return append(Component.text(text, color, *decoration))
    }

    fun text(
        boolean: Boolean,
        color: TextColor? = null,
        vararg decoration: TextDecoration,
    ): SurfComponentBuilder {
        return append(Component.text(boolean, color, *decoration))
    }

    fun text(
        char: Char,
        color: TextColor? = null,
        vararg decoration: TextDecoration,
    ): SurfComponentBuilder {
        return append(Component.text(char, color, *decoration))
    }

    /**
     * Appends [number] as text.
     *
     * `Double`, `Float`, `Int` and `Long` use Adventure's numeric overloads; any other number is
     * appended through `toString()`.
     *
     * @param number the number to append
     * @param color the text color, or `null` to inherit it
     * @param decoration the decorations to apply
     * @return this builder
     */
    fun text(
        number: Number,
        color: TextColor? = null,
        vararg decoration: TextDecoration,
    ): SurfComponentBuilder {
        val component = when (number) {
            is Double -> Component.text(number, color, *decoration)
            is Float -> Component.text(number, color, *decoration)
            is Int -> Component.text(number, color, *decoration)
            is Long -> Component.text(number, color, *decoration)
            else -> Component.text(number.toString(), color, *decoration)
        }

        return append(component)
    }

    /**
     * Appends [text] in [color].
     *
     * The named color helpers such as [primary] or [red] delegate here.
     *
     * @param text the text to append
     * @param color the text color
     * @param decoration the decorations to apply
     * @return this builder
     */
    fun colored(
        text: String,
        color: TextColor,
        vararg decoration: TextDecoration,
    ): SurfComponentBuilder {
        return this.text(text, color, *decoration)
    }

    fun colored(
        boolean: Boolean,
        color: TextColor,
        vararg decoration: TextDecoration,
    ): SurfComponentBuilder {
        return text(boolean, color, *decoration)
    }

    fun colored(
        char: Char,
        color: TextColor,
        vararg decoration: TextDecoration,
    ): SurfComponentBuilder {
        return text(char, color, *decoration)
    }

    fun colored(
        number: Number,
        color: TextColor,
        vararg decoration: TextDecoration,
    ): SurfComponentBuilder {
        return text(number, color, *decoration)
    }

    fun primary(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.PRIMARY, *decoration)
    }

    fun primary(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.PRIMARY, *decoration)
    }

    fun primary(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.PRIMARY, *decoration)
    }

    fun primary(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.PRIMARY, *decoration)
    }

    fun secondary(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.SECONDARY, *decoration)
    }

    fun secondary(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.SECONDARY, *decoration)
    }

    fun secondary(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.SECONDARY, *decoration)
    }

    fun secondary(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.SECONDARY, *decoration)
    }

    fun info(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.INFO, *decoration)
    }

    fun info(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.INFO, *decoration)
    }

    fun info(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.INFO, *decoration)
    }

    fun info(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.INFO, *decoration)
    }

    fun success(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.SUCCESS, *decoration)
    }

    fun success(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.SUCCESS, *decoration)
    }

    fun success(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.SUCCESS, *decoration)
    }

    fun success(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.SUCCESS, *decoration)
    }

    fun warning(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.WARNING, *decoration)
    }

    fun warning(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.WARNING, *decoration)
    }

    fun warning(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.WARNING, *decoration)
    }

    fun warning(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.WARNING, *decoration)
    }

    fun error(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.ERROR, *decoration)
    }

    fun error(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.ERROR, *decoration)
    }

    fun error(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.ERROR, *decoration)
    }

    fun error(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.ERROR, *decoration)
    }

    fun variableKey(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.VARIABLE_KEY, *decoration)
    }

    fun variableKey(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.VARIABLE_KEY, *decoration)
    }

    fun variableKey(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.VARIABLE_KEY, *decoration)
    }

    fun variableKey(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.VARIABLE_KEY, *decoration)
    }

    fun variableValue(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.VARIABLE_VALUE, *decoration)
    }

    fun variableValue(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.VARIABLE_VALUE, *decoration)
    }

    fun variableValue(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.VARIABLE_VALUE, *decoration)
    }

    fun variableValue(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.VARIABLE_VALUE, *decoration)
    }

    fun spacer(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.SPACER, *decoration)
    }

    fun spacer(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.SPACER, *decoration)
    }

    fun spacer(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.SPACER, *decoration)
    }

    fun spacer(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.SPACER, *decoration)
    }

    fun darkSpacer(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.DARK_SPACER, *decoration)
    }

    fun darkSpacer(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.DARK_SPACER, *decoration)
    }

    fun darkSpacer(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.DARK_SPACER, *decoration)
    }

    fun darkSpacer(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.DARK_SPACER, *decoration)
    }

    fun black(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.BLACK, *decoration)
    }

    fun black(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.BLACK, *decoration)
    }

    fun black(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.BLACK, *decoration)
    }

    fun black(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.BLACK, *decoration)
    }

    fun darkBlue(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.DARK_BLUE, *decoration)
    }

    fun darkBlue(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.DARK_BLUE, *decoration)
    }

    fun darkBlue(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.DARK_BLUE, *decoration)
    }

    fun darkBlue(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.DARK_BLUE, *decoration)
    }

    fun darkGreen(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.DARK_GREEN, *decoration)
    }

    fun darkGreen(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.DARK_GREEN, *decoration)
    }

    fun darkGreen(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.DARK_GREEN, *decoration)
    }

    fun darkGreen(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.DARK_GREEN, *decoration)
    }

    fun darkAqua(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.DARK_AQUA, *decoration)
    }

    fun darkAqua(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.DARK_AQUA, *decoration)
    }

    fun darkAqua(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.DARK_AQUA, *decoration)
    }

    fun darkAqua(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.DARK_AQUA, *decoration)
    }

    fun darkRed(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.DARK_RED, *decoration)
    }

    fun darkRed(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.DARK_RED, *decoration)
    }

    fun darkRed(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.DARK_RED, *decoration)
    }

    fun darkRed(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.DARK_RED, *decoration)
    }

    fun darkPurple(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.DARK_PURPLE, *decoration)
    }

    fun darkPurple(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.DARK_PURPLE, *decoration)
    }

    fun darkPurple(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.DARK_PURPLE, *decoration)
    }

    fun darkPurple(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.DARK_PURPLE, *decoration)
    }

    fun gold(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.GOLD, *decoration)
    }

    fun gold(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.GOLD, *decoration)
    }

    fun gold(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.GOLD, *decoration)
    }

    fun gold(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.GOLD, *decoration)
    }

    fun gray(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.GRAY, *decoration)
    }

    fun gray(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.GRAY, *decoration)
    }

    fun gray(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.GRAY, *decoration)
    }

    fun gray(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.GRAY, *decoration)
    }

    fun darkGray(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.DARK_GRAY, *decoration)
    }

    fun darkGray(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.DARK_GRAY, *decoration)
    }

    fun darkGray(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.DARK_GRAY, *decoration)
    }

    fun darkGray(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.DARK_GRAY, *decoration)
    }

    fun blue(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.BLUE, *decoration)
    }

    fun blue(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.BLUE, *decoration)
    }

    fun blue(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.BLUE, *decoration)
    }

    fun blue(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.BLUE, *decoration)
    }

    fun green(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.GREEN, *decoration)
    }

    fun green(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.GREEN, *decoration)
    }

    fun green(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.GREEN, *decoration)
    }

    fun green(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.GREEN, *decoration)
    }

    fun aqua(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.AQUA, *decoration)
    }

    fun aqua(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.AQUA, *decoration)
    }

    fun aqua(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.AQUA, *decoration)
    }

    fun aqua(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.AQUA, *decoration)
    }

    fun red(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.RED, *decoration)
    }

    fun red(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.RED, *decoration)
    }

    fun red(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.RED, *decoration)
    }

    fun red(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.RED, *decoration)
    }

    fun lightPurple(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.LIGHT_PURPLE, *decoration)
    }

    fun lightPurple(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.LIGHT_PURPLE, *decoration)
    }

    fun lightPurple(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.LIGHT_PURPLE, *decoration)
    }

    fun lightPurple(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.LIGHT_PURPLE, *decoration)
    }

    fun yellow(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.YELLOW, *decoration)
    }

    fun yellow(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.YELLOW, *decoration)
    }

    fun yellow(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.YELLOW, *decoration)
    }

    fun yellow(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.YELLOW, *decoration)
    }

    fun white(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.WHITE, *decoration)
    }

    fun white(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.WHITE, *decoration)
    }

    fun white(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.WHITE, *decoration)
    }

    fun white(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.WHITE, *decoration)
    }

    fun note(text: String, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(text, Colors.NOTE, *decoration)
    }

    fun note(boolean: Boolean, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(boolean, Colors.NOTE, *decoration)
    }

    fun note(char: Char, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(char, Colors.NOTE, *decoration)
    }

    fun note(number: Number, vararg decoration: TextDecoration): SurfComponentBuilder {
        return colored(number, Colors.NOTE, *decoration)
    }

    /**
     * Appends a key-value pair such as `Coins: 42`.
     *
     * The key is in variable-key color, the `": "` separator in spacer color and the value in
     * variable-value color.
     *
     * @return this builder
     */
    fun keyValue(key: String, value: String): SurfComponentBuilder {
        return appendKeyAndSeparator(key).variableValue(value)
    }

    /**
     * Appends a key-value pair with a [Boolean] value, styled like `keyValue(String, String)`.
     *
     * @return this builder
     */
    fun keyValue(key: String, value: Boolean): SurfComponentBuilder {
        return appendKeyAndSeparator(key).variableValue(value)
    }

    /**
     * Appends a key-value pair with a [Char] value, styled like `keyValue(String, String)`.
     *
     * @return this builder
     */
    fun keyValue(key: String, value: Char): SurfComponentBuilder {
        return appendKeyAndSeparator(key).variableValue(value)
    }

    /**
     * Appends a key-value pair with a [Number] value, styled like `keyValue(String, String)`.
     *
     * @return this builder
     */
    fun keyValue(key: String, value: Number): SurfComponentBuilder {
        return appendKeyAndSeparator(key).variableValue(value)
    }

    private fun appendKeyAndSeparator(key: String): SurfComponentBuilder {
        return variableKey(key).spacer(": ")
    }

    /**
     * Appends an em dash with a space on each side (` — `) in spacer color to separate two parts
     * of a sentence.
     *
     * @return this builder
     */
    fun appendEmDash(): SurfComponentBuilder = spacer(" — ")

    /**
     * Appends an en dash without spaces (`–`) in spacer color, as in a range such as `1–5`.
     *
     * @return this builder
     */
    fun appendEnDash(): SurfComponentBuilder = append(CommonComponents.EN_DASH)

    fun appendInfoPrefix(): SurfComponentBuilder = append(Colors.INFO_PREFIX)

    fun appendNewInfoPrefixedLine(amount: Int = 1): SurfComponentBuilder {
        repeat(amount) { appendNewline().appendInfoPrefix() }

        return this
    }

    inline fun appendNewInfoPrefixedLine(
        block: SurfComponentBuilder.() -> Unit,
    ): SurfComponentBuilder {
        return appendNewInfoPrefixedLine().append(block)
    }

    fun appendSuccessPrefix(): SurfComponentBuilder = append(Colors.SUCCESS_PREFIX)

    fun appendNewSuccessPrefixedLine(amount: Int = 1): SurfComponentBuilder {
        repeat(amount) { appendNewline().appendSuccessPrefix() }

        return this
    }

    inline fun appendNewSuccessPrefixedLine(
        block: SurfComponentBuilder.() -> Unit,
    ): SurfComponentBuilder {
        return appendNewSuccessPrefixedLine().append(block)
    }

    fun appendWarningPrefix(): SurfComponentBuilder = append(Colors.WARNING_PREFIX)

    fun appendNewWarningPrefixedLine(amount: Int = 1): SurfComponentBuilder {
        repeat(amount) { appendNewline().appendWarningPrefix() }

        return this
    }

    inline fun appendNewWarningPrefixedLine(
        block: SurfComponentBuilder.() -> Unit,
    ): SurfComponentBuilder {
        return appendNewWarningPrefixedLine().append(block)
    }

    fun appendErrorPrefix(): SurfComponentBuilder = append(Colors.ERROR_PREFIX)

    fun appendNewErrorPrefixedLine(amount: Int = 1): SurfComponentBuilder {
        repeat(amount) { appendNewline().appendErrorPrefix() }

        return this
    }

    inline fun appendNewErrorPrefixedLine(
        block: SurfComponentBuilder.() -> Unit,
    ): SurfComponentBuilder {
        return appendNewErrorPrefixedLine().append(block)
    }

    fun appendText(text: String, color: TextColor? = null): SurfComponentBuilder {
        return this.text(text, color)
    }

    /**
     * Appends [text] in [color] as a child that [block] configures further.
     *
     * @param text the text to append
     * @param color the text color, or `null` to inherit it
     * @param block configures the Adventure builder of the child
     * @return this builder
     */
    inline fun appendText(
        text: String,
        color: TextColor? = null,
        block: TextComponent.Builder.() -> Unit,
    ): SurfComponentBuilder {
        val builder = Component.text()
            .content(text)
            .color(color)
        builder.block()

        return append(builder.build())
    }

    fun clickOpensUrl(url: String): SurfComponentBuilder = clickEvent(ClickEvent.openUrl(url))

    fun clickRunsCommand(command: String): SurfComponentBuilder {
        return clickEvent(ClickEvent.runCommand(command))
    }

    fun clickSuggestsCommand(command: String): SurfComponentBuilder {
        return clickEvent(ClickEvent.suggestCommand(command))
    }

    fun clickCopiesToClipboard(value: String): SurfComponentBuilder {
        return clickEvent(ClickEvent.copyToClipboard(value))
    }

    fun clickCallback(callback: ClickCallback<Audience>): SurfComponentBuilder {
        return clickEvent(ClickEvent.callback(callback))
    }

    fun clickCallbackWithOptions(
        builder: ClickCallbackWithOptionsBuilder<Audience>.() -> Unit,
    ): SurfComponentBuilder {
        val callbackBuilder = ClickCallbackWithOptionsBuilder(Audience::class.java)
        callbackBuilder.builder()

        return clickEvent(callbackBuilder.build())
    }

    fun ellipsis(color: TextColor? = Colors.SPACER): SurfComponentBuilder {
        return append(CommonComponents.ELLIPSIS.color(color))
    }

    fun translatable(
        key: String,
        color: TextColor? = Colors.WHITE,
        vararg decoration: TextDecoration,
    ): SurfComponentBuilder {
        return append(Component.translatable(key, color, *decoration))
    }

    fun appendDiscordLink(): SurfComponentBuilder = append(CommonComponents.DISCORD_LINK)

    fun appendDisconnectHeader(): SurfComponentBuilder = append(CommonComponents.DISCONNECT_HEADER)

    /**
     * Appends the retry footer, or the issue footer with the Discord link when [issue] is `true`.
     *
     * @param issue whether to append the issue footer
     * @return this builder
     */
    fun appendDisconnectFooterTryAgainLater(issue: Boolean): SurfComponentBuilder {
        if (issue) {
            return append(CommonComponents.ISSUE_FOOTER)
        }

        return append(CommonComponents.RETRY_LATER_FOOTER)
    }

    /**
     * Renders a kick message into this builder.
     *
     * @param messageRenderer builds the message below the kick headline
     * @param footerRenderer builds the footer below the message
     * @return this builder
     * @see CommonComponents.renderKickDisconnectMessage
     */
    inline fun appendKickDisconnectMessage(
        messageRenderer: SurfComponentBuilder.() -> Unit,
        footerRenderer: SurfComponentBuilder.() -> Unit = { },
    ): SurfComponentBuilder {
        return appendKickDisconnectMessage(
            message = SurfComponentBuilder(messageRenderer),
            footer = SurfComponentBuilder(footerRenderer),
        )
    }

    /**
     * Renders a kick message into this builder: [CommonComponents.DISCONNECT_HEADER], the kick
     * headline, [message] and [footer]. Headline, message and footer are separated by three
     * newlines.
     *
     * @param message the message below the kick headline
     * @param footer the footer below the message
     * @return this builder
     */
    fun appendKickDisconnectMessage(
        message: Component,
        footer: Component = Component.empty(),
    ): SurfComponentBuilder {
        appendDisconnectHeader()
        text("DU WURDEST VOM SERVER GEWORFEN", Colors.ERROR)
        appendNewline(3)
        append(message)
        appendNewline(3)

        return append(footer)
    }

    /**
     * Renders a disconnect message into this builder.
     *
     * @param disconnectReason the headline, displayed in uppercase
     * @param suggestHelp builds the help text below the headline
     * @param footerRenderer builds the footer below the help text
     * @return this builder
     * @see CommonComponents.renderDisconnectMessage
     */
    inline fun appendDisconnectMessage(
        disconnectReason: @NoLowercase String,
        suggestHelp: SurfComponentBuilder.() -> Unit,
        footerRenderer: SurfComponentBuilder.() -> Unit = { },
    ): SurfComponentBuilder {
        return appendDisconnectMessage(
            disconnectReason = disconnectReason,
            help = SurfComponentBuilder(suggestHelp),
            footer = SurfComponentBuilder(footerRenderer),
        )
    }

    /**
     * Renders a disconnect message into this builder: [CommonComponents.DISCONNECT_HEADER],
     * [disconnectReason] in uppercase, [help] and [footer]. Headline, help text and footer are
     * separated by three newlines.
     *
     * @param disconnectReason the headline, displayed in uppercase
     * @param help the help text below the headline
     * @param footer the footer below the help text
     * @return this builder
     */
    fun appendDisconnectMessage(
        disconnectReason: @NoLowercase String,
        help: Component,
        footer: Component = Component.empty(),
    ): SurfComponentBuilder {
        appendDisconnectHeader()
        text(disconnectReason.uppercase(), Colors.ERROR)
        appendNewline(3)
        append(help)
        appendNewline(3)

        return append(footer)
    }

    /**
     * Appends [collection] as a comma-separated list.
     *
     * @param collection the elements to append
     * @param formatter converts an element to a component
     * @return this builder
     * @see CommonComponents.formatCollection
     */
    inline fun <E> appendCollection(
        collection: Iterable<E>,
        formatter: (E) -> Component,
    ): SurfComponentBuilder {
        return append(CommonComponents.formatCollection(collection.map(formatter)))
    }

    /**
     * Appends [collection] with each element on a new line behind [linePrefix].
     *
     * @param collection the elements to append
     * @param linePrefix the component starting each line
     * @param formatter converts an element to a component
     * @return this builder
     * @see CommonComponents.formatCollectionNewLine
     */
    inline fun <E> appendCollectionNewLine(
        collection: Iterable<E>,
        linePrefix: Component = Colors.PREFIX,
        formatter: (E) -> Component,
    ): SurfComponentBuilder {
        val components = collection.map(formatter)

        return append(CommonComponents.formatCollectionNewLine(components, linePrefix))
    }

    /**
     * Appends [map] with each entry on a new line behind [linePrefix].
     *
     * @param map the entries to append
     * @param keyFormatter converts a key to a component
     * @param valueFormatter converts a value to a component
     * @param linePrefix the component starting each line
     * @param keyValueSeparator the component between key and value
     * @return this builder
     * @see CommonComponents.formatMap
     */
    inline fun <K, V> appendMap(
        map: Map<K, V>,
        keyFormatter: (K) -> Component,
        valueFormatter: (V) -> Component,
        linePrefix: Component = Colors.PREFIX,
        keyValueSeparator: Component = CommonComponents.MAP_SEPARATOR,
    ): SurfComponentBuilder {
        val entries = map.map { (key, value) -> keyFormatter(key) to valueFormatter(value) }

        return append(CommonComponents.formatMap(entries, linePrefix, keyValueSeparator))
    }

    /**
     * Appends [time] in German words or short forms.
     *
     * @param time the duration to format
     * @param showSeconds whether to include seconds
     * @param shortForms whether to use short unit forms such as `d` and `h`
     * @param separator the component between units
     * @param timeColor the color of the values and units
     * @return this builder
     * @see CommonComponents.formatTime
     */
    fun appendTime(
        time: Duration,
        showSeconds: Boolean = true,
        shortForms: Boolean = false,
        separator: Component = CommonComponents.TIME_SEPARATOR,
        timeColor: TextColor = Colors.VARIABLE_VALUE,
    ): SurfComponentBuilder {
        val formatted = CommonComponents.formatTime(
            time = time,
            showSeconds = showSeconds,
            shortForms = shortForms,
            separator = separator,
            timeColor = timeColor,
        )

        return append(formatted)
    }

    abstract fun content(): String
    abstract fun content(content: String): SurfComponentBuilder
    abstract fun children(): List<Component>

    abstract fun build(): TextComponent

    abstract fun append(builder: ComponentBuilder<*, *>): SurfComponentBuilder
    abstract fun append(component: Component): SurfComponentBuilder
    abstract fun append(component: ComponentLike): SurfComponentBuilder
    abstract fun append(components: Iterable<ComponentLike>): SurfComponentBuilder
    abstract fun append(vararg components: Component): SurfComponentBuilder
    abstract fun append(vararg components: ComponentLike): SurfComponentBuilder
    abstract fun appendNewline(): SurfComponentBuilder
    abstract fun appendSpace(): SurfComponentBuilder
    abstract fun applicableApply(applicable: ComponentBuilderApplicable): SurfComponentBuilder
    abstract fun apply(consumer: Consumer<in ComponentBuilder<*, *>>): SurfComponentBuilder
    abstract fun applyDeep(action: Consumer<in ComponentBuilder<*, *>>): SurfComponentBuilder

    abstract fun clickEvent(event: ClickEvent<*>?): SurfComponentBuilder

    abstract fun color(color: TextColor?): SurfComponentBuilder
    abstract fun colorIfAbsent(color: TextColor?): SurfComponentBuilder
    abstract fun decorate(decoration: TextDecoration): SurfComponentBuilder
    abstract fun decorate(vararg decorations: TextDecoration): SurfComponentBuilder
    abstract fun decoration(decoration: TextDecoration, flag: Boolean): SurfComponentBuilder
    abstract fun decoration(
        decoration: TextDecoration,
        state: TextDecoration.State,
    ): SurfComponentBuilder

    abstract fun decorationIfAbsent(
        decoration: TextDecoration,
        state: TextDecoration.State,
    ): SurfComponentBuilder

    abstract fun decorations(
        decorations: Map<TextDecoration, TextDecoration.State>,
    ): SurfComponentBuilder

    abstract fun decorations(
        decorations: Set<TextDecoration>,
        flag: Boolean,
    ): SurfComponentBuilder

    abstract fun font(font: Key?): SurfComponentBuilder
    abstract fun hoverEvent(source: HoverEventSource<*>?): SurfComponentBuilder
    abstract fun insertion(insertion: String?): SurfComponentBuilder

    abstract fun mapChildren(function: Function<Component, out Component>): SurfComponentBuilder

    abstract fun mapChildrenDeep(
        function: Function<Component, out Component>,
    ): SurfComponentBuilder

    abstract fun mergeStyle(that: Component): SurfComponentBuilder
    abstract fun mergeStyle(that: Component, merges: Set<Style.Merge>): SurfComponentBuilder
    abstract fun mergeStyle(that: Component, vararg merges: Style.Merge): SurfComponentBuilder
    abstract fun resetStyle(): SurfComponentBuilder
    abstract fun style(consumer: Consumer<Style.Builder>): SurfComponentBuilder
    abstract fun style(style: Style): SurfComponentBuilder
    abstract fun shadowColor(argb: ARGBLike?): SurfComponentBuilder
    abstract fun shadowColorIfAbsent(argb: ARGBLike?): SurfComponentBuilder
}

inline fun <reified T : Audience> SurfComponentBuilder.clickCallbackTypedWithOptions(
    builder: ClickCallbackWithOptionsBuilder<T>.() -> Unit,
): SurfComponentBuilder {
    val callbackBuilder = ClickCallbackWithOptionsBuilder(T::class.java)
    callbackBuilder.builder()

    return clickEvent(callbackBuilder.build())
}

inline fun <reified T : Audience> SurfComponentBuilder.clickCallbackTyped(
    callback: ClickCallback<T>,
): SurfComponentBuilder {
    return clickEvent(ClickEvent.callback(ClickCallback.widen(callback, T::class.java)))
}
