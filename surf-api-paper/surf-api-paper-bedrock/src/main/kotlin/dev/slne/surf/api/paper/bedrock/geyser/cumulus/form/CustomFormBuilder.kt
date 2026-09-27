package dev.slne.surf.api.paper.bedrock.geyser.cumulus.form

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.Component
import org.geysermc.cumulus.form.CustomForm
import org.geysermc.cumulus.response.CustomFormResponse
import org.geysermc.cumulus.util.FormImage
import java.util.*
import kotlin.math.roundToInt

/**
 * Creates a Cumulus [CustomForm] (a form with inputs, toggles, sliders, dropdowns, ...) using the DSL.
 * Every component function returns a typed [FormField], which is used to read the submitted value.
 *
 * Components can be added conditionally with a plain `if`, the field indices stay correct.
 *
 * **Example Usage:**
 * ```kotlin
 * val form = customForm {
 *     title("Settings")
 *     label("Configure your profile")
 *     val name = input("Name", placeholder = "Steve")
 *     val age = intSlider("Age", 1..99, default = 18)
 *     val mode = enumDropdown<GameMode>("Game mode", default = GameMode.SURVIVAL)
 *     val color = dropdown<NamedTextColor>("Color") {
 *         option(NamedTextColor.RED, text("Red", NamedTextColor.RED))
 *         option(NamedTextColor.BLUE, text("Blue", NamedTextColor.BLUE), default = true)
 *     }
 *     val notify = toggle("Notifications", default = true)
 *         .onChange { _, enabled -> player.sendMessage("Notifications: $enabled") }
 *     val features = enumToggles<Feature>(default = { it.isEnabled(player) })
 *         .onChange { feature, enabled -> feature.set(player, enabled) }
 *
 *     onChanges { changes -> changes.forEach { log("${it.label}: ${it.old} -> ${it.new}") } }
 *     onSubmit { values ->
 *         saveProfile(values[name], values[age], values[mode], values[color], values[notify])
 *     }
 * }
 * ```
 */
fun customForm(block: CustomFormBuilder.() -> Unit): CustomForm =
    CustomFormBuilder().apply(block).build()

/**
 * A typed handle to a component of a [CustomForm], used to read its submitted value
 * from [CustomFormValues] and to listen for changes.
 *
 * @property index The index of the component inside the form (labels included).
 * @property default The value the component has when the form is opened.
 */
class FormField<T> internal constructor(
    val index: Int,
    internal val text: FormText,
    val default: T,
    private val reader: (CustomFormResponse, Int) -> T,
) {
    private val changeListeners = mutableListOf<(old: T, new: T) -> Unit>()

    /**
     * Called when the player submitted the form with a value different from [default].
     *
     * Bedrock clients send no values when the form is closed, so changes are only
     * detected on submit.
     */
    fun onChange(listener: (old: T, new: T) -> Unit): FormField<T> {
        changeListeners += listener
        return this
    }

    internal fun read(response: CustomFormResponse): T = reader(response, index)

    internal fun dispatchChange(response: CustomFormResponse, locale: Locale?): FieldChange<T>? {
        val value = read(response)
        if (value == default) return null
        changeListeners.forEach { it(default, value) }
        return FieldChange(this, text.resolve(locale), default, value)
    }
}

/**
 * A changed value of a [FormField].
 *
 * @property field The changed field.
 * @property label The resolved text of the field.
 * @property old The default value of the field.
 * @property new The submitted value of the field.
 */
data class FieldChange<T> internal constructor(
    val field: FormField<T>,
    val label: String,
    val old: T,
    val new: T,
)

/**
 * A group of toggles, one per value, created with [CustomFormBuilder.toggles].
 *
 * @property toggles The toggle field of each value.
 */
class ToggleGroup<T> internal constructor(val toggles: Map<T, FormField<Boolean>>) {
    /**
     * Returns the toggle field of the given [value].
     */
    operator fun get(value: T): FormField<Boolean> = toggles.getValue(value)

