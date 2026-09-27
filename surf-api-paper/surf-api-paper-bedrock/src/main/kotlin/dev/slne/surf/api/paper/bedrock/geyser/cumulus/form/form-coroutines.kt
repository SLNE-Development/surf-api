package dev.slne.surf.api.paper.bedrock.geyser.cumulus.form

import kotlinx.coroutines.suspendCancellableCoroutine
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.geysermc.cumulus.response.FormResponse
import org.geysermc.cumulus.response.ModalFormResponse
import org.geysermc.cumulus.response.SimpleFormResponse
import org.geysermc.cumulus.response.result.InvalidFormResponseResult
import org.geysermc.cumulus.response.result.ValidFormResponseResult
import org.geysermc.cumulus.util.FormImage

/**
 * The result of a form awaited with a suspending function.
 */
sealed interface FormResult<out T> {
    /**
     * The player submitted the form.
     */
    data class Valid<T>(val value: T) : FormResult<T>

    /**
     * The player closed the form.
     */
    data object Closed : FormResult<Nothing>

    /**
     * The client sent an invalid response.
     */
    data class Invalid(val componentIndex: Int, val errorMessage: String) : FormResult<Nothing>

    /**
     * The form could not be sent, e.g. because the player is not a Bedrock player.
     */
    data object NotSent : FormResult<Nothing>

    /**
     * Returns the value if the form was submitted, otherwise `null`.
     */
    fun getOrNull(): T? = (this as? Valid<T>)?.value
}

private suspend fun <R : FormResponse, T> Player.awaitForm(
    builder: BedrockFormBuilder<*, R, *>,
    mapper: (R) -> T,
): FormResult<T> = suspendCancellableCoroutine { continuation ->
    builder.addInternalListener { result ->
        if (!continuation.isActive) return@addInternalListener
        continuation.resumeWith(runCatching {
            when (result) {
                is ValidFormResponseResult<R> -> FormResult.Valid(mapper(result.response()))
                is InvalidFormResponseResult<R> -> FormResult.Invalid(
                    result.componentIndex(),
                    result.errorMessage()
                )

                else -> FormResult.Closed
            }
        })
    }

    if (!sendForm(builder.build())) {
        continuation.resumeWith(Result.success(FormResult.NotSent))
        return@suspendCancellableCoroutine
    }

    continuation.invokeOnCancellation { closeForm() }
}

/**
 * Sends a [SimpleForm][org.geysermc.cumulus.form.SimpleForm] to this player and suspends until
 * it is answered. Button handlers are still called.
 *
 * **Example Usage:**
 * ```kotlin
 * val index = player.awaitSimpleForm {
 *     title("Choose")
 *     button("A")
 *     button("B")
 * }.getOrNull()
 * ```
 *
 * @return The index of the clicked button.
 */
suspend fun Player.awaitSimpleForm(block: SimpleFormBuilder.() -> Unit): FormResult<Int> =
    awaitForm(
        SimpleFormBuilder().localizedFor(this).apply(block),
        SimpleFormResponse::clickedButtonId
    )

/**
 * Sends a [SimpleForm][org.geysermc.cumulus.form.SimpleForm] with one button per value to this
 * player and suspends until it is answered.
 *
 * @return The value of the clicked button.
 */
suspend fun <T> Player.awaitChoice(
    values: List<T>,
    text: (T) -> Component,
    image: (T) -> FormImage? = { null },
    block: SimpleFormBuilder.() -> Unit = {},
): FormResult<T> = awaitForm(SimpleFormBuilder().localizedFor(this).apply {
    block()
    buttons(values, text, image) {}
}) { values[it.clickedButtonId()] }

/**
 * Sends a [ModalForm][org.geysermc.cumulus.form.ModalForm] to this player and suspends until it
 * is answered. Button handlers are still called.
 *
 * **Example Usage:**
 * ```kotlin
 * val confirmed = player.awaitModalForm {
 *     title("Confirm")
 *     content("Are you sure?")
 *     button1("Yes")
 *     button2("No")
 * }.getOrNull() == true
 * ```
 *
 * @return `true` if the first button was clicked.
 */
suspend fun Player.awaitModalForm(block: ModalFormBuilder.() -> Unit): FormResult<Boolean> =
    awaitForm(ModalFormBuilder().localizedFor(this).apply(block), ModalFormResponse::clickedFirst)

/**
 * Sends a [CustomForm][org.geysermc.cumulus.form.CustomForm] to this player and suspends until it
 * is answered. The [block] has to return a mapper created with [CustomFormBuilder.result].
 *
 * **Example Usage:**
 * ```kotlin
 * val profile = player.awaitCustomForm {
 *     title("Profile")
 *     val name = input("Name")
 *     val age = intSlider("Age", 1..99)
 *     result { Profile(it[name], it[age]) }
 * }.getOrNull()
 * ```
 */
suspend fun <T> Player.awaitCustomForm(block: CustomFormBuilder.() -> CustomFormResultMapper<T>): FormResult<T> {
    val builder = CustomFormBuilder().localizedFor(this)
    val mapper = builder.block().mapper
    return awaitForm(builder) { mapper(CustomFormValues(it)) }
}
