package dev.slne.surf.api.paper.bedrock.geyser.cumulus.form

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.geysermc.cumulus.form.CustomForm
import org.geysermc.cumulus.form.Form
import org.geysermc.cumulus.form.ModalForm
import org.geysermc.cumulus.form.SimpleForm
import org.geysermc.floodgate.api.FloodgateApi
import org.geysermc.geyser.api.GeyserApi
import java.util.*

/**
 * Sends Cumulus forms to Bedrock players. Uses Floodgate if it is installed (which also works
 * behind a proxy) and falls back to Geyser otherwise.
 */
object BedrockForms {
    private val floodgate
        get() = if (Bukkit.getPluginManager()
                .isPluginEnabled("floodgate")
        ) FloodgateApi.getInstance() else null
    private val geyser
        get() = if (Bukkit.getPluginManager()
                .isPluginEnabled("Geyser-Spigot")
        ) GeyserApi.api() else null

    /**
     * Returns `true` if the player with the given [uuid] is a Bedrock player.
     */
    fun isBedrockPlayer(uuid: UUID): Boolean =
        floodgate?.isFloodgatePlayer(uuid) ?: geyser?.isBedrockPlayer(uuid) ?: false

    /**
     * Sends the [form] to the player with the given [uuid].
     *
     * @return `true` if the form was sent, `false` if the player is not a Bedrock player
     * or neither Floodgate nor Geyser is installed.
     */
    fun send(uuid: UUID, form: Form): Boolean {
        floodgate?.let { if (it.isFloodgatePlayer(uuid)) return it.sendForm(uuid, form) }
        return geyser?.sendForm(uuid, form) ?: false
    }

    /**
     * Closes the currently open form of the player with the given [uuid].
     *
     * @return `true` if the form was closed.
     */
    fun close(uuid: UUID): Boolean {
        floodgate?.let { if (it.isFloodgatePlayer(uuid)) return it.closeForm(uuid) }
        val connection = geyser?.connectionByUuid(uuid) ?: return false
        connection.closeForm()
        return true
    }

    /**
     * Returns `true` if the player with the given [uuid] currently has a form open.
     * Only available if Geyser is installed on this server, returns `false` otherwise.
     */
    fun hasFormOpen(uuid: UUID): Boolean = geyser?.connectionByUuid(uuid)?.hasFormOpen() ?: false
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
