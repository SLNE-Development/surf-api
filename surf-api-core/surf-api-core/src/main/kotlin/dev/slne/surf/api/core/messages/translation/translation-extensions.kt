package dev.slne.surf.api.core.messages.translation

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import java.util.*

/**
 * Regex pattern matching `{placeholderName}` tokens in translated strings.
 * Does not match `{reference:...}` tokens (those are resolved by the [TranslationProcessor]).
 */
private val PLACEHOLDER_REGEX = Regex("""\{([^}:]+)}""")

/**
 * Translates the given [key] from the translation store identified by [storeKey] and replaces
 * placeholders with the provided [Component] [replacements].
 *
 * Placeholder tokens in the translated string use the format `{name}`, where `name` corresponds
 * to a key in the [replacements] map. Placeholders that have no matching replacement are kept
 * as-is in the resulting component.
 *
 * If the translation key or store is not found, a warning is logged and the raw [key] is returned
 * as a text component.
 *
 * ### Example
 * ```kotlin
 * val message = translate(
 *     storeKey = Key.key("surf", "messages"),
 *     key = "welcome.message",
 *     replacements = mapOf(
 *         "player" to Component.text("Steve", NamedTextColor.GREEN),
 *         "server" to Component.text("Lobby", NamedTextColor.GOLD)
 *     )
 * )
 * // Template: "Welcome {player} to {server}!"
 * // Result:   Component["Welcome ", <green>Steve</green>, " to ", <gold>Lobby</gold>, "!"]
 * ```
 *
 * @param storeKey The [Key] identifying the translation store.
 * @param key The translation key to look up.
 * @param replacements A map of placeholder names to [Component] values that replace `{name}` tokens.
 * @param locale The locale to use for translation. Defaults to the system default locale.
 * @return A [Component] with the translated text and component replacements applied.
 */
fun translate(
    storeKey: Key,
    key: String,
    replacements: Map<String, Component> = emptyMap(),
    locale: Locale = Locale.getDefault(),
): Component {
    val translated = TranslationService.translate(storeKey, locale, key)
        ?: return Component.text(key)

    return applyComponentReplacements(translated, replacements)
}

/**
 * Translates the given [key] from the translation store identified by this [Key] and replaces
 * placeholders with the provided [Component] [replacements].
 *
 * This is a convenience extension on [Key] for a more fluent API:
 * ```kotlin
 * val STORE = Key.key("surf", "messages")
 * val msg = STORE.translate("welcome.message", mapOf("player" to playerName))
 * ```
 *
 * @param key The translation key to look up.
 * @param replacements A map of placeholder names to [Component] values.
 * @param locale The locale to use for translation. Defaults to the system default locale.
 * @return A [Component] with the translated text and component replacements applied.
 * @see translate
 */
fun Key.translate(
    key: String,
    replacements: Map<String, Component> = emptyMap(),
    locale: Locale = Locale.getDefault(),
): Component = translate(storeKey = this, key = key, replacements = replacements, locale = locale)

/**
 * Translates a list of entries from the translation store identified by [storeKey].
 *
 * This looks up keys `{key}.0`, `{key}.1`, ... `{key}.N` and replaces placeholders with the
 * provided [Component] [replacements] in each entry.
 *
 * @param storeKey The [Key] identifying the translation store.
 * @param key The base translation key for the list.
 * @param replacements A map of placeholder names to [Component] values.
 * @param locale The locale to use for translation. Defaults to the system default locale.
 * @return A list of [Component]s with translations and replacements applied.
 */
fun translateList(
    storeKey: Key,
    key: String,
    replacements: Map<String, Component> = emptyMap(),
    locale: Locale = Locale.getDefault(),
): List<Component> = TranslationService.translateList(storeKey, locale, key)
    .map { applyComponentReplacements(it, replacements) }

/**
 * Translates a list of entries using this [Key] as the store key.
 *
 * @param key The base translation key for the list.
 * @param replacements A map of placeholder names to [Component] values.
 * @param locale The locale to use for translation. Defaults to the system default locale.
 * @return A list of [Component]s with translations and replacements applied.
 * @see translateList
 */
fun Key.translateList(
    key: String,
    replacements: Map<String, Component> = emptyMap(),
    locale: Locale = Locale.getDefault(),
): List<Component> = translateList(storeKey = this, key = key, replacements = replacements, locale = locale)

/**
 * Replaces `{placeholder}` tokens in a pre-translated string with [Component] values.
 *
 * Text segments between placeholders are converted to plain text components.
 * If [replacements] is empty, the entire string is returned as a single text component.
 *
 * @param translated The already-translated string containing `{placeholder}` tokens.
 * @param replacements A map of placeholder names to [Component] values.
 * @return A [Component] with text and component parts interleaved.
 */
private fun applyComponentReplacements(
    translated: String,
    replacements: Map<String, Component>,
): Component {
    if (replacements.isEmpty()) {
        return Component.text(translated)
    }

    val builder = Component.text()
    var lastEnd = 0
    var anyReplaced = false

    for (match in PLACEHOLDER_REGEX.findAll(translated)) {
        val replacement = replacements[match.groupValues[1]] ?: continue

        // Append text before the placeholder
        if (match.range.first > lastEnd) {
            builder.append(Component.text(translated.substring(lastEnd, match.range.first)))
        }

        builder.append(replacement)
        lastEnd = match.range.last + 1
        anyReplaced = true
    }

    if (!anyReplaced) {
        return Component.text(translated)
    }

    // Append remaining text after the last replacement
    if (lastEnd < translated.length) {
        builder.append(Component.text(translated.substring(lastEnd)))
    }

    return builder.build()
}
