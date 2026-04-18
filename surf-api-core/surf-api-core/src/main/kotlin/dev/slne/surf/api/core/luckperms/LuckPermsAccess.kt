package dev.slne.surf.api.core.luckperms

import dev.slne.surf.api.core.messages.adventure.uuidOrNull
import kotlinx.coroutines.future.await
import net.kyori.adventure.audience.Audience
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.model.user.User
import net.luckperms.api.node.NodeType
import java.util.*

object LuckPermsAccess {
    val luckperms by lazy {
        LuckPermsProvider.get()
    }

    fun getUser(uuid: UUID) = luckperms.userManager.getUser(uuid)
    suspend fun loadUser(uuid: UUID): User = luckperms.userManager.loadUser(uuid).await()
}

val User.prefix: String
    get() = this.cachedData.metaData.prefix ?: ""
val User.suffix: String
    get() = this.cachedData.metaData.suffix ?: ""

val User.weight
    get() = LuckPermsAccess.luckperms.groupManager.getGroup(this.primaryGroup)?.weight ?: 0

inline fun <reified T : Any> User.getMeta(key: String): T? {
    val value = this.resolveInheritedNodes(NodeType.META, this.queryOptions)
        .find { it.metaKey == key }
        ?.metaValue

    return value as? T
}

inline fun <reified T : Any> User.getMeta(key: String, default: T): T {
    val value = this.resolveInheritedNodes(NodeType.META, this.queryOptions)
        .find { it.metaKey == key }
        ?.metaValue

    return value as? T ?: default
}


fun Audience.getLuckPermsUser(): User = this.getLuckPermsUserOrNull()
    ?: error("Audience does not have a valid UUID or LuckPerms user could not be found.")

fun Audience.getLuckPermsUserOrNull(): User? = this.uuidOrNull()?.let { LuckPermsAccess.getUser(it) }