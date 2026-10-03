package dev.slne.surf.api.paper.bedrock.form

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.geysermc.cumulus.form.Form
import org.geysermc.cumulus.form.util.FormBuilder
import org.geysermc.cumulus.response.FormResponse
import org.geysermc.cumulus.response.result.FormResponseResult
import org.geysermc.cumulus.response.result.InvalidFormResponseResult
import org.geysermc.cumulus.response.result.ValidFormResponseResult
import org.geysermc.cumulus.util.FormImage
import java.util.*
import java.util.concurrent.Executor

/**
 * A DSL marker for the Bedrock form DSL to prevent scope conflicts in nested DSL blocks.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class BedrockFormDsl

/**
 * Text used inside a form. Either a raw string (which may contain legacy color codes or
 * translation keys for the Cumulus translator) or an Adventure [Component], which is rendered
 * with the form's locale and serialized to legacy section format, since Bedrock forms only
 * support plain strings.
 */
internal sealed interface FormText {
    fun resolve(locale: Locale?): String

    class Raw(val text: String) : FormText {
        override fun resolve(locale: Locale?) = text
    }

    class Rich(val component: Component) : FormText {
        override fun resolve(locale: Locale?): String {
            val rendered =
                if (locale != null) GlobalTranslator.render(component, locale) else component
            return LegacyComponentSerializer.legacySection().serialize(rendered)
        }
    }

    companion object {
        val EMPTY: FormText = Raw("")
    }
}

internal fun buildText(block: SurfComponentBuilder.() -> Unit): FormText =
    FormText.Rich(SurfComponentBuilder().apply(block).build())

/**
 * Creates a [FormImage] pointing to an image URL.
 */
fun formImageUrl(url: String): FormImage = FormImage.of(FormImage.Type.URL, url)

/**
 * Creates a [FormImage] pointing to a path inside the Bedrock resource packs,
 * e.g. `textures/items/diamond`.
 */
fun formImagePath(path: String): FormImage = FormImage.of(FormImage.Type.PATH, path)

/**
 * Base class of all Bedrock form DSL builders. Wraps the Cumulus [FormBuilder] and adds
 * Adventure component support, multiple result handlers and handler executors.
 *
 * @param F The Cumulus form type.
 * @param R The Cumulus response type.
 * @param B The Cumulus builder type.
 */
@BedrockFormDsl
sealed class BedrockFormBuilder<F : Form, R : FormResponse, B : FormBuilder<B, F, R>> {
    private var title: FormText = FormText.EMPTY
    private var translator: ((key: String, locale: String) -> String)? = null
    private var translatorLocale: String? = null
    private var executor: Executor? = null

    private val closedHandlers = mutableListOf<() -> Unit>()
    private val invalidHandlers = mutableListOf<(InvalidFormResponseResult<R>) -> Unit>()
    private val validHandlers = mutableListOf<(R) -> Unit>()
    private val resultHandlers = mutableListOf<(F, FormResponseResult<R>) -> Unit>()
    private val internalListeners = mutableListOf<(FormResponseResult<R>) -> Unit>()

    /**
     * The locale used to render Adventure components (e.g. translatable components) in this form.
     * Automatically set to the player's locale when the form is sent via the player extensions.
     */
    var locale: Locale? = null

    /**
     * Sets the title of the form.
     */
    fun title(title: String) {
        this.title = FormText.Raw(title)
    }

    /**
     * Sets the title of the form.
     */
    fun title(title: Component) {
        this.title = FormText.Rich(title)
    }

    /**
     * Sets the title of the form using the [SurfComponentBuilder].
     */
    fun title(block: SurfComponentBuilder.() -> Unit) {
        this.title = buildText(block)
    }

    /**
     * Sets the Cumulus translator, which is applied to every string of the form.
     *
     * @param locale The locale passed to the translator. If `null`, the locale of the Bedrock
     * client is used.
     * @param translator Receives the translation key and the locale and returns the translated string.
     */
    fun translator(locale: String? = null, translator: (key: String, locale: String) -> String) {
        this.translator = translator
        this.translatorLocale = locale
    }

    /**
     * Sets the executor all handlers of this form are executed on.
     * By default, handlers run on the thread Floodgate delivers the response on,
     * which is **not** the server thread.
     *
     * @see runOn
     */
    fun executor(executor: Executor) {
        this.executor = executor
    }

    /**
     * Runs all handlers of this form on the entity scheduler of the given [player].
     * This makes it safe to use the Bukkit API inside the handlers (Folia compatible).
     */
    fun runOn(player: Player, plugin: Plugin) {
        executor { task -> player.scheduler.run(plugin, { task.run() }, null) }
    }

    /**
     * Called when the player closed the form.
     */
    fun onClose(handler: () -> Unit) {
        closedHandlers += handler
    }

    /**
     * Called when the client sent an invalid response.
     */
    fun onInvalid(handler: (InvalidFormResponseResult<R>) -> Unit) {
        invalidHandlers += handler
    }

    /**
     * Called when the player closed the form or the client sent an invalid response.
     */
    fun onClosedOrInvalid(handler: (FormResponseResult<R>) -> Unit) {
        resultHandlers += { _, result -> if (!result.isValid) handler(result) }
    }

    /**
     * Called with the raw Cumulus response when the player submitted the form.
     */
    fun onValid(handler: (R) -> Unit) {
        validHandlers += handler
    }

    /**
     * Called for every result of this form, regardless of its type.
     */
    fun onResult(handler: (form: F, result: FormResponseResult<R>) -> Unit) {
        resultHandlers += handler
    }

    internal fun addInternalListener(listener: (FormResponseResult<R>) -> Unit) {
        internalListeners += listener
    }

    protected abstract fun newBuilder(): B
    protected abstract fun B.configure(locale: Locale?)

    /**
     * Dispatches a valid response to builder-specific handlers (e.g. button click handlers).
     */
    protected open fun handleValid(response: R) = Unit

    /**
     * Builds the Cumulus form.
     */
    fun build(): F {
        val locale = locale
        val builder = newBuilder()
        builder.title(title.resolve(locale))

        translator?.let { translator ->
            val locale = translatorLocale
            if (locale != null) {
                builder.translator(translator, locale)
            } else {
                builder.translator(translator)
            }
        }

        builder.configure(locale)
        builder.resultHandler { form, result -> dispatch(form, result) }
        return builder.build()
    }

    private fun dispatch(form: F, result: FormResponseResult<R>) {
        internalListeners.forEach { it(result) }

        val handle = {
            when (result) {
                is ValidFormResponseResult<R> -> {
                    val response = result.response()
                    handleValid(response)
                    validHandlers.forEach { it(response) }
                }

                is InvalidFormResponseResult<R> -> invalidHandlers.forEach { it(result) }
                else -> closedHandlers.forEach { it() }
            }
            resultHandlers.forEach { it(form, result) }
        }

        val executor = executor
        if (executor != null) executor.execute(handle) else handle()
    }
}
