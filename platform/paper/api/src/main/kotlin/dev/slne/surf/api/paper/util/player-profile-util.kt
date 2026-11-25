@file:JvmName("UtilPlayerProfile")

package dev.slne.surf.api.paper.util

import com.destroystokyo.paper.profile.PlayerProfile
import java.util.*

/**
 * Returns the unique id of this profile.
 *
 * @return the profile's unique id
 * @throws IllegalStateException if the profile does not provide a unique id
 */
fun PlayerProfile.idOrThrow(): UUID = id ?: error("PlayerProfile does not provide a uuid")