    /**
     * Called for every toggle of this group that the player changed.
     */
    fun onChange(listener: (value: T, enabled: Boolean) -> Unit): ToggleGroup<T> {
        for ((value, field) in toggles) {
            field.onChange { _, new -> listener(value, new) }
        }
        return this
    }
}

/**
 * DSL builder for the toggles of a [ToggleGroup].
 *
 * @param T The value type of a toggle.
 */
@BedrockFormDsl
class ToggleGroupBuilder<T> @PublishedApi internal constructor() {
    internal class Toggle<T>(val value: T, val text: FormText, val default: Boolean)

    internal val toggles = mutableListOf<Toggle<T>>()

    /**
     * Adds a toggle.
     *
     * @param value The value this toggle represents.
     * @param text The displayed text.
     * @param default Whether this toggle is enabled by default.
     */
    fun toggle(value: T, text: String, default: Boolean = false) {
        toggles += Toggle(value, FormText.Raw(text), default)
    }

    /**
     * Adds a toggle.
     *
     * @param value The value this toggle represents.
     * @param text The displayed text.
     * @param default Whether this toggle is enabled by default.
     */
    fun toggle(value: T, text: Component, default: Boolean = false) {
        toggles += Toggle(value, FormText.Rich(text), default)
    }
}

/**
 * The submitted values of a [CustomForm].
 *
 * @property response The raw Cumulus response.
 */
class CustomFormValues internal constructor(val response: CustomFormResponse) {
    /**
     * Returns the submitted value of the given [field].
     */
    operator fun <T> get(field: FormField<T>): T = field.read(response)

    /**
     * Returns the submitted state of every toggle of the given [group].
     */
    operator fun <T> get(group: ToggleGroup<T>): Map<T, Boolean> =
        group.toggles.mapValues { (_, field) -> field.read(response) }

    /**
     * Returns the values of all enabled toggles of the given [group].
     */
    fun <T> enabled(group: ToggleGroup<T>): Set<T> =
        group.toggles.filterValues { it.read(response) }.keys
}

/**
 * DSL builder for the options of a dropdown or the steps of a step slider.
 *
 * @param T The value type of an option.
 */
@BedrockFormDsl
class FormOptionsBuilder<T> @PublishedApi internal constructor() {
    internal class Option<T>(val value: T, val text: FormText)

    internal val options = mutableListOf<Option<T>>()
    internal var defaultIndex = 0

    /**
     * Adds an option.
     *
     * @param value The value returned when this option is selected.
     * @param text The displayed text.
     * @param default Whether this option is selected by default.
     */
    fun option(value: T, text: String, default: Boolean = false) {
        add(Option(value, FormText.Raw(text)), default)
    }

    /**
     * Adds an option.
     *
     * @param value The value returned when this option is selected.
     * @param text The displayed text.
     * @param default Whether this option is selected by default.
     */
    fun option(value: T, text: Component, default: Boolean = false) {
        add(Option(value, FormText.Rich(text)), default)
    }

    private fun add(option: Option<T>, default: Boolean) {
        if (default) defaultIndex = options.size
        options += option
    }
}

/**
 * Adds an option whose value is its displayed text.
 */
fun FormOptionsBuilder<String>.option(text: String, default: Boolean = false) =
    option(text, text, default)

/**
 * DSL builder for a Cumulus [CustomForm].
 */
