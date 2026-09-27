package dev.slne.surf.api.paper.bedrock.geyser.cumulus.form

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.Component
import org.geysermc.cumulus.component.ButtonComponent
import org.geysermc.cumulus.form.SimpleForm
import org.geysermc.cumulus.response.SimpleFormResponse
import org.geysermc.cumulus.util.FormImage
import java.util.*

/**
 * Creates a Cumulus [SimpleForm] (a form with a text and a list of buttons) using the DSL.
 *
 * **Example Usage:**
 * ```kotlin
 * val form = simpleForm {
 *     title("Teleporter")
 *     content("Where do you want to go?")
 *     button("Spawn", formImagePath("textures/items/compass_item")) { player.teleport(spawn) }
 *     button("Farmwelt", formImageUrl("https://example.com/farm.png")) { player.teleport(farm) }
 *     onClose { player.sendMessage("Closed") }
 * }
 * ```
 */
fun simpleForm(block: SimpleFormBuilder.() -> Unit): SimpleForm =
    SimpleFormBuilder().apply(block).build()

/**
 * DSL builder for a Cumulus [SimpleForm].
 */
@BedrockFormDsl
class SimpleFormBuilder @PublishedApi internal constructor() :
    BedrockFormBuilder<SimpleForm, SimpleFormResponse, SimpleForm.Builder>() {
    private class Button(val text: FormText, val image: FormImage?, val onClick: (() -> Unit)?)

    private var content: FormText = FormText.EMPTY
    private val buttons = mutableListOf<Button>()
    private val clickHandlers = mutableListOf<(index: Int, button: ButtonComponent) -> Unit>()

    /**
     * Sets the content text shown above the buttons.
     */
    fun content(content: String) {
        this.content = FormText.Raw(content)
    }

    /**
     * Sets the content text shown above the buttons.
     */
    fun content(content: Component) {
        this.content = FormText.Rich(content)
    }

    /**
     * Sets the content text shown above the buttons using the [SurfComponentBuilder].
     */
    fun content(block: SurfComponentBuilder.() -> Unit) {
        this.content = buildText(block)
    }

    /**
     * Adds a button.
     *
     * @param image An optional image shown next to the button, see [formImageUrl] and [formImagePath].
     * @param onClick Called when the player clicks this button.
     */
    fun button(text: String, image: FormImage? = null, onClick: (() -> Unit)? = null) {
        buttons += Button(FormText.Raw(text), image, onClick)
    }

    /**
     * Adds a button.
     *
     * @param image An optional image shown next to the button, see [formImageUrl] and [formImagePath].
     * @param onClick Called when the player clicks this button.
     */
    fun button(text: Component, image: FormImage? = null, onClick: (() -> Unit)? = null) {
        buttons += Button(FormText.Rich(text), image, onClick)
    }

    /**
     * Adds one button for each of the given [values].
     *
     * @param text The button text of a value.
     * @param image The optional button image of a value.
     * @param onClick Called with the value of the clicked button.
     */
    fun <T> buttons(
        values: Iterable<T>,
        text: (T) -> Component,
        image: (T) -> FormImage? = { null },
        onClick: (T) -> Unit,
    ) {
        for (value in values) {
            buttons += Button(FormText.Rich(text(value)), image(value)) { onClick(value) }
        }
    }

    /**
     * Called when any button is clicked, with the index and the clicked button.
     */
    fun onButtonClick(handler: (index: Int, button: ButtonComponent) -> Unit) {
        clickHandlers += handler
    }

    override fun newBuilder(): SimpleForm.Builder = SimpleForm.builder()

    override fun SimpleForm.Builder.configure(locale: Locale?) {
        content(content.resolve(locale))
        for (button in buttons) {
            button(button.text.resolve(locale), button.image)
        }
    }

    override fun handleValid(response: SimpleFormResponse) {
        val index = response.clickedButtonId()
        buttons.getOrNull(index)?.onClick?.invoke()
        clickHandlers.forEach { it(index, response.clickedButton()) }
    }
}
