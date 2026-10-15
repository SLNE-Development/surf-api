package dev.slne.surf.api.paper.bedrock.form

import dev.slne.surf.api.paper.bedrock.ifBedrockAvailable
import org.bukkit.entity.Player
import org.geysermc.cumulus.form.CustomForm
import org.geysermc.cumulus.form.Form
import org.geysermc.cumulus.form.ModalForm
import org.geysermc.cumulus.form.SimpleForm
import org.geysermc.floodgate.api.FloodgateApi
import java.util.*

/**
 * Sends Cumulus forms to Bedrock players via Floodgate (which also works behind a proxy).
 * Requires Floodgate to be installed, otherwise every function is a no-op.
 */
object BedrockForms {
    private val floodgate get() = FloodgateApi.getInstance()

    /**
     * Returns `true` if the player with the given [uuid] is a Bedrock player.
     * Returns `false` if Floodgate is not installed.
     */
    fun isBedrockPlayer(uuid: UUID): Boolean =
        ifBedrockAvailable(false) { floodgate.isFloodgatePlayer(uuid) }

    /**
     * Sends the [form] to the player with the given [uuid].
     *
     * @return `true` if the form was sent, `false` if the player is not a Bedrock player
     * or Floodgate is not installed.
     */
    fun send(uuid: UUID, form: Form): Boolean = ifBedrockAvailable(false) {
        floodgate.isFloodgatePlayer(uuid) && floodgate.sendForm(uuid, form)
    }

    /**
     * Closes the currently open form of the player with the given [uuid].
     *
     * @return `true` if the form was closed.
     */
    fun close(uuid: UUID): Boolean = ifBedrockAvailable(false) {
        floodgate.isFloodgatePlayer(uuid) && floodgate.closeForm(uuid)
    }
}

internal fun <B : BedrockFormBuilder<*, *, *>> B.localizedFor(player: Player): B =
    apply { locale = player.locale() }

/**
 * Sends the [form] to this player.
 *
 * @return `true` if the form was sent.
 * @see BedrockForms.send
 */
fun Player.sendForm(form: Form): Boolean = BedrockForms.send(uniqueId, form)

/**
 * Builds a [SimpleForm] with this player's locale and sends it to this player.
 *
 * @return `true` if the form was sent.
 */
fun Player.sendSimpleForm(block: SimpleFormBuilder.() -> Unit): Boolean =
    sendForm(SimpleFormBuilder().localizedFor(this).apply(block).build())

/**
 * Builds a [ModalForm] with this player's locale and sends it to this player.
 *
 * @return `true` if the form was sent.
 */
fun Player.sendModalForm(block: ModalFormBuilder.() -> Unit): Boolean =
    sendForm(ModalFormBuilder().localizedFor(this).apply(block).build())

/**
 * Builds a [CustomForm] with this player's locale and sends it to this player.
 *
 * @return `true` if the form was sent.
 */
fun Player.sendCustomForm(block: CustomFormBuilder.() -> Unit): Boolean =
    sendForm(CustomFormBuilder().localizedFor(this).apply(block).build())

/**
 * Closes the currently open form of this player.
 *
 * @return `true` if the form was closed.
 */
fun Player.closeForm(): Boolean = BedrockForms.close(uniqueId)
