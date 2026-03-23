package dev.slne.surf.surfapi.shared.api.build

/**
 * Marks a data class for automatic Builder generation by the Surf Builder compiler plugin.
 *
 * The compiler plugin will generate:
 * - A nested `Builder` class with setter methods for each constructor parameter
 * - Required parameter validation in the `build()` method
 * - A top-level DSL function `ClassName { ... }` for convenient construction
 * - For `List<T>` parameters: `addXxx()` methods, and DSL blocks if `T` is also `@GenerateBuilder`
 * - For `Set<T>` parameters: `addXxx()` methods
 * - For `Map<K,V>` parameters: `putXxx()` methods
 *
 * Example:
 * ```kotlin
 * @GenerateBuilder
 * data class Person(
 *     val name: String,                        // required
 *     val lastName: String,                     // required
 *     val phoneNumber: String? = null,          // optional
 *     val friends: List<Person> = emptyList()   // collection with DSL support
 * )
 *
 * // Usage:
 * val person = Person {
 *     name("Max")
 *     lastName("Mustermann")
 *     phoneNumber("0123456789")
 *     friend {
 *         name("Anna")
 *         lastName("Schmidt")
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class GenerateBuilder
