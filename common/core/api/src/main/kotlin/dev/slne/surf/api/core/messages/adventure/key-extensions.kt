package dev.slne.surf.api.core.messages.adventure

import net.kyori.adventure.key.InvalidKeyException
import net.kyori.adventure.key.Key
import net.kyori.adventure.key.KeyPattern
import net.kyori.adventure.key.Namespaced

/**
 * Creates a [Key] from a string, using `:` as the default separator.
 *
 * @param string The key string, for example, `"my_plugin:translations"`.
 * @return A [Key] parsed from the string.
 * @throws InvalidKeyException if the namespace or value contains invalid characters.
 */
fun key(@KeyPattern string: String): Key = Key.key(string)

/**
 * Creates a [Key] from a string using a custom separator.
 *
 * @param string The key string, for example, `"my_plugin.translations"`.
 * @param separator The character separating the namespace from the value.
 * @return A [Key] parsed from the string.
 * @throws InvalidKeyException if the namespace or value contains invalid characters.
 */
fun key(@KeyPattern string: String, separator: Char): Key = Key.key(string, separator)

/**
 * Creates a [Key] using a namespace and a value.
 *
 * @param namespace The namespace, for example, `"my_plugin"`.
 * @param value The key value, for example, `"translations"`.
 * @return A [Key] with the given namespace and value.
 * @throws InvalidKeyException if the namespace or value contains invalid characters.
 */
fun key(namespace: String, value: String): Key = Key.key(namespace, value)

/**
 * Creates a [Key] from a [Namespaced] object and a value.
 *
 * @param namespaced The [Namespaced] object providing the namespace.
 * @param value The key value, for example, `"translations"`.
 * @return A [Key] with the namespace from [namespaced] and the given value.
 * @throws InvalidKeyException if the namespace or value contains invalid characters.
 */
fun key(namespaced: Namespaced, value: String): Key = Key.key(namespaced, value)

/**
 * Converts this string into a [Key] using `:` as the default separator.
 *
 * @receiver The key string, for example, `"my_plugin:translations"`.
 * @return A [Key] parsed from the string.
 * @throws InvalidKeyException if the namespace or value contains invalid characters.
 */
fun String.toKey(): Key = Key.key(this)

/**
 * Converts this string into a [Key] using a custom separator.
 *
 * @receiver The key string, for example, `"my_plugin.translations"`.
 * @param separator The character separating the namespace from the value.
 * @return A [Key] parsed from the string.
 * @throws InvalidKeyException if the namespace or value contains invalid characters.
 */
fun String.toKey(separator: Char): Key = Key.key(this, separator)

/**
 * Converts a [Pair] of namespace and value into a [Key].
 *
 * @receiver A [Pair] where the first element is the namespace and the second is the value.
 * @return A [Key] constructed from the namespace and value.
 * @throws InvalidKeyException if the namespace or value contains invalid characters.
 */
fun Pair<String, String>.toKey(): Key = Key.key(first, second)

/**
 * Converts a [Namespaced] object and a value into a [Key].
 *
 * @receiver A [Namespaced] object providing the namespace.
 * @param value The key value, for example, `"translations"`.
 * @return A [Key] with the namespace from the [Namespaced] object and the given value.
 * @throws InvalidKeyException if the namespace or value contains invalid characters.
 */
fun Namespaced.toKey(value: String): Key = Key.key(this, value)

/**
 * Returns the minimal string representation of this [Key].
 *
 * If the namespace is `"minecraft"`, only the value is returned.
 *
 * @receiver The [Key] to convert.
 * @return The minimal string representation of this key.
 */
fun Key.asMinimal(): String = this.asMinimalString()

/**
 * Returns the full string representation of this [Key].
 *
 * @receiver The [Key] to convert.
 * @return The full string representation, including namespace and value.
 */
fun Key.asFull(): String = this.asString()

/**
 * Creates a [Key] using this string as the namespace and the provided value.
 *
 * Example: `"my_plugin" withKey "translations"` → `Key("my_plugin", "translations")`
 *
 * @receiver The namespace string.
 * @param value The key value.
 * @return A [Key] with the given namespace and value.
 * @throws InvalidKeyException if the namespace or value contains invalid characters.
 */
infix fun String.withKey(value: String): Key = Key.key(this, value)

/**
 * Creates a [Key] using this string as the namespace and the provided value.
 *
 * Example: `"my_plugin" defines "translations"` → `Key("my_plugin", "translations")`
 *
 * @receiver The namespace string.
 * @param value The key value.
 * @return A [Key] with the given namespace and value.
 * @throws InvalidKeyException if the namespace or value contains invalid characters.
 */
infix fun String.defines(value: String): Key = Key.key(this, value)

/**
 * Creates a [Key] using this string as the namespace and the provided value.
 *
 * Example: `"my_plugin" named "translations"` → `Key("my_plugin", "translations")`
 *
 * @receiver The namespace string.
 * @param value The key value.
 * @return A [Key] with the given namespace and value.
 * @throws InvalidKeyException if the namespace or value contains invalid characters.
 */
infix fun String.named(value: String): Key = Key.key(this, value)

/**
 * Creates a [Key] using this [Namespaced] object as the namespace and the provided value.
 *
 * Example: `someNamespacedInstance withKey "translations"`
 *
 * @receiver The [Namespaced] object.
 * @param value The key value.
 * @return A [Key] with the given namespace and value.
 * @throws InvalidKeyException if the namespace or value contains invalid characters.
 */
infix fun Namespaced.withKey(value: String): Key = Key.key(this, value)

/**
 * Creates a [Key] using this [Namespaced] object as the namespace and the provided value.
 *
 * Example: `someNamespacedInstance defines "translations"`
 *
 * @receiver The [Namespaced] object.
 * @param value The key value.
 * @return A [Key] with the given namespace and value.
 * @throws InvalidKeyException if the namespace or value contains invalid characters.
 */
infix fun Namespaced.defines(value: String): Key = Key.key(this, value)

/**
 * Creates a [Key] using this [Namespaced] object as the namespace and the provided value.
 *
 * Example: `someNamespacedInstance named "translations"`
 *
 * @receiver The [Namespaced] object.
 * @param value The key value.
 * @return A [Key] with the given namespace and value.
 * @throws InvalidKeyException if the namespace or value contains invalid characters.
 */
infix fun Namespaced.named(value: String): Key = Key.key(this, value)
