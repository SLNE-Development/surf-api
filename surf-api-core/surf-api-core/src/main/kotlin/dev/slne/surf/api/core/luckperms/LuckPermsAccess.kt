package dev.slne.surf.api.core.luckperms

import dev.slne.surf.api.core.messages.adventure.uuidOrNull
import kotlinx.coroutines.future.await
import net.kyori.adventure.audience.Audience
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.model.user.User
import java.util.*

object LuckPermsAccess {
    val luckperms: LuckPerms get() = LuckPermsProvider.get()

    fun getUser(uuid: UUID): User? = luckperms.userManager.getUser(uuid)
    suspend fun loadUser(uuid: UUID): User = luckperms.userManager.loadUser(uuid).await()

    suspend fun getOrLoadUser(uuid: UUID): User = getUser(uuid) ?: loadUser(uuid)
}

val User.prefix: String
    get() = this.cachedData.metaData.prefix ?: ""

val User.suffix: String
    get() = this.cachedData.metaData.suffix ?: ""

val User.weight: Int
    get() = this.cachedData.metaData.weight

fun User.getMetaValue(key: String): String? {
    return cachedData.metaData.getMetaValue(key)
}

fun User.getMeta(key: String, default: String): String {
    return getMetaValue(key) ?: default
}

inline fun <T : Any> User.getMeta(
    key: String,
    transform: (String) -> T?,
): T? = getMetaValue(key)?.let(transform)

inline fun <T : Any> User.getMeta(
    key: String,
    default: T,
    transform: (String) -> T?
): T = getMeta(key, transform) ?: default

@Deprecated(
    message = "Use getMetaValue(key) for strings or getMeta(key, transform) for typed values.",
    replaceWith = ReplaceWith("getMeta(key) { it as? T }")
)
inline fun <reified T : Any> User.getMeta(key: String): T? {
    return getMeta(key) { it as? T }
}

@Deprecated(
    message = "Use getMeta(key, default, transform) for typed values.",
    replaceWith = ReplaceWith("getMeta(key, default) { it as? T }")
)
inline fun <reified T : Any> User.getMeta(key: String, default: T): T {
    return getMeta(key, default) { it as? T }
}

fun Audience.getLuckPermsUser(): User = this.getLuckPermsUserOrNull()
    ?: error("Audience does not have a valid UUID or LuckPerms user could not be found.")

fun Audience.getLuckPermsUserOrNull(): User? {
    return this.uuidOrNull()?.let { LuckPermsAccess.getUser(it) }
}