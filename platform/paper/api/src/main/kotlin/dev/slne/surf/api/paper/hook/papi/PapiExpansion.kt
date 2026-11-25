package dev.slne.surf.api.paper.hook.papi

import dev.slne.surf.api.spring.ConditionalOnPlugin
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

/**
 * Declares a PlaceholderAPI expansion whose placeholders are the [Placeholder] methods of the
 * annotated bean and of the classes in [placeholders].
 *
 * The annotated class is a component of the plugin's context that only exists while PlaceholderAPI
 * is installed. surf-api registers the expansion when the plugin enables, before its `onEnable`
 * hooks run, and unregisters it when the plugin disables. A request `%<identifier>_<name>_<args>%`
 * calls the placeholder `<name>` with `<args>`; `%<identifier>_%` and unknown names go to
 * [PapiExpansionFallback] when the annotated class implements it, and resolve to `null`
 * otherwise. PlaceholderAPI matches identifiers case-insensitively.
 *
 * ```kotlin
 * @PapiExpansion("coins")
 * class CoinPlaceholders(private val coins: CoinService) {
 *
 *     @Placeholder("balance")
 *     fun balance(player: OfflinePlayer?): String? = player?.let { coins.balance(it).toString() }
 *
 *     @Placeholder("rank")
 *     fun rank(player: Player, args: String): Component = coins.rankBadge(player, args)
 * }
 * ```
 *
 * @property identifier the expansion's identifier, the first segment of every placeholder; it must
 * not be empty or contain `%`, `{`, `}` or `_`
 * @property author the expansion's author; blank uses the authors of the owning plugin's
 * `paper-plugin.yml`
 * @property version the expansion's version; blank uses the owning plugin's version
 * @property placeholders further classes declaring [Placeholder] methods. A Kotlin `object`
 * contributes its instance, and a class with a bean in the plugin's context contributes that bean;
 * several matching beans fail the registration. Any other class is instantiated through the
 * context, with its constructor parameters injected, and destroyed when the plugin disables.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Component
@ConditionalOnPlugin("PlaceholderAPI")
annotation class PapiExpansion(
    val identifier: String,
    val author: String = "",
    val version: String = "",
    val placeholders: Array<KClass<*>> = [],
)
