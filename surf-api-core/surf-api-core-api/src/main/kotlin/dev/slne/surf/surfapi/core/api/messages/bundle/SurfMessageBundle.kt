package dev.slne.surf.surfapi.core.api.messages.bundle

import dev.slne.surf.surfapi.core.api.messages.BundlePath
import dev.slne.surf.surfapi.core.api.messages.adventure.key
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import dev.slne.surf.surfapi.core.api.util.toObjectSet
import it.unimi.dsi.fastutil.objects.ObjectList
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.TranslationRegistry
import net.kyori.adventure.util.UTF8ResourceBundleControl
import org.jetbrains.annotations.NonNls
import java.net.URLClassLoader
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.*
import java.util.function.Supplier
import kotlin.io.path.*

/**
 * A class for managing and loading message bundles used for translations in a plugin environment.
 *
 * This class provides functionality to:
 * - Load resource bundles from both the classpath and the plugin's data folder.
 * - Copy missing resource bundles from the classpath to the data folder.
 * - Update resource bundles in the data folder with missing keys from the bundled resources.
 * - Register all loaded bundles with the global Adventure translator.
 * - Provide utilities to fetch messages in a translatable format using keys.
 *
 * The [SurfMessageBundle] ensures that translations are updated and available for use across
 * the application by leveraging the Adventure library's translation capabilities.
 *
 * ### Optimal Usage Example
 * ```
 * // Create an object wrapper for the message bundle
 * object MessageBundleExample {
 *     // Define a constant for the bundle's base name
 *     private const val BUNDLE = "messages.ExampleBundle"
 *     private val bundle = SurfMessageBundle(javaClass, BUNDLE, plugin.dataPath).apply { load() }
 *
 *     // Retrieve a translatable message
 *     fun getMessage(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Component) = bundle.getMessage(key, *params)
 *
 *     // Retrieve a lazily-evaluated translatable message
 *     fun lazyMessage(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Component) = bundle.lazyMessage(key, *params)
 * }
 *
 * // Example usage
 * fun main() {
 *     val message = MessageBundleExample.getMessage("example.key")
 *     println(message)
 * }
 * ```
 * ### Basic Usage Example
 * ```
 * // Define the path to the message bundle
 * private const val BUNDLE = "messages.ExampleBundle"
 *
 * // Create an instance of SurfMessageBundle and load it
 * val bundle = SurfMessageBundle(javaClass, BUNDLE, plugin.dataPath).apply { load() }
 *
 * // Retrieve a translatable message
 * val message = bundle.getMessage("example.key")
 *
 * // Use the message in your application
 * println(message)
 * ```
 *
 * @property bundleClazz The class used to locate the resource bundles. Typically, this is the class where the
 * resource files are packaged or loaded.
 * @property pathToBundle The relative path to the base name of the resource bundle files, excluding the file extension.
 * For example, if the bundle is located at `messages/example.properties`, the path would be `messages.example`.
 * @property dataFolder The directory where the plugin stores its data, including resource bundles. This is used to
 * store and manage localized message files.
 * @property classLoader The class loader used to load resource bundles from the classpath. Defaults to the class loader
 * of [bundleClazz].
 * @constructor Creates a new instance of [SurfMessageBundle].
 *
 * @param bundleClazz The class used for locating the resource bundles.
 * @param pathToBundle The relative path to the bundle files, excluding the file extension.
 * @param dataFolder The directory used for storing and managing resource bundles.
 * @param classLoader The class loader used to load bundled resources. Defaults to the class loader of [bundleClazz].
 */
