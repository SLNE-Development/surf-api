package dev.slne.surf.api.paper.bedrock.form

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.Component
import org.geysermc.cumulus.form.ModalForm
import org.geysermc.cumulus.response.ModalFormResponse
import java.util.*

/**
 * Creates a Cumulus [ModalForm] (a form with a text and exactly two buttons) using the DSL.
 *
 * **Example Usage:**
 * ```kotlin
 * val form = modalForm {
 *     title("Delete home")
 *     content("Do you really want to delete your home?")
 *     button1("Yes") { deleteHome() }
 *     button2("No")
 * }
 * ```
 */
fun modalForm(block: ModalFormBuilder.() -> Unit): ModalForm =
    ModalFormBuilder().apply(block).build()

/**
 * DSL builder for a Cumulus [ModalForm].
 */
@BedrockFormDsl
class ModalFormBuilder @PublishedApi internal constructor() :
    BedrockFormBuilder<ModalForm, ModalFormResponse, ModalForm.Builder>() {
    private var content: FormText = FormText.EMPTY
    private var button1: FormText = FormText.EMPTY
    private var button2: FormText = FormText.EMPTY
    private var onButton1: (() -> Unit)? = null
    private var onButton2: (() -> Unit)? = null
    private val responseHandlers = mutableListOf<(clickedFirst: Boolean) -> Unit>()

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
     * Sets the first (upper) button.
     */
    fun button1(text: String, onClick: (() -> Unit)? = null) {
        button1 = FormText.Raw(text)
        onButton1 = onClick
    }

    /**
     * Sets the first (upper) button.
     */
    fun button1(text: Component, onClick: (() -> Unit)? = null) {
        button1 = FormText.Rich(text)
        onButton1 = onClick
    }

    /**
     * Sets the second (lower) button.
     */
    fun button2(text: String, onClick: (() -> Unit)? = null) {
        button2 = FormText.Raw(text)
        onButton2 = onClick
    }

    /**
     * Sets the second (lower) button.
     */
    fun button2(text: Component, onClick: (() -> Unit)? = null) {
        button2 = FormText.Rich(text)
        onButton2 = onClick
    }

    /**
     * Called when either button is clicked. `clickedFirst` is `true` if [button1] was clicked.
     */
    fun onResponse(handler: (clickedFirst: Boolean) -> Unit) {
        responseHandlers += handler
    }

    override fun newBuilder(): ModalForm.Builder = ModalForm.builder()

    override fun ModalForm.Builder.configure(locale: Locale?) {
        content(content.resolve(locale))
        button1(button1.resolve(locale))
        button2(button2.resolve(locale))
    }

    override fun handleValid(response: ModalFormResponse) {
        val clickedFirst = response.clickedFirst()
        if (clickedFirst) onButton1?.invoke() else onButton2?.invoke()
        responseHandlers.forEach { it(clickedFirst) }
    }
}