@BedrockFormDsl
class CustomFormBuilder @PublishedApi internal constructor() :
    BedrockFormBuilder<CustomForm, CustomFormResponse, CustomForm.Builder>() {
    private var icon: FormImage? = null
    private val components = mutableListOf<CustomForm.Builder.(Locale?) -> Unit>()
    private val fields = mutableListOf<FormField<*>>()
    private val submitHandlers = mutableListOf<(CustomFormValues) -> Unit>()
    private val changesHandlers = mutableListOf<(List<FieldChange<*>>) -> Unit>()

    /**
     * Sets the icon of the form, see [formImageUrl] and [formImagePath].
     */
    fun icon(image: FormImage) {
        icon = image
    }

    /**
     * Sets the icon of the form to an image URL.
     */
    fun iconUrl(url: String) = icon(formImageUrl(url))

    /**
     * Sets the icon of the form to a resource pack path.
     */
    fun iconPath(path: String) = icon(formImagePath(path))

    private fun <T> field(
        text: FormText,
        default: T,
        reader: (CustomFormResponse, Int) -> T,
        component: CustomForm.Builder.(Locale?) -> Unit,
    ): FormField<T> {
        val field = FormField(components.size, text, default, reader)
        components += component
        fields += field
        return field
    }

    /**
     * Adds a label (plain text without a value).
     */
    fun label(text: String) {
        components += { label(text) }
    }

    /**
     * Adds a label (plain text without a value).
     */
    fun label(text: Component) {
        val formText = FormText.Rich(text)
        components += { label(formText.resolve(it)) }
    }

    /**
     * Adds a label (plain text without a value) using the [SurfComponentBuilder].
     */
    fun label(block: SurfComponentBuilder.() -> Unit) {
        val formText = buildText(block)
        components += { label(formText.resolve(it)) }
    }

    /**
     * Adds a text input field.
     *
     * @return A field with the entered text.
     */
    fun input(text: String, placeholder: String = "", default: String = ""): FormField<String> =
        input(FormText.Raw(text), placeholder, default)

    /**
     * Adds a text input field.
     *
     * @return A field with the entered text.
     */
    fun input(text: Component, placeholder: String = "", default: String = ""): FormField<String> =
        input(FormText.Rich(text), placeholder, default)

    private fun input(text: FormText, placeholder: String, default: String) =
        field(text, default, { response, index -> response.asInput(index).orEmpty() }) {
            input(text.resolve(it), placeholder, default)
        }

    /**
     * Adds a toggle.
     *
     * @return A field with the toggle state.
     */
    fun toggle(text: String, default: Boolean = false): FormField<Boolean> =
        toggle(FormText.Raw(text), default)

    /**
     * Adds a toggle.
     *
     * @return A field with the toggle state.
     */
    fun toggle(text: Component, default: Boolean = false): FormField<Boolean> =
        toggle(FormText.Rich(text), default)

    private fun toggle(text: FormText, default: Boolean) =
        field(text, default, { response, index -> response.asToggle(index) }) {
            toggle(text.resolve(it), default)
        }

    /**
     * Adds a slider.
     *
     * @return A field with the selected value.
     */
    fun slider(
        text: String,
        min: Float,
        max: Float,
        step: Float = 1f,
        default: Float = min
    ): FormField<Float> =
        slider(FormText.Raw(text), min, max, step, default)

    /**
     * Adds a slider.
     *
     * @return A field with the selected value.
     */
    fun slider(
        text: Component,
        min: Float,
        max: Float,
        step: Float = 1f,
        default: Float = min
    ): FormField<Float> =
        slider(FormText.Rich(text), min, max, step, default)

    private fun slider(text: FormText, min: Float, max: Float, step: Float, default: Float) =
        field(text, default, { response, index -> response.asSlider(index) }) {
            slider(text.resolve(it), min, max, step, default)
        }

    /**
     * Adds a slider with integer values.
     *
     * @return A field with the selected value.
     */
    fun intSlider(
        text: String,
        range: IntRange,
        step: Int = 1,
        default: Int = range.first
    ): FormField<Int> =
        intSlider(FormText.Raw(text), range, step, default)

    /**
     * Adds a slider with integer values.
     *
     * @return A field with the selected value.
     */
    fun intSlider(
        text: Component,
        range: IntRange,
        step: Int = 1,
        default: Int = range.first
    ): FormField<Int> =
        intSlider(FormText.Rich(text), range, step, default)

    private fun intSlider(text: FormText, range: IntRange, step: Int, default: Int) =
        field(text, default, { response, index -> response.asSlider(index).roundToInt() }) {
            slider(
                text.resolve(it),
                range.first.toFloat(),
                range.last.toFloat(),
                step.toFloat(),
                default.toFloat()
            )
        }

    /**
     * Adds a dropdown with the options defined in [block].
     *
     * @return A field with the value of the selected option.
     */
    fun <T> dropdown(text: String, block: FormOptionsBuilder<T>.() -> Unit): FormField<T> =
        dropdown(FormText.Raw(text), FormOptionsBuilder<T>().apply(block))

    /**
     * Adds a dropdown with the options defined in [block].
     *
     * @return A field with the value of the selected option.
     */
    fun <T> dropdown(text: Component, block: FormOptionsBuilder<T>.() -> Unit): FormField<T> =
        dropdown(FormText.Rich(text), FormOptionsBuilder<T>().apply(block))

    /**
     * Adds a dropdown with one option per value.
     *
     * @param default The value selected by default.
     * @param display The displayed text of a value.
     * @return A field with the selected value.
     */
    fun <T> dropdown(
        text: String,
        values: Iterable<T>,
        default: T? = null,
        display: (T) -> String = { it.toString() },
    ): FormField<T> = dropdown(FormText.Raw(text), optionsOf(values, default, display))

    /**
     * Adds a dropdown with one option per enum constant.
     *
     * @param default The constant selected by default.
     * @param display The displayed text of a constant.
     * @return A field with the selected constant.
     */
    inline fun <reified E : Enum<E>> enumDropdown(
        text: String,
        default: E? = null,
        noinline display: (E) -> String = { it.name },
    ): FormField<E> = dropdown(text, enumValues<E>().asList(), default, display)

    private fun <T> dropdown(text: FormText, options: FormOptionsBuilder<T>): FormField<T> {
        require(options.options.isNotEmpty()) { "dropdown needs at least one option" }
        val values = options.options.map { it.value }
        val default = values[options.defaultIndex]
        return field(text, default, { response, index -> values[response.asDropdown(index)] }) { locale ->
            dropdown(
                text.resolve(locale),
                options.options.map { it.text.resolve(locale) },
                options.defaultIndex
            )
        }
    }

    /**
     * Adds a step slider with the steps defined in [block].
     *
     * @return A field with the value of the selected step.
     */
    fun <T> stepSlider(text: String, block: FormOptionsBuilder<T>.() -> Unit): FormField<T> =
        stepSlider(FormText.Raw(text), FormOptionsBuilder<T>().apply(block))

    /**
     * Adds a step slider with the steps defined in [block].
     *
     * @return A field with the value of the selected step.
     */
    fun <T> stepSlider(text: Component, block: FormOptionsBuilder<T>.() -> Unit): FormField<T> =
        stepSlider(FormText.Rich(text), FormOptionsBuilder<T>().apply(block))

    /**
     * Adds a step slider with one step per value.
     *
     * @param default The value selected by default.
     * @param display The displayed text of a value.
     * @return A field with the selected value.
     */
    fun <T> stepSlider(
        text: String,
        values: Iterable<T>,
        default: T? = null,
        display: (T) -> String = { it.toString() },
    ): FormField<T> = stepSlider(FormText.Raw(text), optionsOf(values, default, display))

    /**
     * Adds a step slider with one step per enum constant.
     *
     * @param default The constant selected by default.
     * @param display The displayed text of a constant.
     * @return A field with the selected constant.
     */
    inline fun <reified E : Enum<E>> enumStepSlider(
        text: String,
        default: E? = null,
        noinline display: (E) -> String = { it.name },
    ): FormField<E> = stepSlider(text, enumValues<E>().asList(), default, display)

    private fun <T> stepSlider(text: FormText, options: FormOptionsBuilder<T>): FormField<T> {
        require(options.options.isNotEmpty()) { "stepSlider needs at least one option" }
        val values = options.options.map { it.value }
        val default = values[options.defaultIndex]
        return field(text, default, { response, index -> values[response.asStepSlider(index)] }) { locale ->
            stepSlider(
                text.resolve(locale),
                options.options.map { it.text.resolve(locale) },
                options.defaultIndex
            )
        }
    }

    /**
     * Adds multiple toggles at once, one per value.
     *
     * **Example Usage:**
     * ```kotlin
     * val features = toggles<Feature> {
     *     toggle(Feature.FLY, "Fly", default = true)
     *     toggle(Feature.GOD, "God mode")
     * }.onChange { feature, enabled -> feature.set(player, enabled) }
     * ```
     *
     * @return A group to read the state of every toggle.
     */
    fun <T> toggles(block: ToggleGroupBuilder<T>.() -> Unit): ToggleGroup<T> {
        val toggles = ToggleGroupBuilder<T>().apply(block).toggles
        return ToggleGroup(toggles.associate { it.value to toggle(it.text, it.default) })
    }

    /**
     * Adds multiple toggles at once, one per value.
     *
     * @param display The displayed text of a value.
     * @param default Whether the toggle of a value is enabled by default.
     * @return A group to read the state of every toggle.
     */
    fun <T> toggles(
        values: Iterable<T>,
        display: (T) -> String = { it.toString() },
        default: (T) -> Boolean = { false },
    ): ToggleGroup<T> = toggles {
        for (value in values) toggle(value, display(value), default(value))
    }

    /**
     * Adds one toggle per enum constant.
     *
     * @param display The displayed text of a constant.
     * @param default Whether the toggle of a constant is enabled by default.
     * @return A group to read the state of every toggle.
     */
    inline fun <reified E : Enum<E>> enumToggles(
        noinline display: (E) -> String = { it.name },
        noinline default: (E) -> Boolean = { false },
    ): ToggleGroup<E> = toggles(enumValues<E>().asList(), display, default)

    private fun <T> optionsOf(values: Iterable<T>, default: T?, display: (T) -> String) =
        FormOptionsBuilder<T>().apply {
            for (value in values) option(value, display(value), value == default)
        }

    /**
     * Called when the player submitted the form.
     */
    fun onSubmit(handler: (values: CustomFormValues) -> Unit) {
        submitHandlers += handler
    }

    /**
     * Called when the player submitted the form, with every field whose value differs
     * from its default. The list is empty if nothing was changed.
     *
     * Bedrock clients send no values when the form is closed, so changes are only
     * detected on submit.
     *
     * @see FormField.onChange
     */
    fun onChanges(handler: (changes: List<FieldChange<*>>) -> Unit) {
        changesHandlers += handler
    }

    /**
     * Creates a mapper that converts the submitted values into a result,
     * used by [awaitCustomForm][awaitCustomForm].
     */
    fun <T> result(mapper: (values: CustomFormValues) -> T): CustomFormResultMapper<T> =
        CustomFormResultMapper(mapper)

    override fun newBuilder(): CustomForm.Builder = CustomForm.builder()

    override fun CustomForm.Builder.configure(locale: Locale?) {
        icon?.let { icon(it) }
        for (component in components) {
            component(locale)
        }
    }

    override fun handleValid(response: CustomFormResponse) {
        val changes = fields.mapNotNull { it.dispatchChange(response, locale) }
        changesHandlers.forEach { it(changes) }

        val values = CustomFormValues(response)
        submitHandlers.forEach { it(values) }
    }
}

/**
 * Maps the submitted values of a [CustomForm] into a result.
 *
 * @see CustomFormBuilder.result
 */
class CustomFormResultMapper<T> internal constructor(internal val mapper: (CustomFormValues) -> T)
