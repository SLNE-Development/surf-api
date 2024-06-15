package dev.slne.surf.surfapi.core.api.command.builder;

import dev.slne.surf.surfapi.core.api.messages.Colors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * You can use this class to build a command exception message. For example, if a command fails to
 * parse the input, you can use this class to build a message that shows the user where the error
 * occurred.
 * <p>
 * The message will be built with the following format:
 * <pre>
 *         [prefix] [detailErrorMessage]
 *         [prefix] At position [cursor]: [context]
 *     </pre>
 * Where:
 *     <ul>
 *         <li>[prefix] is an optional prefix that will be added to the message</li>
 *         <li>[detailErrorMessage] is an optional detail error message</li>
 *         <li>[cursor] is the position of the error in the input</li>
 *         <li>[context] is the context of the error</li>
 *     </ul>
 * </p>
 * <p>
 *     Here is a simple example of how you may use this class:
 *     <pre>
 *         {@code
 *         try {
 *             Component message = MiniMessage.miniMessage().deserialize(messageRaw);
 *         } catch (ParsingException e) {
 *             source.sendMessage(new CommandExceptionBuilder(
 *                 e.detailMessage(),
 *                 e.originalText(),
 *                 e.endIndex())
 *                 .build());
 *             return;
 *         }
 *         }
 *       </pre>
 *       In this example, we are catching a {@link net.kyori.adventure.text.minimessage.ParsingException} and using
 *       the {@link CommandExceptionBuilder} to build a message that shows the user where the error occurred.
 * </p>
 */
public class CommandExceptionBuilder {

  public static final int CONTEXT_AMOUNT = 10;

  private final String detailErrorMessage;
  private final String input;
  private final int cursor;

  @Contract(pure = true)
  public CommandExceptionBuilder(@Nullable String detailErrorMessage, String input, int cursor) {
    this.detailErrorMessage = detailErrorMessage;
    this.input = input;
    this.cursor = cursor;
  }

  /**
   * Builds the command exception message with the default prefix ({@link Colors#PREFIX}).
   *
   * @return The built message
   */
  public Component build() {
    return build(Colors.PREFIX);
  }

  /**
   * Builds the command exception message with the given prefix.
   *
   * @param prefix The prefix to add to the message
   * @return The built message
   */
  public Component build(@Nullable Component prefix) {
    final TextComponent.Builder builder = Component.text();
    final Component context = getContext();

    if (prefix != null) {
      builder.append(prefix);
    }

    if (detailErrorMessage != null) {
      builder.append(Component.text(detailErrorMessage, Colors.WARNING));

      builder.appendNewline();
      if (prefix != null) {
        builder.append(prefix);
      }
    }

    if (context != null) {
      builder.append(Component.text("At position " + cursor + ": ", Colors.ERROR));
      builder.append(context);
    }

    return builder.build();
  }

  /**
   * Gets the context of the error.
   *
   * @return The context of the error
   */
  protected @Nullable Component getContext() {
    if (input == null || cursor < 0) {
      return null;
    }

    final TextComponent.Builder builder = Component.text();
    final int cursor = Math.min(input.length(), this.cursor);
    final int start = Math.max(0, cursor - CONTEXT_AMOUNT);

    if (cursor > CONTEXT_AMOUNT) {
      builder.append(Component.text("...", Colors.ERROR));
    }

    for (int i = start; i < cursor; i++) {
      builder.append(Component.text(input.charAt(i), Colors.ERROR, TextDecoration.UNDERLINED));
    }

    builder.append(Component.translatable("command.context.here", Colors.ERROR));

    return builder.build();
  }
}