class SurfMessageBundle @JvmOverloads constructor(
    val bundleClazz: Class<*>,
    val pathToBundle: @BundlePath @NonNls String,
    val dataFolder: Path,
    val classLoader: ClassLoader = bundleClazz.classLoader,
) {
    private val bundleDir =
        dataFolder.resolve(pathToBundle.substringBeforeLast('.', "").replace('.', '/'))

    fun load() {
        // Ensure data folder exists
        dataFolder.createDirectories()

        // Load bundled and external bundles
        val bundled = loadBundledBundles()
        val external = loadExternalBundles()

        // Copy missing bundle files
        copyMissingBundles(bundled, external)
        // Update existing files with missing keys
        syncMissingKeys(bundled.associateBy { it.name }, external)
        // Register all external bundles for translation
        registerBundlesWithTranslator(external)
    }

    private fun loadBundledBundles() =
        Locale.getAvailableLocales().mapNotNullTo(mutableObjectListOf()) { locale ->
            val name = UTF8ResourceBundleControl.get().toBundleName(pathToBundle, locale)
            try {
                val bundle = ResourceBundle.getBundle(
                    pathToBundle,
                    locale,
                    classLoader,
                    UTF8ResourceBundleControl.get()
                )
                LoadedBundle(name, locale, bundle)
            } catch (_: MissingResourceException) {
                null
            }
        }

    private fun loadExternalBundles(): ObjectList<LoadedBundle> {
        if (dataFolder.notExists()) return mutableObjectListOf()
        return dataFolder.walk(PathWalkOption.FOLLOW_LINKS)
            .filter {
                it.extension == "properties"
                        && it.name.startsWith(pathToBundle.substringAfterLast('.'))
            }
            .mapNotNull { path ->
                val name = dataFolder.relativize(path)
                    .toString()
                    .replace(FileSystems.getDefault().separator, ".")
                    .substringBeforeLast('.')
                val localeTag = name.substringAfterLast('_', "").replace('_', '-')
                val locale = Locale.forLanguageTag(localeTag)
                try {
                    val loader = URLClassLoader(arrayOf(dataFolder.toUri().toURL()))
                    val bundle = ResourceBundle.getBundle(
                        name, locale, loader, UTF8ResourceBundleControl.get()
                    )
                    LoadedBundle(name, locale, bundle)
                } catch (_: Throwable) {
                    null
                }
            }.toCollection(mutableObjectListOf())
    }

    private fun copyMissingBundles(
        bundled: ObjectList<LoadedBundle>,
        external: ObjectList<LoadedBundle>,
    ) {
        val existing = external.map { it.name }.toObjectSet()
        bundled.filter { it.name !in existing }.forEach { bundle ->
            writeProperties(
                bundle.name,
                bundle.bundle.toProperties(),
                header = "Generated by SurfAPI"
            )
            external += bundle
        }
    }

    private fun syncMissingKeys(
        bundledMap: Map<String, LoadedBundle>,
        external: List<LoadedBundle>,
    ) {
        external.forEach { ext ->
            val src = bundledMap[ext.name] ?: return@forEach
            val props = Properties().apply { putAll(ext.bundle.toProperties()) }
            src.bundle.keys.asSequence()
                .filter { it !in props }
                .forEach { props[it] = src.bundle.getString(it) }
            writeProperties(ext.name, props)
        }
    }

    private fun registerBundlesWithTranslator(bundles: List<LoadedBundle>) {
        val registry = TranslationRegistry.create(
            key(
                "surf",
                "bundle-${pathToBundle.substringAfterLast('.')}"
            )
        ).apply { defaultLocale(Locale.getDefault()) }
        bundles.forEach { b -> registry.registerAll(b.locale, b.bundle, true) }
        GlobalTranslator.translator().addSource(registry)
    }


    private fun writeProperties(
        name: String,
        props: Properties,
        header: String? = null,
    ) {
        dataFolder.createDirectories()
        val file = dataFolder.resolve("$name.properties")
        file.outputStream().use { props.store(it, header) }
    }

    private data class LoadedBundle(
        val name: String,
        val locale: Locale,
        val bundle: ResourceBundle,
    )

    private fun ResourceBundle.toProperties(): Properties {
        val p = Properties()
        keys.asSequence().forEach { k -> p[k] = getString(k) }
        return p
    }

    /**
     * Retrieves a translatable message as a [TranslatableComponent].
     *
     * @param key The key of the message in the resource bundle.
     * @param params Optional parameters to format the message.
     * @return The message as a translatable [TranslatableComponent].
     */
    fun getMessage(key: String, vararg params: Component) = Component.translatable(key, *params)

    /**
     * Retrieves a lazily-evaluated message supplier as a [TranslatableComponent].
     *
     * @param key The key of the message in the resource bundle.
     * @param params Optional parameters to format the message.
     * @return A [Supplier] that provides the message as a translatable [TranslatableComponent].
     */
    fun lazyMessage(key: String, vararg params: Component) = Supplier { getMessage(key, *params) }

    /**
     * Operator function for retrieving a translatable message as a [TranslatableComponent].
     *
     * @param key The key of the message in the resource bundle.
     * @param params Optional parameters to format the message.
     * @return The message as a translatable [TranslatableComponent].
     */
    operator fun get(key: String, vararg params: Component) = getMessage(key, *params)
}