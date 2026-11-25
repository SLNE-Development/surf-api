package dev.slne.surf.api.paper.server.hook.papi

import dev.slne.surf.api.paper.hook.papi.PapiExpansionFallback
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.OfflinePlayer

/**
 * Resolves the requests of an expansion declared with
 * [dev.slne.surf.api.paper.hook.papi.PapiExpansion].
 *
 * `<name>_<args>` calls the invoker of `<name>` with `<args>`; an empty request or an unknown name
 * goes to [fallback]. Immutable after construction.
 *
 * @param placeholders the invokers keyed by placeholder name
 * @param fallback resolves the requests no invoker handles; `null` resolves them to `null`
 */
internal class AnnotatedExpansionResolver(
    placeholders: Map<String, PlaceholderInvoker>,
    private val fallback: PapiExpansionFallback?,
) : PlaceholderResolver {

    private val placeholders = HashMap(placeholders)

    override fun resolve(player: OfflinePlayer?, params: String): String? {
        val invoker = placeholders[params.substringBefore('_')]
            ?: return fallback?.onRequest(player, params)

        return render(invoker.invoke(player, params.substringAfter('_', "")))
    }

    private fun render(result: Any?): String? {
        return when (result) {
            null -> null
            is String -> result
            is Component -> LegacyComponentSerializer.legacySection().serialize(result)
            else -> error("A placeholder returned ${result.javaClass.name}")
        }
    }
}
