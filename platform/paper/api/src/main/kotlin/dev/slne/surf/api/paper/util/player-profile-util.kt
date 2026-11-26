@file:JvmName("UtilPlayerProfile")

package dev.slne.surf.api.paper.util

import com.destroystokyo.paper.profile.PlayerProfile
import dev.slne.surf.api.command.SurfCommandUtil
import dev.slne.surf.api.messages.adventure.buildText
import net.kyori.adventure.text.TextComponent
import java.util.*

/**
 * Returns the unique id of this profile.
 *
 * Use it outside commands, where a missing id is a programming error. Inside a command, use
 * [idOrFail], which reports a missing id to the command sender instead.
 *
 * @return the profile's unique id
 * @throws IllegalStateException if the profile does not provide a unique id
 */
fun PlayerProfile.idOrThrow(): UUID = id ?: error("PlayerProfile does not provide a uuid")

/**
 * Returns the unique id of this profile or fails the running command.
 *
 * Use it inside a command executor or argument, where the failure reaches the command sender as an
 * error message naming the profile. Outside commands, use [idOrThrow].
 *
 * @return the profile's unique id
 * @throws dev.slne.surf.api.command.exception.WrapperCommandExceptionComponent if the profile does
 * not provide a unique id
 */
fun PlayerProfile.idOrFail(): UUID {
    return id ?: SurfCommandUtil.failWithMessage(missingIdMessage(name ?: "Unbekannt"))
}

private fun missingIdMessage(profileName: String): TextComponent {
    return buildText {
        appendErrorPrefix()
        error("Das Profil ")
        variableValue(profileName)
        error(" hat keine UUID.")
    }
}
