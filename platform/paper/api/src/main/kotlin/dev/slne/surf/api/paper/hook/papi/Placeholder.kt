package dev.slne.surf.api.paper.hook.papi

/**
 * Marks a method that resolves the placeholder [value] of a [PapiExpansion].
 *
 * The method takes the requesting player as its first parameter and optionally the arguments as a
 * second `String` parameter, and returns `String?` or a `Component?`:
 *
 * - `OfflinePlayer?` receives every request, including those without a player, as `null`.
 * - A non-null Kotlin `OfflinePlayer` receives only requests with a player; a request without one
 *   resolves to `null` without calling the method and without an error.
 * - `Player` receives only requests for an online player; any other request resolves to `null` the
 *   same way.
 * - The arguments are the rest of the request after `<name>_`, unsplit, and empty without one.
 *
 * A `Component` is serialized to legacy section-sign text, which PlaceholderAPI passes on. The
 * method must not be `suspend`. PlaceholderAPI calls it on the thread that resolves the
 * placeholder, which may be any thread, so it must be thread-safe.
 *
 * @property value the placeholder's name; it must not be empty or contain `%`, `{`, `}` or `_`
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Placeholder(val value: String)
