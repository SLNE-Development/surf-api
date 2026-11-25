@file:JvmName("LuckPermsExtensions")

package dev.slne.surf.api.luckperms

import dev.slne.surf.api.messages.adventure.uuidOrNull
import net.kyori.adventure.audience.Audience
import net.luckperms.api.model.user.User
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import java.util.*

/**
 * The user's prefix, or an empty string if they have none.
 */
val User.prefix: String
    get() = cachedData.metaData.prefix ?: ""

/**
 * The user's suffix, or an empty string if they have none.
 */
val User.suffix: String
    get() = cachedData.metaData.suffix ?: ""

/**
 * The user's weight.
 */
val User.weight: Int
    get() = cachedData.metaData.weight

/**
 * Returns the user's meta value under [key].
 *
 * @param key the meta key
 * @return the value, or `null` if the user has no such meta
 */
fun User.getMetaValue(key: String): String? = cachedData.metaData.getMetaValue(key)

/**
 * Returns the user's meta value under [key], or [default] if they have none.
 *
 * @param key the meta key
 * @param default the value returned when the meta is missing
 * @return the value or [default]
 */
fun User.getMeta(key: String, default: String): String = getMetaValue(key) ?: default

/**
 * Returns the user's meta value under [key], converted by [transform].
 *
 * @param key the meta key
 * @param transform converts the raw value; returns `null` for a value it cannot convert
 * @return the converted value, or `null` if the meta is missing or [transform] returned `null`
 */
inline fun <T : Any> User.getMeta(
    key: String,
    transform: (String) -> T?,
): T? = getMetaValue(key)?.let(transform)

/**
 * Returns the user's meta value under [key], converted by [transform], or [default].
 *
 * @param key the meta key
 * @param default the value returned when the meta is missing or cannot be converted
 * @param transform converts the raw value; returns `null` for a value it cannot convert
 * @return the converted value or [default]
 */
inline fun <T : Any> User.getMeta(
    key: String,
    default: T,
    transform: (String) -> T?,
): T = getMeta(key, transform) ?: default

/**
 * Returns the LuckPerms user with this UUID if LuckPerms holds them in memory.
 *
 * @return the loaded user, or `null` if LuckPerms has not loaded them
 * @throws IllegalStateException if LuckPerms is not running
 * @see SurfLuckPerms.getUser
 */
fun UUID.getLuckPermsUserOrNull(): User? = surfLuckPerms().getUser(this)

/**
 * Returns the LuckPerms user with this UUID, who must be loaded, such as an online player.
 *
 * @return the loaded user
 * @throws IllegalStateException if LuckPerms is not running or has not loaded the user
 */
fun UUID.getLuckPermsUser(): User {
    return getLuckPermsUserOrNull()
        ?: error("LuckPerms has not loaded the user $this")
}

/**
 * Returns the LuckPerms user with this UUID from memory, or loads them from storage.
 *
 * @return the user
 * @throws IllegalStateException if LuckPerms is not running
 * @see SurfLuckPerms.getOrLoadUser
 */
suspend fun UUID.loadLuckPermsUser(): User = surfLuckPerms().getOrLoadUser(this)

/**
 * Returns the loaded LuckPerms user of this audience.
 *
 * @return the loaded user
 * @throws IllegalStateException if the audience has no UUID, LuckPerms is not running or has not
 * loaded the user
 */
fun Audience.getLuckPermsUser(): User {
    return getLuckPermsUserOrNull()
        ?: error("The audience has no UUID or LuckPerms has not loaded its user")
}

/**
 * Returns the loaded LuckPerms user of this audience.
 *
 * @return the loaded user, or `null` if the audience has no UUID or LuckPerms has not loaded them
 * @throws IllegalStateException if LuckPerms is not running
 */
fun Audience.getLuckPermsUserOrNull(): User? = uuidOrNull()?.getLuckPermsUserOrNull()

private fun surfLuckPerms(): SurfLuckPerms {
    return try {
        SurfLuckPerms.instance
    } catch (exception: NoSuchBeanDefinitionException) {
        throw IllegalStateException("LuckPerms is not available on this platform.", exception)
    }
}
