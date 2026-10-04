package dev.slne.surf.api.minestom.server.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Parses the JSON of a {@code minestom-plugin.json}, since no JSON library is on the class path
 * while the bootstrap runs.
 *
 * <p>Objects become {@link Map Maps}, arrays {@link List Lists}, numbers {@link Double Doubles}.</p>
 */
@NullMarked
final class MiniJson {

    private final String text;
    private int position;

    private MiniJson(final String text) {
        this.text = text;
    }

    static @Nullable Object parse(final String text) {
        final MiniJson parser = new MiniJson(text);
        final @Nullable Object value = parser.value();
        parser.skipWhitespace();
        if (parser.position != text.length()) {
            throw parser.error("Unexpected content after the value");
        }
        return value;
    }

    private @Nullable Object value() {
        this.skipWhitespace();
        if (this.position >= this.text.length()) {
            throw this.error("Unexpected end of input");
        }

        return switch (this.text.charAt(this.position)) {
            case '{' -> this.object();
            case '[' -> this.array();
            case '"' -> this.string();
            case 't' -> this.literal("true", Boolean.TRUE);
            case 'f' -> this.literal("false", Boolean.FALSE);
            case 'n' -> this.literal("null", null);
            default -> this.number();
        };
    }

    private Map<String, @Nullable Object> object() {
        final Map<String, @Nullable Object> result = new LinkedHashMap<>();
        this.position++;
        this.skipWhitespace();
        if (this.peek() == '}') {
            this.position++;
            return result;
        }

        while (true) {
            this.skipWhitespace();
            final String key = this.string();
            this.skipWhitespace();
            this.expect(':');
            result.put(key, this.value());
            this.skipWhitespace();
            if (this.peek() == ',') {
                this.position++;
                continue;
            }
            this.expect('}');
            return result;
        }
    }

    private List<@Nullable Object> array() {
        final List<@Nullable Object> result = new ArrayList<>();
        this.position++;
        this.skipWhitespace();
        if (this.peek() == ']') {
            this.position++;
            return result;
        }

        while (true) {
            result.add(this.value());
            this.skipWhitespace();
            if (this.peek() == ',') {
                this.position++;
                continue;
            }
            this.expect(']');
            return result;
        }
    }

    private String string() {
        this.expect('"');
        final StringBuilder result = new StringBuilder();
        while (true) {
            if (this.position >= this.text.length()) {
                throw this.error("Unterminated string");
            }
            final char c = this.text.charAt(this.position++);
            if (c == '"') {
                return result.toString();
            }
            if (c != '\\') {
                result.append(c);
                continue;
            }

            final char escaped = this.text.charAt(this.position++);
            switch (escaped) {
                case 'b' -> result.append('\b');
                case 'f' -> result.append('\f');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
                case 'u' -> {
                    result.append((char) Integer.parseInt(this.text.substring(this.position, this.position + 4), 16));
                    this.position += 4;
                }
                default -> result.append(escaped);
            }
        }
    }

    private Double number() {
        final int start = this.position;
        while (this.position < this.text.length() && "+-0123456789.eE".indexOf(this.text.charAt(this.position)) >= 0) {
            this.position++;
        }
        if (start == this.position) {
            throw this.error("Unexpected character '" + this.text.charAt(start) + "'");
        }
        return Double.parseDouble(this.text.substring(start, this.position));
    }

    private @Nullable Object literal(final String literal, final @Nullable Object value) {
        if (!this.text.startsWith(literal, this.position)) {
            throw this.error("Expected " + literal);
        }
        this.position += literal.length();
        return value;
    }

    private char peek() {
        return this.position < this.text.length() ? this.text.charAt(this.position) : '\0';
    }

    private void expect(final char c) {
        if (this.peek() != c) {
            throw this.error("Expected '" + c + "'");
        }
        this.position++;
    }

    private void skipWhitespace() {
        while (this.position < this.text.length() && Character.isWhitespace(this.text.charAt(this.position))) {
            this.position++;
        }
    }

    private IllegalArgumentException error(final String message) {
        return new IllegalArgumentException(message + " at position " + this.position);
    }
}
