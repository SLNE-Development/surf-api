package dev.slne.surf.surfapi.core.api.messages.bundle

import dev.slne.surf.surfapi.core.api.messages.BundlePath
import dev.slne.surf.surfapi.core.api.messages.adventure.key
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.http.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.TranslationRegistry
import net.kyori.adventure.util.UTF8ResourceBundleControl
import org.jetbrains.annotations.NonNls
import java.util.*
import java.util.function.Supplier

/**
 * A message bundle that loads translations from a Weblate project.
 *
 * The bundle fetches the latest translations from a remote Weblate instance
 * using Ktor and registers them with Adventure's [GlobalTranslator]. A fallback
 * bundle packaged with the plugin is used for missing keys or when remote
 * loading fails. Bundles can be reloaded at runtime to obtain updated
 * translations.
 *
 * @property bundleClazz Class used to locate the fallback resource bundle.
 * @property pathToBundle Base name of the bundle resources (e.g. `messages.Example`).
 * @property baseUrl Base URL of the Weblate download endpoint returning
 * `properties` files without the bundle name or locale, for example
 * `https://translations.example.com/api/download/example/`.
 * @property classLoader Class loader used to load the fallback bundle.
 * @property client Optional [HttpClient] instance for requests.
 */
class SurfMessageBundle @JvmOverloads constructor(
    val bundleClazz: Class<*>,
    val pathToBundle: @BundlePath @NonNls String,
    val baseUrl: String,
    val classLoader: ClassLoader = bundleClazz.classLoader,
    val client: HttpClient = HttpClient(OkHttp)
) {
    private val translatorKey = key("surf", "bundle-${pathToBundle.substringAfterLast('.')}")
    private val fallbackBundles: Map<Locale, ResourceBundle> = loadFallbackBundles()
    private var registry: TranslationRegistry? = null
    private val version: String = bundleClazz.`package`?.implementationVersion ?: "dev"

    /**
     * Loads translations from Weblate and registers them with Adventure.
     * If loading fails, only the fallback bundle is registered.
     */
    suspend fun load() {
        val remote = fetchRemoteBundles()
        val reg = TranslationRegistry.create(translatorKey).apply {
            defaultLocale(Locale.getDefault())
        }
        fallbackBundles.forEach { (locale, bundle) ->
            reg.registerAll(locale, bundle, true)
        }
        remote.forEach { (locale, bundle) ->
            reg.registerAll(locale, bundle, true)
        }
        registry?.let { GlobalTranslator.translator().removeSource(it) }
        GlobalTranslator.translator().addSource(reg)
        registry = reg
    }

    /** Reloads the translations from Weblate. */
    suspend fun reload() = load()

    private fun loadFallbackBundles(): Map<Locale, ResourceBundle> {
        val map = mutableMapOf<Locale, ResourceBundle>()
        Locale.getAvailableLocales().forEach { locale ->
            try {
                val bundle = ResourceBundle.getBundle(
                    pathToBundle,
                    locale,
                    classLoader,
                    UTF8ResourceBundleControl.get()
                )
                map[locale] = bundle
            } catch (_: MissingResourceException) {
                // ignore
            }
        }
        return map
    }

    private suspend fun fetchRemoteBundles(): Map<Locale, ResourceBundle> {
        val map = mutableMapOf<Locale, ResourceBundle>()
        for (locale in fallbackBundles.keys) {
            val code = locale.toLanguageTag()
            try {
                val text: String = client.get("$baseUrl$code.properties") {
                    parameter("version", version)
                    accept(ContentType.Text.Plain)
                }.bodyAsText()
                map[locale] = PropertyResourceBundle(text.byteInputStream())
            } catch (_: Exception) {
                // ignore individual failures
            }
        }
        return map
    }

    fun getMessage(key: String, vararg params: Component) =
        Component.translatable(key, *params)

    fun lazyMessage(key: String, vararg params: Component) =
        Supplier { getMessage(key, *params) }

    operator fun get(key: String, vararg params: Component) =
        getMessage(key, *params)
}
