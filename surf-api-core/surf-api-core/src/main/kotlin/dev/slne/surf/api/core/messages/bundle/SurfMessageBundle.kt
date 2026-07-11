package dev.slne.surf.api.core.messages.bundle

import dev.slne.surf.api.core.messages.BundlePath
import dev.slne.surf.api.core.util.mutableObjectListOf
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.TranslationStore
import net.kyori.adventure.translation.Translator
import org.jetbrains.annotations.NonNls
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.*
import java.util.function.Supplier
import kotlin.io.path.*

private val AVAILABLE_BUNDLE_LOCALES = Locale.getAvailableLocales()

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
    private val bundlePath = dataFolder.resolve(pathToBundle.replace('.', '/'))
    private val bundleFolder = requireNotNull(bundlePath.parent) {
        "Bundle path must have a parent: $pathToBundle"
    }
    private var registeredStore: Translator? = null

    /**
     * Loads and updates resource bundles from both the classpath and the data folder.
     * This method ensures that missing resource bundles and keys are added to the data folder,
     * and all bundles are registered with the global translator.
     */
    @Synchronized
    fun load() {
        bundleFolder.createDirectories()

        // get all resource bundles that are bundled with the plugin
        val bundledResourceBundles = getBundledBundles(pathToBundle, classLoader)

        // get all resource bundles that are in the plugin's data folder
        val dataFolderResourceBundles = getBundles(pathToBundle)

        // update the resource bundles
        // Step 1: Copy all bundled resource bundles that aren't in the data folder
        copyMissingBundles(bundledResourceBundles, dataFolderResourceBundles)

        // Step 2: Add all missing keys to the data folder resource and update them
        updateResourceBundles(bundledResourceBundles, dataFolderResourceBundles)

        // Step 3: Add all resource bundles to the translator
        addBundlesToTranslator(pathToBundle, getBundles(pathToBundle))
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

    /**
     * Adds the provided resource bundles to the global translator for use in message translation.
     *
     * @param baseName The base name of the resource bundle.
     * @param bundles The list of resource bundles to be added.
     */
    private fun addBundlesToTranslator(
        baseName: String,
        bundles: List<ResourceBundle>,
    ) {
        val store = TranslationStore.messageFormat(
            Key.key(
                "surf",
                "bundle-${baseName.substringAfterLast('.').lowercase()}"
            )
        )

        store.defaultLocale(Locale.getDefault())
        for (bundle in bundles) {
            store.registerAll(bundle.locale, bundle, true)
        }

        val globalTranslator = GlobalTranslator.translator()
        registeredStore?.let(globalTranslator::removeSource)
        globalTranslator.addSource(store)
        registeredStore = store
    }

    /**
     * Updates resource bundles in the data folder by copying missing keys from the bundled resources.
     *
     * @param bundledResourceBundles The list of bundled resource bundles.
     * @param dataFolderResourceBundles The list of resource bundles in the data folder.
     */
    private fun updateResourceBundles(
        bundledResourceBundles: List<ResourceBundle>,
        dataFolderResourceBundles: List<ResourceBundle>,
    ) {
        val bundledResourceBundlesMap = bundledResourceBundles.associateBy { it.getBundleName() }

        for (bundle in dataFolderResourceBundles) {
            val key = bundle.getBundleName()
            val bundledBundle = bundledResourceBundlesMap[key] ?: continue
            updateBundleKeys(bundledBundle, bundle)
        }
    }


    private fun updateBundleKeys(
        from: ResourceBundle,
        to: ResourceBundle,
    ) {
        val toKeys = to.keys.asSequence().toSet()
        val missingKeys = mutableObjectListOf<String>()
        val fromKeys = from.keys
        while (fromKeys.hasMoreElements()) {
            val key = fromKeys.nextElement()
            if (key !in toKeys) {
                missingKeys.add(key)
            }
        }
        if (missingKeys.isEmpty()) return

        val properties = Properties()
        for (key in toKeys) {
            properties[key] = to.getString(key)
        }

        for (key in missingKeys) {
            properties[key] = from.getString(key)
        }

        val target = bundleFile(to)
        target.parent.createDirectories()
        target.outputStream().use { properties.store(it, null) }
    }

    /**
     * Copies missing resource bundles from the bundled resources to the data folder.
     *
     * @param bundledResourceBundles The list of bundled resource bundles.
     * @param dataFolderResourceBundles The mutable list of resource bundles in the data folder to be updated.
     */
    private fun copyMissingBundles(
        bundledResourceBundles: List<ResourceBundle>,
        dataFolderResourceBundles: MutableList<ResourceBundle>,
    ) {
        val dataFolderBundleNames = dataFolderResourceBundles
            .mapTo(HashSet(dataFolderResourceBundles.size)) { it.getBundleName() }

        for (bundle in bundledResourceBundles) {
            if (!dataFolderBundleNames.add(bundle.getBundleName())) continue
            copyBundleToDataFolder(bundle)
            dataFolderResourceBundles.add(bundle)
        }
    }

    /**
     * Copies a single resource bundle to the data folder.
     *
     * @param bundle The resource bundle to be copied.
     */
    private fun copyBundleToDataFolder(bundle: ResourceBundle) {
        val target = bundleFile(bundle)
        target.parent.createDirectories()

        val properties = Properties()
        for (key in bundle.keys.asSequence()) {
            properties[key] = bundle.getString(key)
        }

        target.outputStream().use { properties.store(it, "Generated by SurfAPI") }
    }

    /**
     * Retrieves resource bundles from the data folder based on the given prefix and default locale.
     *
     * @param prefix The prefix of the bundle file names.
     * @param defaultLocale The default locale to be used if none is specified in the file names.
     * @return A list of resource bundles found in the data folder.
     */
    private fun getBundles(baseName: String): MutableList<ResourceBundle> {
        if (bundleFolder.notExists()) return mutableObjectListOf()

        val filePrefix = bundlePath.name
        return URLClassLoader(arrayOf(dataFolder.toUri().toURL())).use { dataClassLoader ->
            bundleFolder.listDirectoryEntries("$filePrefix*.properties")
                .asSequence()
                .filter { path ->
                    val stem = path.name.removeSuffix(".properties")
                    stem == filePrefix || stem.startsWith("${filePrefix}_")
                }
                .mapNotNullTo(mutableObjectListOf()) { path ->
                    val locale = path.name.toBundleLocale(filePrefix)
                    runCatching {
                        ResourceBundle.getBundle(baseName, locale, dataClassLoader)
                    }.getOrNull()
                }
        }
    }


    /**
     * Retrieves resource bundles that are bundled with the plugin, filtering by available locales.
     *
     * @param baseName The base name of the resource bundle.
     * @param classLoader The class loader to use for resource loading.
     * @return A list of bundled resource bundles.
     */
    private fun getBundledBundles(
        baseName: String,
        classLoader: ClassLoader,
    ): MutableList<ResourceBundle> {
        val result = mutableObjectListOf<ResourceBundle>()

        fun addIfAvailable(locale: Locale) {
            if (!isResourceBundleAvailable(baseName, locale, classLoader)) return
            try {
                result.add(ResourceBundle.getBundle(baseName, locale, classLoader))
            } catch (_: MissingResourceException) {
                // The availability check and actual load may observe different classloader state.
            }
        }

        addIfAvailable(Locale.ROOT)
        for (locale in AVAILABLE_BUNDLE_LOCALES) {
            if (locale != Locale.ROOT) {
                addIfAvailable(locale)
            }
        }
        return result
    }

    /**
     * Checks if a resource bundle is available for the given base name and locale.
     *
     * @param baseName The base name of the resource bundle.
     * @param locale The locale to check.
     * @param classLoader The class loader to use for resource loading.
     * @return `true` if the resource bundle is available; otherwise, `false`.
     */
    private fun isResourceBundleAvailable(
        baseName: String,
        locale: Locale,
        classLoader: ClassLoader,
    ): Boolean {
        val bundleName = baseName.toBundleName(locale)
        val resourceName = bundleName.toResourceName("properties")

        return try {
            classLoader.getResourceAsStream(resourceName).use { it != null }
        } catch (_: Throwable) {
            false
        }
    }

    private fun String.toBundleLocale(filePrefix: String): Locale {
        val localeSuffix = removeSuffix(".properties")
            .removePrefix(filePrefix)
            .removePrefix("_")
        if (localeSuffix.isEmpty()) return Locale.ROOT

        val parts = localeSuffix.split('_')

        return when (parts.size) {
            1 -> Locale.of(parts[0])
            2 -> Locale.of(parts[0], parts[1])
            3 -> Locale.of(parts[0], parts[1], parts[2])
            else -> Locale.ROOT
        }
    }

    private fun String.toBundleName(locale: Locale): String {
        if (locale == Locale.ROOT) {
            return this
        }

        return buildString {
            append(this@toBundleName)

            val language = locale.language
            val country = locale.country
            val variant = locale.variant

            if (language.isNotEmpty()) {
                append('_')
                append(language)
            }

            if (country.isNotEmpty()) {
                append('_')
                append(country)
            }

            if (variant.isNotEmpty()) {
                append('_')
                append(variant)
            }
        }
    }

    private fun ResourceBundle.getBundleName() = buildString {
        append(baseBundleName)
        if (locale != Locale.ROOT) {
            append('_')
            append(locale)
        }
    }

    private fun bundleFile(bundle: ResourceBundle): Path =
        dataFolder.resolve(bundle.getBundleName().replace('.', '/') + ".properties")

    private fun String.toResourceName(suffix: String) = replace('.', '/') + ".$suffix"

}
