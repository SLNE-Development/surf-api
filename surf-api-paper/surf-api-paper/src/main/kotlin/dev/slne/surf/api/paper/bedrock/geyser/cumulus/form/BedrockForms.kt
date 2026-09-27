package dev.slne.surf.api.paper.bedrock.geyser.cumulus.form

import dev.slne.surf.api.paper.bedrock.ifBedrockAvailable
import org.bukkit.entity.Player
import org.geysermc.cumulus.form.CustomForm
import org.geysermc.cumulus.form.Form
import org.geysermc.cumulus.form.ModalForm
import org.geysermc.cumulus.form.SimpleForm
import org.geysermc.floodgate.api.FloodgateApi
import org.geysermc.geyser.api.GeyserApi
import java.util.*

/**
 * Sends Cumulus forms to Bedrock players. Requires Geyser and Floodgate to be installed,
 * otherwise every function is a no-op. Uses Floodgate for Floodgate players (which also works
 * behind a proxy) and falls back to Geyser otherwise.
 */
object BedrockForms {
    private val floodgate get() = FloodgateApi.getInstance()
    private val geyser get() = GeyserApi.api()

    /**
     * Returns `true` if the player with the given [uuid] is a Bedrock player.
     * Returns `false` if Geyser or Floodgate is not installed.
     */
    fun isBedrockPlayer(uuid: UUID): Boolean = ifBedrockAvailable(false) {
        floodgate.isFloodgatePlayer(uuid) || geyser.isBedrockPlayer(uuid)
    }

    /**
     * Sends the [form] to the player with the given [uuid].
     *
     * @return `true` if the form was sent, `false` if the player is not a Bedrock player
     * or Geyser or Floodgate is not installed.
     */
    fun send(uuid: UUID, form: Form): Boolean = ifBedrockAvailable(false) {
        if (floodgate.isFloodgatePlayer(uuid)) floodgate.sendForm(uuid, form)
        else geyser.sendForm(uuid, form)
    }

    /**
     * Closes the currently open form of the player with the given [uuid].
     *
     * @return `true` if the form was closed.
     */
    fun close(uuid: UUID): Boolean = ifBedrockAvailable(false) {
        if (floodgate.isFloodgatePlayer(uuid)) return@ifBedrockAvailable floodgate.closeForm(uuid)
        val connection = geyser.connectionByUuid(uuid) ?: return@ifBedrockAvailable false
        connection.closeForm()
        true
    }

    /**
     * Returns `true` if the player with the given [uuid] currently has a form open.
     * Returns `false` if Geyser or Floodgate is not installed.
     */
    fun hasFormOpen(uuid: UUID): Boolean = ifBedrockAvailable(false) {
        geyser.connectionByUuid(uuid)?.hasFormOpen() ?: false
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

/**
 * Returns `true` if this player currently has a form open.
 *
 * @see BedrockForms.hasFormOpen
 */
fun Player.hasFormOpen(): Boolean = BedrockForms.hasFormOpen(uniqueId)
